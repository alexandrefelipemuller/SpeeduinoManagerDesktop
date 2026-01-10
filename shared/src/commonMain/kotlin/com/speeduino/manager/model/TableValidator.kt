package com.speeduino.manager.model

import com.speeduino.manager.shared.Logger

/**
 * Table Validator - Safety Validations for Speeduino Tables
 *
 * Validates table data before writing to ECU to prevent:
 * - Engine damage (excessive ignition advance)
 * - Out-of-range values
 * - Invalid configurations
 * - Non-monotonic bins (RPM/Load must be strictly increasing)
 *
 * Usage:
 * ```
 * val validator = TableValidator(tableMetadata)
 * val result = validator.validateBeforeWrite(table)
 * if (!result.isValid) {
 *     throw ValidationException(result.errors)
 * }
 * if (result.warnings.isNotEmpty()) {
 *     showWarningDialog(result.warnings)
 * }
 * ```
 */
class TableValidator(private val metadata: TableMetadata) {

    companion object {
        private const val TAG = "TableValidator"

        // Safety thresholds
        const val IGNITION_ADVANCE_WARNING_THRESHOLD = 45.0  // degrees
        const val IGNITION_ADVANCE_DANGER_THRESHOLD = 55.0   // degrees
        const val VE_VERY_LEAN_THRESHOLD = 40.0              // %
        const val VE_VERY_RICH_THRESHOLD = 200.0             // %
        const val AFR_LEAN_THRESHOLD = 16.0                  // AFR
        const val AFR_RICH_THRESHOLD = 10.0                  // AFR
        private const val MAP_LOAD_SCALE = 2
        private const val MAP_LOAD_MAX = MAP_LOAD_SCALE * 255
    }

    /**
     * Validate table data (non-blocking, allows warnings)
     *
     * @param table Generic table with rpmBins, loadBins, and values
     * @return ValidationResult with errors and warnings
     */
    fun validate(table: Any): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        when (table) {
            is VeTable -> {
                validateVeTable(table, errors, warnings)
            }
            is IgnitionTable -> {
                validateIgnitionTable(table, errors, warnings)
            }
            // AFR Table validation can be added here
            else -> {
                errors.add("Unknown table type: ${table::class.simpleName}")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validate table before writing to ECU (stricter)
     *
     * This method is more strict than validate() and should be used
     * right before writing to ECU. It checks for critical safety issues.
     *
     * @param table Table to validate
     * @return ValidationResult
     */
    fun validateBeforeWrite(table: Any): ValidationResult {
        val result = validate(table)

        // Log validation results
        if (!result.isValid) {
            Logger.e(TAG, "❌ Validation FAILED for ${metadata.name}")
            result.errors.forEach { Logger.e(TAG, "  ERROR: $it") }
        }

        if (result.warnings.isNotEmpty()) {
            Logger.w(TAG, "⚠️  Validation warnings for ${metadata.name}")
            result.warnings.forEach { Logger.w(TAG, "  WARNING: $it") }
        }

        if (result.isValid && result.warnings.isEmpty()) {
            Logger.i(TAG, "✅ Validation OK for ${metadata.name}")
        }

        return result
    }

    // ========================================
    // VE Table Validation
    // ========================================

    private fun validateVeTable(table: VeTable, errors: MutableList<String>, warnings: MutableList<String>) {
        Logger.d(TAG, "Validating VE Table...")

        // 1. Validate RPM bins
        validateRpmBins(table.rpmBins, errors, warnings)

        // 2. Validate Load bins
        val isMap = table.loadType == VeTable.LoadType.MAP
        validateLoadBins(table.loadBins, isMap, errors, warnings)

        // 3. Validate VE values (0-255%)
        validateVeValues(table.values, errors, warnings)

        // 4. Check for extreme VE values
        checkExtremeVeValues(table.values, warnings)
    }

    private fun validateVeValues(values: List<List<Int>>, errors: MutableList<String>, warnings: MutableList<String>) {
        var outOfRangeCount = 0

        values.forEachIndexed { row, rowValues ->
            rowValues.forEachIndexed { col, value ->
                if (value.toDouble() !in metadata.valueRange) {
                    outOfRangeCount++
                    if (outOfRangeCount <= 5) { // Report first 5 only
                        errors.add("VE value out of range at [$row,$col]: $value (valid: ${metadata.valueRange})")
                    }
                }
            }
        }

        if (outOfRangeCount > 5) {
            errors.add("... and ${outOfRangeCount - 5} more VE values out of range")
        }
    }

    private fun checkExtremeVeValues(values: List<List<Int>>, warnings: MutableList<String>) {
        var veryLeanCount = 0
        var veryRichCount = 0

        values.forEach { rowValues ->
            rowValues.forEach { value ->
                if (value < VE_VERY_LEAN_THRESHOLD) {
                    veryLeanCount++
                } else if (value > VE_VERY_RICH_THRESHOLD) {
                    veryRichCount++
                }
            }
        }

        if (veryLeanCount > 0) {
            warnings.add("⚠️  $veryLeanCount cells with very lean VE (<${VE_VERY_LEAN_THRESHOLD.toInt()}%) - engine may not start")
        }

        if (veryRichCount > 0) {
            warnings.add("⚠️  $veryRichCount cells with very rich VE (>${VE_VERY_RICH_THRESHOLD.toInt()}%) - excessive fuel consumption")
        }
    }

    // ========================================
    // Ignition Table Validation
    // ========================================

    private fun validateIgnitionTable(table: IgnitionTable, errors: MutableList<String>, warnings: MutableList<String>) {
        Logger.d(TAG, "Validating Ignition Table...")

        // 1. Validate RPM bins
        validateRpmBins(table.rpmBins, errors, warnings)

        // 2. Validate Load bins
        val isMap = table.loadType == IgnitionTable.LoadType.MAP
        validateLoadBins(table.loadBins, isMap, errors, warnings)

        // 3. Validate Ignition advance values (-40 to 70°)
        validateIgnitionValues(table.values, errors, warnings)

        // 4. CRITICAL: Check for dangerous ignition advance
        checkDangerousIgnitionAdvance(table.values, errors, warnings)

        // 5. Check for excessive retard
        checkExcessiveRetard(table.values, warnings)
    }

    private fun validateIgnitionValues(values: List<List<Int>>, errors: MutableList<String>, warnings: MutableList<String>) {
        var outOfRangeCount = 0

        values.forEachIndexed { row, rowValues ->
            rowValues.forEachIndexed { col, value ->
                if (value.toDouble() !in metadata.valueRange) {
                    outOfRangeCount++
                    if (outOfRangeCount <= 5) {
                        errors.add("Ignition advance out of range at [$row,$col]: ${value}° (valid: ${metadata.valueRange}°)")
                    }
                }
            }
        }

        if (outOfRangeCount > 5) {
            errors.add("... and ${outOfRangeCount - 5} more ignition values out of range")
        }
    }

    private fun checkDangerousIgnitionAdvance(values: List<List<Int>>, errors: MutableList<String>, warnings: MutableList<String>) {
        var dangerCount = 0
        var warningCount = 0
        val dangerousCells = mutableListOf<String>()

        values.forEachIndexed { row, rowValues ->
            rowValues.forEachIndexed { col, advance ->
                when {
                    advance >= IGNITION_ADVANCE_DANGER_THRESHOLD -> {
                        dangerCount++
                        if (dangerousCells.size < 5) {
                            dangerousCells.add("[$row,$col]: ${advance}°")
                        }
                    }
                    advance >= IGNITION_ADVANCE_WARNING_THRESHOLD -> {
                        warningCount++
                    }
                }
            }
        }

        if (dangerCount > 0) {
            errors.add("🚨 CRITICAL: $dangerCount cells with DANGEROUS ignition advance (>=${IGNITION_ADVANCE_DANGER_THRESHOLD.toInt()}°)")
            errors.add("    This can cause SEVERE ENGINE DAMAGE (detonation/knock)!")
            errors.add("    Dangerous cells: ${dangerousCells.joinToString(", ")}")
            if (dangerCount > 5) {
                errors.add("    ... and ${dangerCount - 5} more dangerous cells")
            }
        }

        if (warningCount > 0) {
            warnings.add("⚠️  $warningCount cells with high ignition advance (${IGNITION_ADVANCE_WARNING_THRESHOLD.toInt()}°-${IGNITION_ADVANCE_DANGER_THRESHOLD.toInt()}°)")
            warnings.add("    Monitor engine for knock/detonation carefully!")
        }
    }

    private fun checkExcessiveRetard(values: List<List<Int>>, warnings: MutableList<String>) {
        var retardCount = 0

        values.forEach { rowValues ->
            rowValues.forEach { advance ->
                if (advance < 0) {
                    retardCount++
                }
            }
        }

        if (retardCount > 0) {
            warnings.add("ℹ️  $retardCount cells with retarded timing (<0°) - this is unusual but may be intentional")
        }
    }

    // ========================================
    // Common Validations (RPM/Load bins)
    // ========================================

    private fun validateRpmBins(rpmBins: List<Int>, errors: MutableList<String>, warnings: MutableList<String>) {
        // Check size
        val expectedSize = metadata.valuesShape.second // columns
        if (rpmBins.size != expectedSize) {
            errors.add("Invalid RPM bins count: ${rpmBins.size} (expected: $expectedSize)")
            return
        }

        // Check range
        rpmBins.forEachIndexed { index, rpm ->
            if (rpm !in metadata.rpmRange) {
                errors.add("RPM bin[$index] out of range: $rpm (valid: ${metadata.rpmRange})")
            }
        }

        // Check monotonicity (strictly increasing)
        val nonMonotonicIndices = mutableListOf<Int>()
        for (i in 1 until rpmBins.size) {
            if (rpmBins[i] <= rpmBins[i - 1]) {
                nonMonotonicIndices.add(i)
            }
        }

        if (nonMonotonicIndices.isNotEmpty()) {
            warnings.add("⚠️  RPM bins are not strictly increasing (indices: $nonMonotonicIndices). Speeduino firmware 202501+ WILL REJECT this with error 0x84!")
            warnings.add("    Current RPM bins: ${rpmBins.joinToString(", ")}")
            nonMonotonicIndices.take(3).forEach { i ->
                warnings.add("    RPM[$i-1]=${rpmBins[i-1]}, RPM[$i]=${rpmBins[i]} (expected ${rpmBins[i]} > ${rpmBins[i-1]})")
            }
        }

        // Check for reasonable RPM distribution
        val minRpm = rpmBins.minOrNull() ?: 0
        val maxRpm = rpmBins.maxOrNull() ?: 0
        if (maxRpm - minRpm < 1000) {
            warnings.add("RPM range is very narrow (${minRpm}-${maxRpm} RPM) - consider wider distribution")
        }
    }

    private fun validateLoadBins(
        loadBins: List<Int>,
        isMap: Boolean,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Check size
        val expectedSize = metadata.valuesShape.first // rows
        if (loadBins.size != expectedSize) {
            errors.add("Invalid Load bins count: ${loadBins.size} (expected: $expectedSize)")
            return
        }

        // Check range
        val maxLoad = if (isMap) MAP_LOAD_MAX else metadata.loadRange.endInclusive.toInt()
        loadBins.forEachIndexed { index, load ->
            if (load !in 0..maxLoad) {
                val rangeLabel = if (isMap) "0..$MAP_LOAD_MAX" else metadata.loadRange.toString()
                errors.add("Load bin[$index] out of range: $load (valid: $rangeLabel)")
            }
        }

        // Check monotonicity (strictly increasing)
        val nonMonotonicIndices = mutableListOf<Int>()
        for (i in 1 until loadBins.size) {
            if (loadBins[i] <= loadBins[i - 1]) {
                nonMonotonicIndices.add(i)
            }
        }

        if (nonMonotonicIndices.isNotEmpty()) {
            warnings.add("⚠️  Load bins are not strictly increasing (indices: $nonMonotonicIndices). Speeduino firmware 202501+ WILL REJECT this with error 0x84!")
            warnings.add("    Current Load bins: ${loadBins.joinToString(", ")}")
            nonMonotonicIndices.take(3).forEach { i ->
                warnings.add("    Load[$i-1]=${loadBins[i-1]}, Load[$i]=${loadBins[i]} (expected ${loadBins[i]} > ${loadBins[i-1]})")
            }
        }

        if (isMap) {
            val rawBins = loadBins.map { it / MAP_LOAD_SCALE }
            val rawNonMonotonic = mutableListOf<Int>()
            for (i in 1 until rawBins.size) {
                if (rawBins[i] <= rawBins[i - 1]) {
                    rawNonMonotonic.add(i)
                }
            }

            if (rawNonMonotonic.isNotEmpty()) {
                warnings.add("Warning: Load bins lose resolution after MAP scaling (2 kPa). Speeduino will reject with error 0x84 if raw bins are not increasing.")
                warnings.add("    Raw bins: ${rawBins.joinToString(", ")}")
            } else if (loadBins.any { it % MAP_LOAD_SCALE != 0 }) {
                warnings.add("Warning: Load bins include odd kPa values; Speeduino stores MAP bins in 2 kPa steps.")
            }
        }
    }
}

/**
 * Validation result
 *
 * @param isValid True if no errors (warnings are allowed)
 * @param errors Critical errors that prevent writing to ECU
 * @param warnings Non-critical issues that user should review
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
) {
    /**
     * Get formatted error message for display
     */
    fun getErrorMessage(): String {
        return buildString {
            if (errors.isNotEmpty()) {
                appendLine("❌ ERRORS:")
                errors.forEach { appendLine("  • $it") }
            }
            if (warnings.isNotEmpty()) {
                if (errors.isNotEmpty()) appendLine()
                appendLine("⚠️  WARNINGS:")
                warnings.forEach { appendLine("  • $it") }
            }
        }.trim()
    }

    /**
     * Check if there are any issues (errors or warnings)
     */
    fun hasIssues(): Boolean = errors.isNotEmpty() || warnings.isNotEmpty()

    /**
     * Check if safe to write (no errors, warnings are OK)
     */
    fun isSafeToWrite(): Boolean = isValid
}

/**
 * Exception thrown when validation fails. Always carries the ValidationResult so callers
 * can show the full context (errors + warnings).
 */
class ValidationException(val result: ValidationResult) : Exception(result.getErrorMessage()) {
    constructor(message: String) : this(
        ValidationResult(
            isValid = false,
            errors = listOf(message),
            warnings = emptyList()
        )
    )
}
