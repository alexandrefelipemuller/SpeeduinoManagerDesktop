package com.speeduino.manager.model

import kotlin.math.roundToInt

enum class ClosedLoopSensorType {
    DISABLED,
    NARROW_BAND,
    WIDE_BAND,
}

enum class ClosedLoopStrategy {
    SIMPLE,
    PID,
    NO_CORRECTION,
}

data class ClosedLoopCorrectionConfig(
    val sensorType: ClosedLoopSensorType = ClosedLoopSensorType.WIDE_BAND,
    val strategy: ClosedLoopStrategy = ClosedLoopStrategy.SIMPLE,
    val ignitionEventsPerStep: Int = 0,
    val authorityPercent: Int = 0,
    val minAfr: Double = 0.0,
    val maxAfr: Double = 0.0,
    val minLambda: Double = 0.0,
    val maxLambda: Double = 0.0,
    val activeAboveCoolantF: Int = -40,
    val activeAboveRpm: Int = 0,
    val activeBelowTpsPercent: Double = 0.0,
    val activeBelowMapKpa: Int = 0,
    val activeAboveMapKpa: Int = 0,
    val delayAfterStartSeconds: Int = 0,
    val pidProportional: Int = 0,
    val pidIntegral: Int = 0,
    val pidDerivative: Int = 0,
) {
    fun usesPid(): Boolean = strategy == ClosedLoopStrategy.PID
    fun isCorrectionEnabled(): Boolean = sensorType != ClosedLoopSensorType.DISABLED && strategy != ClosedLoopStrategy.NO_CORRECTION
}

object ClosedLoopCorrectionMapper {
    const val PAGE_NUMBER = 6
    const val PAGE_SIZE = 192
    const val STOICH_AFR = 14.7

    private const val ALGORITHM_OFFSET = 0
    private const val PID_KP_OFFSET = 1
    private const val PID_KI_OFFSET = 2
    private const val PID_KD_OFFSET = 3
    private const val COOLANT_OFFSET = 4
    private const val EVENTS_PER_STEP_OFFSET = 5
    private const val AUTHORITY_OFFSET = 7
    private const val MIN_AFR_OFFSET = 8
    private const val MAX_AFR_OFFSET = 9
    private const val DELAY_OFFSET = 10
    private const val RPM_OFFSET = 11
    private const val TPS_OFFSET = 12
    private const val MAP_MAX_OFFSET = 112
    private const val MAP_MIN_OFFSET = 113

    fun isSupported(era: FirmwareEra): Boolean = era.isModern()

    fun fromPage(page: ByteArray, era: FirmwareEra): ClosedLoopCorrectionConfig {
        require(isSupported(era)) { "Closed-loop corrections requer firmware Speeduino moderno" }

        val sensorType = when (readBits(page, ALGORITHM_OFFSET, 2, 2)) {
            1 -> ClosedLoopSensorType.NARROW_BAND
            2 -> ClosedLoopSensorType.WIDE_BAND
            else -> ClosedLoopSensorType.DISABLED
        }

        val strategy = when (readBits(page, ALGORITHM_OFFSET, 0, 2)) {
            2 -> ClosedLoopStrategy.PID
            3 -> ClosedLoopStrategy.NO_CORRECTION
            else -> ClosedLoopStrategy.SIMPLE
        }

        val minAfr = rawAfrToDisplay(readU8(page, MIN_AFR_OFFSET))
        val maxAfr = rawAfrToDisplay(readU8(page, MAX_AFR_OFFSET))

        return syncWindow(
            ClosedLoopCorrectionConfig(
                sensorType = sensorType,
                strategy = strategy,
                ignitionEventsPerStep = readU8(page, EVENTS_PER_STEP_OFFSET),
                authorityPercent = readU8(page, AUTHORITY_OFFSET),
                minAfr = minAfr,
                maxAfr = maxAfr,
                minLambda = afrToLambda(minAfr),
                maxLambda = afrToLambda(maxAfr),
                activeAboveCoolantF = rawCoolantToFahrenheit(readU8(page, COOLANT_OFFSET)),
                activeAboveRpm = readU8(page, RPM_OFFSET) * 100,
                activeBelowTpsPercent = readU8(page, TPS_OFFSET) * 0.5,
                activeBelowMapKpa = readU8(page, MAP_MAX_OFFSET) * 2,
                activeAboveMapKpa = readU8(page, MAP_MIN_OFFSET) * 2,
                delayAfterStartSeconds = readU8(page, DELAY_OFFSET),
                pidProportional = readU8(page, PID_KP_OFFSET),
                pidIntegral = readU8(page, PID_KI_OFFSET),
                pidDerivative = readU8(page, PID_KD_OFFSET),
            )
        )
    }

    fun applyToPage(base: ByteArray, config: ClosedLoopCorrectionConfig, era: FirmwareEra): ByteArray {
        require(isSupported(era)) { "Closed-loop corrections requer firmware Speeduino moderno" }
        val page = base.copyOf()
        val normalized = sanitize(config)

        val strategyBits = when (normalized.strategy) {
            ClosedLoopStrategy.SIMPLE -> 0
            ClosedLoopStrategy.PID -> 2
            ClosedLoopStrategy.NO_CORRECTION -> 3
        }
        val sensorBits = when (normalized.sensorType) {
            ClosedLoopSensorType.DISABLED -> 0
            ClosedLoopSensorType.NARROW_BAND -> 1
            ClosedLoopSensorType.WIDE_BAND -> 2
        }

        writeBits(page, ALGORITHM_OFFSET, 0, 2, strategyBits)
        writeBits(page, ALGORITHM_OFFSET, 2, 2, sensorBits)
        writeU8(page, PID_KP_OFFSET, normalized.pidProportional)
        writeU8(page, PID_KI_OFFSET, normalized.pidIntegral)
        writeU8(page, PID_KD_OFFSET, normalized.pidDerivative)
        writeU8(page, COOLANT_OFFSET, fahrenheitToRawCoolant(normalized.activeAboveCoolantF))
        writeU8(page, EVENTS_PER_STEP_OFFSET, normalized.ignitionEventsPerStep)
        writeU8(page, AUTHORITY_OFFSET, normalized.authorityPercent)
        writeU8(page, MIN_AFR_OFFSET, displayAfrToRaw(normalized.minAfr))
        writeU8(page, MAX_AFR_OFFSET, displayAfrToRaw(normalized.maxAfr))
        writeU8(page, DELAY_OFFSET, normalized.delayAfterStartSeconds)
        writeU8(page, RPM_OFFSET, (normalized.activeAboveRpm / 100.0).roundToInt())
        writeU8(page, TPS_OFFSET, (normalized.activeBelowTpsPercent / 0.5).roundToInt())
        writeU8(page, MAP_MAX_OFFSET, (normalized.activeBelowMapKpa / 2.0).roundToInt())
        writeU8(page, MAP_MIN_OFFSET, (normalized.activeAboveMapKpa / 2.0).roundToInt())
        return page
    }

    fun syncFromAfr(config: ClosedLoopCorrectionConfig): ClosedLoopCorrectionConfig = syncWindow(config)

    fun syncFromLambda(config: ClosedLoopCorrectionConfig): ClosedLoopCorrectionConfig {
        return sanitize(
            config.copy(
                minAfr = lambdaToAfr(config.minLambda),
                maxAfr = lambdaToAfr(config.maxLambda),
            )
        )
    }

    fun sanitize(config: ClosedLoopCorrectionConfig): ClosedLoopCorrectionConfig {
        val afrMin = config.minAfr.coerceIn(7.0, 25.0)
        val afrMax = config.maxAfr.coerceIn(7.0, 25.0)
        val correctedMinAfr = minOf(afrMin, afrMax)
        val correctedMaxAfr = maxOf(afrMin, afrMax)
        val pidEnabled = config.strategy == ClosedLoopStrategy.PID

        return config.copy(
            ignitionEventsPerStep = config.ignitionEventsPerStep.coerceIn(0, 255),
            authorityPercent = config.authorityPercent.coerceIn(0, 16),
            minAfr = correctedMinAfr,
            maxAfr = correctedMaxAfr,
            minLambda = afrToLambda(correctedMinAfr),
            maxLambda = afrToLambda(correctedMaxAfr),
            activeAboveCoolantF = config.activeAboveCoolantF.coerceIn(-40, 419),
            activeAboveRpm = config.activeAboveRpm.coerceIn(0, 25500),
            activeBelowTpsPercent = config.activeBelowTpsPercent.coerceIn(0.0, 100.0),
            activeBelowMapKpa = config.activeBelowMapKpa.coerceIn(0, 511),
            activeAboveMapKpa = config.activeAboveMapKpa.coerceIn(0, 511),
            delayAfterStartSeconds = config.delayAfterStartSeconds.coerceIn(0, 120),
            pidProportional = (if (pidEnabled) config.pidProportional else 0).coerceIn(0, 200),
            pidIntegral = (if (pidEnabled) config.pidIntegral else 0).coerceIn(0, 200),
            pidDerivative = (if (pidEnabled) config.pidDerivative else 0).coerceIn(0, 200),
        )
    }

    private fun syncWindow(config: ClosedLoopCorrectionConfig): ClosedLoopCorrectionConfig = sanitize(
        config.copy(
            minLambda = afrToLambda(config.minAfr),
            maxLambda = afrToLambda(config.maxAfr),
        )
    )

    private fun afrToLambda(afr: Double): Double = afr / STOICH_AFR

    private fun lambdaToAfr(lambda: Double): Double = lambda.coerceIn(7.0 / STOICH_AFR, 25.0 / STOICH_AFR) * STOICH_AFR

    private fun rawAfrToDisplay(raw: Int): Double = raw.coerceIn(0, 255) * 0.1

    private fun displayAfrToRaw(afr: Double): Int = (afr.coerceIn(7.0, 25.0) * 10.0).roundToInt()

    private fun rawCoolantToFahrenheit(raw: Int): Int = (raw * 1.8 - 40.0).roundToInt()

    private fun fahrenheitToRawCoolant(fahrenheit: Int): Int = ((fahrenheit + 40.0) / 1.8).roundToInt()

    private fun readU8(page: ByteArray, offset: Int): Int {
        return page.getOrNull(offset)?.toInt()?.and(0xFF) ?: 0
    }

    private fun writeU8(page: ByteArray, offset: Int, value: Int) {
        if (offset >= page.size) return
        page[offset] = (value and 0xFF).toByte()
    }

    private fun readBits(page: ByteArray, offset: Int, shift: Int, width: Int): Int {
        if (offset >= page.size) return 0
        val mask = (1 shl width) - 1
        return (page[offset].toInt() shr shift) and mask
    }

    private fun writeBits(page: ByteArray, offset: Int, shift: Int, width: Int, value: Int) {
        if (offset >= page.size) return
        val mask = ((1 shl width) - 1) shl shift
        val current = page[offset].toInt() and 0xFF
        val updated = (current and mask.inv()) or ((value shl shift) and mask)
        page[offset] = (updated and 0xFF).toByte()
    }
}
