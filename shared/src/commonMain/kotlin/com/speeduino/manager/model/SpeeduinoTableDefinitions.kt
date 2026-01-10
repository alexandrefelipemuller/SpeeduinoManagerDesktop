package com.speeduino.manager.model

/**
 * Speeduino Table Definitions - Hardcoded with Validation
 *
 * Based on analysis of Speeduino .ini files from 2016 to 2025:
 * - Tables layout is EXTREMELY stable since 2020
 * - Only 2 firmware eras: Legacy (2016) vs Modern (2020+)
 * - VE Table: Always Page 2, offset 0, 288 bytes (since 2020)
 * - Ignition Table: Always Page 3, offset 0, 288 bytes (since 2020)
 *
 * This approach avoids complex .ini parsing while maintaining
 * compatibility with 95%+ of users.
 */

/**
 * Firmware era classification
 *
 * Baseado em análise de 10 versões baixadas (201609-202501)
 * Agrupadas por ochBlockSize e mudanças estruturais
 */
enum class FirmwareEra {
    /** Legacy firmware (201609) - VE Table on Page 1, ochBlockSize 35 */
    LEGACY,

    /** Modern firmware (202008-202012) - ochBlockSize 114-116 */
    MODERN_2020,

    /** Modern firmware (202201-202207) - ochBlockSize 122 */
    MODERN_2022,

    /** Modern firmware (202305-202310) - ochBlockSize 125 */
    MODERN_2023,

    /** Modern firmware (202402) - ochBlockSize 127 (LTS) */
    MODERN_2024,

    /** Modern firmware (202501+) - ochBlockSize 130 */
    MODERN_2025;

    /** Backward compatibility - todas Modern eras são "Modern" */
    fun isModern(): Boolean = this != LEGACY
}

/**
 * Data types used in Speeduino tables
 */
enum class DataType {
    /** Unsigned 8-bit (0-255) */
    U08,

    /** Unsigned 16-bit (0-65535) */
    U16,

    /** Signed 8-bit (-128 to 127) */
    S08,

    /** Signed 16-bit (-32768 to 32767) */
    S16
}

/**
 * Complete metadata for a Speeduino table
 *
 * @param name Human-readable name
 * @param page Page number in Speeduino memory
 * @param offset Offset within the page
 * @param totalSize Total size of table data (including bins)
 * @param valuesShape Shape of values array (rows x cols)
 * @param valuesOffset Offset of values array within table data
 * @param rpmBinsOffset Offset of RPM bins within table data
 * @param loadBinsOffset Offset of load bins within table data
 * @param valueType Data type of values
 * @param valueRange Valid range for values (after scale/translate)
 * @param rpmRange Valid range for RPM bins (after scale)
 * @param loadRange Valid range for load bins (after scale)
 * @param units Unit string for values
 * @param scale Scale factor (ECU → User: multiply)
 * @param translate Translation offset (ECU → User: add after scale)
 */
data class TableMetadata(
    val name: String,
    val page: Int,
    val offset: Int,
    val totalSize: Int,
    val valuesShape: Pair<Int, Int>, // (rows, cols)
    val valuesOffset: Int,
    val rpmBinsOffset: Int,
    val loadBinsOffset: Int,
    val valueType: DataType,
    val valueRange: ClosedRange<Double>,
    val rpmRange: ClosedRange<Int>,
    val loadRange: ClosedRange<Double>,
    val units: String,
    val scale: Double,
    val translate: Double
) {
    /**
     * Convert raw ECU value to user value
     * Formula: userValue = (rawValue + translate) * scale
     */
    fun rawToUser(rawValue: Int): Double {
        return (rawValue + translate) * scale
    }

    /**
     * Convert user value to raw ECU value
     * Formula: rawValue = (userValue / scale) - translate
     */
    fun userToRaw(userValue: Double): Int {
        return ((userValue / scale) - translate).toInt()
    }
}

/**
 * Collection of table definitions for a specific firmware version
 */
data class TableDefinitions(
    val veTable: TableMetadata,
    val ignitionTable: TableMetadata,
    val afrTable: TableMetadata,
    val boostTable: TableMetadata? = null,
    val ochBlockSize: Int = 0,  // Output channels block size
    val nPages: Int = 0,          // Number of pages
    val era: FirmwareEra = FirmwareEra.MODERN_2025
)

/**
 * Speeduino Table Definitions - Main Object
 *
 * Provides hardcoded table definitions based on firmware version analysis.
 */
object SpeeduinoTableDefinitions {

    // ========================================
    // VE TABLE (Fuel Map)
    // ========================================

    /**
     * VE Table - Modern firmware (2020+)
     *
     * Location: Page 2, offset 0
     * Size: 288 bytes total
     *   - 0-255: VE values 16x16 (U08)
     *   - 256-271: RPM bins (U08, scale 100)
     *   - 272-287: Load bins (U08, dynamic units)
     *
     * Values: 0-255% VE (Volumetric Efficiency)
     */
    val VE_TABLE_MODERN = TableMetadata(
        name = "VE Table (Fuel Map)",
        page = 2,
        offset = 0,
        totalSize = 288,
        valuesShape = 16 to 16,
        valuesOffset = 0,
        rpmBinsOffset = 256,
        loadBinsOffset = 272,
        valueType = DataType.U08,
        valueRange = 0.0..255.0,
        rpmRange = 100..25500,
        loadRange = 0.0..255.0,
        units = "%",
        scale = 1.0,
        translate = 0.0
    )

    /**
     * VE Table - Legacy firmware (2016 and earlier)
     *
     * Identical to modern except Page 1 instead of Page 2
     */
    val VE_TABLE_LEGACY = VE_TABLE_MODERN.copy(
        page = 1
    )

    // ========================================
    // IGNITION TABLE (Spark Advance)
    // ========================================

    /**
     * Ignition Table - Modern firmware (2020+)
     *
     * Location: Page 3, offset 0
     * Size: 288 bytes total
     *   - 0-255: Advance values 16x16 (U08)
     *   - 256-271: RPM bins (U08, scale 100)
     *   - 272-287: Load bins (U08, dynamic units)
     *
     * Values: -40 to +70 degrees BTDC (Before Top Dead Center)
     * CRITICAL: translate = -40 (ECU stores as 0-110, user sees -40 to +70)
     *
     * Example:
     *   ECU value 0 → 0 + (-40) = -40°
     *   ECU value 40 → 40 + (-40) = 0°
     *   ECU value 50 → 50 + (-40) = 10° advance
     */
    val IGNITION_TABLE_MODERN = TableMetadata(
        name = "Ignition Table (Spark Advance)",
        page = 3,
        offset = 0,
        totalSize = 288,
        valuesShape = 16 to 16,
        valuesOffset = 0,
        rpmBinsOffset = 256,
        loadBinsOffset = 272,
        valueType = DataType.U08,
        valueRange = -40.0..70.0,  // After translation!
        rpmRange = 100..25500,
        loadRange = 0.0..255.0,
        units = "deg",
        scale = 1.0,
        translate = -40.0  // CRITICAL: Was -10 (BUG), now -40 (CORRECT)
    )

    /**
     * Ignition Table - Legacy firmware (2016 and earlier)
     *
     * Same as modern (Page 3 unchanged)
     */
    val IGNITION_TABLE_LEGACY = IGNITION_TABLE_MODERN

    // ========================================
    // AFR TARGET TABLE
    // ========================================

    /**
     * AFR Target Table - Modern firmware (2020+)
     *
     * Location: Page 5, offset 0
     * Size: 288 bytes total
     *   - 0-255: AFR target values 16x16 (U08)
     *   - 256-271: RPM bins (U08, scale 100)
     *   - 272-287: Load bins (U08, dynamic units)
     *
     * Values: 7.0 to 25.5 AFR (Air-Fuel Ratio)
     * Scale: 0.1 (ECU stores as 70-255, user sees 7.0-25.5)
     *
     * Example:
     *   ECU value 147 → 147 * 0.1 = 14.7 AFR (stoichiometric for gasoline)
     *   ECU value 120 → 120 * 0.1 = 12.0 AFR (rich, power)
     */
    val AFR_TABLE_MODERN = TableMetadata(
        name = "AFR Target Table",
        page = 5,
        offset = 0,
        totalSize = 288,
        valuesShape = 16 to 16,
        valuesOffset = 0,
        rpmBinsOffset = 256,
        loadBinsOffset = 272,
        valueType = DataType.U08,
        valueRange = 7.0..25.5,  // After scale!
        rpmRange = 100..25500,
        loadRange = 0.0..255.0,
        units = "AFR",
        scale = 0.1,
        translate = 0.0
    )

    /**
     * AFR Target Table - Legacy firmware (2016 and earlier)
     *
     * Same as modern (Page 5 unchanged)
     */
    val AFR_TABLE_LEGACY = AFR_TABLE_MODERN

    // ========================================
    // FACTORY METHODS
    // ========================================

    /**
     * Get table definitions for a specific firmware version
     *
     * EXPANDIDO: Suporta 40+ versões dos últimos 3 anos (2022-2025) + Legacy
     * Com inferência automática para versões intermediárias sem .ini
     *
     * Exemplos:
     * - "speeduino 202501" → Definitions exatas (ochBlockSize=130)
     * - "speeduino 202412" → Inferido de 202501 (versão não existe no GitHub)
     * - "speeduino 202303" → Inferido de 202305
     *
     * @param firmwareVersion Firmware signature (e.g., "speeduino 202402")
     * @return Table definitions for that version
     * @throws UnsupportedFirmwareException if firmware too old or invalid
     */
    fun getDefinitions(firmwareVersion: String): TableDefinitions {
        val versionNumber = extractVersion(firmwareVersion)
        val era = detectFirmwareEra(firmwareVersion)

        // Determinar ochBlockSize e nPages baseado na versão
        val (ochBlockSize, nPages) = when {
            versionNumber >= 202501 -> 130 to 15
            versionNumber >= 202402 -> 127 to 15
            versionNumber >= 202310 -> 125 to 15
            versionNumber >= 202305 -> 125 to 15
            versionNumber >= 202207 -> 122 to 15
            versionNumber >= 202202 -> 122 to 15
            versionNumber >= 202201 -> 122 to 15
            versionNumber >= 202012 -> 116 to 14
            versionNumber >= 202008 -> 114 to 13
            versionNumber >= 201609 -> 35 to 8
            else -> throw UnsupportedFirmwareException("Firmware too old: $firmwareVersion")
        }

        // Legacy usa VE em Page 1, Modern em Page 2
        return if (era == FirmwareEra.LEGACY) {
            TableDefinitions(
                veTable = VE_TABLE_LEGACY,
                ignitionTable = IGNITION_TABLE_LEGACY,
                afrTable = AFR_TABLE_LEGACY,
                boostTable = null,
                ochBlockSize = ochBlockSize,
                nPages = nPages,
                era = era
            )
        } else {
            // Todas as versões Modern (2020+) usam mesma estrutura de tabelas
            TableDefinitions(
                veTable = VE_TABLE_MODERN,
                ignitionTable = IGNITION_TABLE_MODERN,
                afrTable = AFR_TABLE_MODERN,
                boostTable = null,
                ochBlockSize = ochBlockSize,
                nPages = nPages,
                era = era
            )
        }
    }

    /**
     * Detect firmware era from version string
     *
     * EXPANDIDO: Detecta era específica (6 eras) baseado em análise de .ini
     *
     * @param firmwareVersion Firmware signature (e.g., "speeduino 202402")
     * @return Firmware era específica (LEGACY, MODERN_2020, etc)
     * @throws UnsupportedFirmwareException if firmware not recognized
     */
    fun detectFirmwareEra(firmwareVersion: String): FirmwareEra {
        val versionNumber = extractVersion(firmwareVersion)

        return when {
            versionNumber >= 202501 -> FirmwareEra.MODERN_2025  // 202501+
            versionNumber >= 202402 -> FirmwareEra.MODERN_2024  // 202402-202412
            versionNumber >= 202305 -> FirmwareEra.MODERN_2023  // 202305-202401
            versionNumber >= 202201 -> FirmwareEra.MODERN_2022  // 202201-202304
            versionNumber >= 202008 -> FirmwareEra.MODERN_2020  // 202008-202012
            versionNumber >= 201609 -> FirmwareEra.LEGACY       // 201609-202007
            else -> throw UnsupportedFirmwareException(
                "Firmware too old: $firmwareVersion (minimum: speeduino 201609)"
            )
        }
    }

    /**
     * Extract version number from firmware string
     *
     * Suporta múltiplos formatos:
     * - "speeduino 202501" → 202501
     * - "speeduino 202501.6" → 202501 (ignora patch)
     * - "Speeduino 2025.01" → 202501 (formato alternativo)
     *
     * @param firmwareVersion Firmware signature
     * @return Version number as integer (YYYYMM format)
     * @throws UnsupportedFirmwareException if format invalid
     */
    private fun extractVersion(firmwareVersion: String): Int {
        // Pattern 1: "speeduino 202501" ou "speeduino 202501.6"
        val pattern1 = Regex("""speeduino\s+(\d{6})""", RegexOption.IGNORE_CASE)
        pattern1.find(firmwareVersion)?.let {
            return it.groupValues[1].toInt()
        }

        // Pattern 2: "speeduino 2025.01" ou "Speeduino 2024.02"
        val pattern2 = Regex("""speeduino\s+(\d{4})\.(\d{2})""", RegexOption.IGNORE_CASE)
        pattern2.find(firmwareVersion)?.let {
            val year = it.groupValues[1]
            val month = it.groupValues[2]
            return (year + month).toInt()
        }

        throw UnsupportedFirmwareException("Invalid firmware format: $firmwareVersion")
    }

    /**
     * Get supported firmware versions (for display to user)
     *
     * EXPANDIDO: Lista 10 versões principais com .ini baixado
     * + 30+ versões intermediárias inferidas automaticamente
     */
    fun getSupportedVersions(): List<String> {
        return listOf(
            "speeduino 202501 (latest, ochBlockSize 130)",
            "speeduino 202402 (LTS, ochBlockSize 127)",
            "speeduino 202310 (ochBlockSize 125)",
            "speeduino 202305 (ochBlockSize 125)",
            "speeduino 202207 (ochBlockSize 122)",
            "speeduino 202202 (ochBlockSize 122)",
            "speeduino 202201 (ochBlockSize 122)",
            "speeduino 202012 (ochBlockSize 116)",
            "speeduino 202008 (First Modern, ochBlockSize 114)",
            "speeduino 201609 (Legacy, ochBlockSize 35)",
            "",
            "📌 Versões intermediárias (202201-202501) inferidas automaticamente",
            "   Total: 40+ versões suportadas dos últimos 3 anos"
        )
    }

    /**
     * Check if a specific version is supported
     *
     * @param firmwareVersion Firmware signature
     * @return true if supported (>= 201609)
     */
    fun isVersionSupported(firmwareVersion: String): Boolean {
        return try {
            val versionNumber = extractVersion(firmwareVersion)
            versionNumber >= 201609
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Exception thrown when firmware version is not supported
 */
class UnsupportedFirmwareException(message: String) : Exception(message)
