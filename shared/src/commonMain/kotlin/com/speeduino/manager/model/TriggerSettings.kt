package com.speeduino.manager.model

/**
 * Trigger Settings - Page 4 (128 bytes)
 *
 * Configurações relacionadas ao sensor de rotação (crank/cam) e sincronismo.
 *
 * Somente os campos utilizados na UI são expostos aqui. Outros bytes da
 * página são preservados quando gravamos de volta na ECU.
 */
data class TriggerSettings(
    val triggerAngleDeg: Int,
    val triggerAngleMultiplier: Int,
    val triggerPattern: Int,
    val primaryBaseTeeth: Int,
    val missingTeeth: Int,
    val primaryTriggerSpeed: TriggerSpeed,
    val triggerEdge: SignalEdge,
    val secondaryTriggerEdge: SignalEdge,
    val secondaryTriggerType: Int,
    val levelForFirstPhaseHigh: Boolean,
    val skipRevolutions: Int,
    val triggerFilter: TriggerFilter,
    val reSyncEveryCycle: Boolean
) {
    enum class SignalEdge {
        RISING,
        FALLING
    }

    enum class TriggerSpeed {
        CRANK,
        CAM
    }

    enum class TriggerFilter {
        OFF,
        WEAK,
        MEDIUM,
        AGGRESSIVE
    }

    companion object {
        const val PAGE_NUMBER: Int = 4
        const val PAGE_LENGTH: Int = 128

        private val TRIGGER_PATTERN_LABELS = listOf(
            "Missing Tooth",
            "Basic Distributor",
            "Dual Wheel",
            "GM 7X",
            "4G63 / Miata / 3000GT",
            "GM 24X",
            "Jeep 2000",
            "Audi 135",
            "Honda D17",
            "Miata 99-05",
            "Mazda AU",
            "Non-360 Dual",
            "Nissan 360",
            "Subaru 6/7",
            "Daihatsu +1",
            "Harley EVO",
            "36-2-2-2",
            "36-2-1",
            "DSM 420a",
            "Weber-Marelli",
            "Ford ST170",
            "DRZ400",
            "Chrysler NGC",
            "Yamaha Vmax 1990+",
            "Renix",
            "Rover MEMS",
            "K6A",
            "INVALID",
            "INVALID",
            "INVALID",
            "INVALID",
            "INVALID"
        )

        private val SECONDARY_PATTERN_LABELS = listOf(
            "Single tooth cam",
            "4-1 cam",
            "Poll level",
            "Rover 5-3-2 cam",
            "Toyota 3 Tooth"
        )

        fun fromPageData(data: ByteArray): TriggerSettings {
            require(data.size >= PAGE_LENGTH) { "Page 4 deve ter 128 bytes" }

            val angleRaw = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
            val triggerAngle = if (angleRaw and 0x8000 != 0) angleRaw - 0x10000 else angleRaw

            val triggerAngleMultiplier = data[4].toInt() and 0xFF

            val byte5 = data[5].toInt() and 0xFF
            val triggerEdge = if (byte5 and 0x01 == 0) SignalEdge.RISING else SignalEdge.FALLING
            val primaryTriggerSpeed = if (byte5 and 0x02 == 0) TriggerSpeed.CRANK else TriggerSpeed.CAM
            val triggerPattern = (byte5 shr 3) and 0x1F

            val byte6 = data[6].toInt() and 0xFF
            val secondaryTriggerEdge = if (byte6 and 0x01 == 0) SignalEdge.RISING else SignalEdge.FALLING
            val reSyncEveryCycle = (byte6 and 0x80) != 0

            val trigPatternSecByte = data[8].toInt() and 0xFF
            val secondaryTriggerType = trigPatternSecByte and 0x7F
            val levelForFirstPhaseHigh = (trigPatternSecByte and 0x80) != 0

            val skipRevolutions = data[11].toInt() and 0xFF

            val byte12 = data[12].toInt() and 0xFF
            val triggerFilter = when ((byte12 shr 5) and 0x03) {
                0 -> TriggerFilter.OFF
                1 -> TriggerFilter.WEAK
                2 -> TriggerFilter.MEDIUM
                else -> TriggerFilter.AGGRESSIVE
            }

            val primaryBaseTeeth = data[15].toInt() and 0xFF
            val missingTeeth = data[16].toInt() and 0xFF

            return TriggerSettings(
                triggerAngleDeg = triggerAngle,
                triggerAngleMultiplier = triggerAngleMultiplier,
                triggerPattern = triggerPattern,
                primaryBaseTeeth = primaryBaseTeeth,
                missingTeeth = missingTeeth,
                primaryTriggerSpeed = primaryTriggerSpeed,
                triggerEdge = triggerEdge,
                secondaryTriggerEdge = secondaryTriggerEdge,
                secondaryTriggerType = secondaryTriggerType,
                levelForFirstPhaseHigh = levelForFirstPhaseHigh,
                skipRevolutions = skipRevolutions,
                triggerFilter = triggerFilter,
                reSyncEveryCycle = reSyncEveryCycle
            )
        }
    }

    val triggerPatternLabel: String
        get() = TRIGGER_PATTERN_LABELS.getOrNull(triggerPattern) ?: "Pattern #$triggerPattern"

    val primaryTriggerSpeedLabel: String
        get() = if (primaryTriggerSpeed == TriggerSpeed.CAM) "Cam Speed" else "Crank Speed"

    val triggerEdgeLabel: String
        get() = if (triggerEdge == SignalEdge.RISING) "RISING" else "FALLING"

    val secondaryTriggerEdgeLabel: String
        get() = if (secondaryTriggerEdge == SignalEdge.RISING) "RISING" else "FALLING"

    val secondaryTriggerTypeLabel: String
        get() = SECONDARY_PATTERN_LABELS.getOrNull(secondaryTriggerType)
            ?: "Tipo $secondaryTriggerType"

    val triggerFilterLabel: String
        get() = when (triggerFilter) {
            TriggerFilter.OFF -> "Desligado"
            TriggerFilter.WEAK -> "Weak"
            TriggerFilter.MEDIUM -> "Medium"
            TriggerFilter.AGGRESSIVE -> "Aggressive"
        }

    /**
     * Serializa somente os campos que editamos, preservando o restante dos bytes
     * da página (que contém outras configurações de ignição).
     */
    fun toPageData(basePage: ByteArray): ByteArray {
        require(basePage.size >= PAGE_LENGTH) { "Page 4 deve ter 128 bytes" }

        val data = basePage.copyOf()

        val clampedAngle = triggerAngleDeg.coerceIn(-360, 360)
        val angleRaw = clampedAngle and 0xFFFF
        data[0] = (angleRaw and 0xFF).toByte()
        data[1] = ((angleRaw shr 8) and 0xFF).toByte()

        data[4] = triggerAngleMultiplier.coerceIn(0, 0xFF).toByte()

        var byte5 = data[5].toInt() and 0xFF
        byte5 = if (triggerEdge == SignalEdge.FALLING) byte5 or 0x01 else byte5 and 0xFE
        byte5 = if (primaryTriggerSpeed == TriggerSpeed.CAM) byte5 or 0x02 else byte5 and 0xFD
        byte5 = (byte5 and 0x07) or ((triggerPattern and 0x1F) shl 3)
        data[5] = byte5.toByte()

        var byte6 = data[6].toInt() and 0xFF
        byte6 = if (secondaryTriggerEdge == SignalEdge.FALLING) byte6 or 0x01 else byte6 and 0xFE
        byte6 = if (reSyncEveryCycle) byte6 or 0x80 else byte6 and 0x7F
        data[6] = byte6.toByte()

        var byte8 = data[8].toInt() and 0xFF
        byte8 = (byte8 and 0x80) or (secondaryTriggerType and 0x7F)
        byte8 = if (levelForFirstPhaseHigh) byte8 or 0x80 else byte8 and 0x7F
        data[8] = byte8.toByte()

        data[11] = skipRevolutions.coerceIn(0, 0xFF).toByte()

        var byte12 = data[12].toInt() and 0xFF
        byte12 = (byte12 and 0x9F) or ((triggerFilter.ordinal and 0x03) shl 5)
        data[12] = byte12.toByte()

        data[15] = primaryBaseTeeth.coerceIn(0, 0xFF).toByte()
        data[16] = missingTeeth.coerceIn(0, 0xFF).toByte()

        return data
    }
}
