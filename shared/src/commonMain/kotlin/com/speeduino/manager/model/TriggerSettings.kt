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
    val reSyncEveryCycle: Boolean,
    val extraFields: Map<String, String> = emptyMap(),
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
        const val MS2_PAGE_NUMBER: Byte = 0x04

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

        fun fromMs2PageData(data: ByteArray): TriggerSettings {
            require(data.size >= 1024) { "MS2 page 0x04 deve ter 1024 bytes" }

            val triggerAngle = readS16Be(data, 42)
            val skipPulses = readU8(data, 1)
            val primaryCapture = readBits(data, 2, 0, 0)
            val triggerTeeth = readU16Be(data, 966)
            val missingTeeth = readU8(data, 968)
            val wheelConfig = readBits(data, 988, 2, 3)
            val wheelEdge = readBits(data, 988, 4, 5)
            val wheelSpeed = readBits(data, 988, 1, 1)
            val levelForPhase1High = readBits(data, 988, 0, 0) == 1
            val sparkMode = readBits(data, 989, 0, 5)
            val resyncFlag = readBits(data, 577, 1, 1) == 1
            val filterMode = readBits(data, 997, 4, 5)

            return TriggerSettings(
                triggerAngleDeg = (triggerAngle * 0.1f).toInt(),
                triggerAngleMultiplier = 1,
                triggerPattern = sparkMode,
                primaryBaseTeeth = triggerTeeth,
                missingTeeth = missingTeeth,
                primaryTriggerSpeed = if (wheelSpeed == 1) TriggerSpeed.CAM else TriggerSpeed.CRANK,
                triggerEdge = if (primaryCapture == 1) SignalEdge.RISING else SignalEdge.FALLING,
                secondaryTriggerEdge = if (wheelEdge == 1) SignalEdge.RISING else SignalEdge.FALLING,
                secondaryTriggerType = wheelConfig,
                levelForFirstPhaseHigh = levelForPhase1High,
                skipRevolutions = skipPulses,
                triggerFilter = when (filterMode) {
                    2 -> TriggerFilter.MEDIUM
                    3 -> TriggerFilter.AGGRESSIVE
                    else -> TriggerFilter.OFF
                },
                reSyncEveryCycle = resyncFlag,
            )
        }

        fun fromRusefiMainPage(data: ByteArray, schemaId: String = "rusefi-main"): TriggerSettings {
            val isF407Discovery = schemaId == "rusefi-f407-discovery"
            val minimumSize = if (isF407Discovery) 1658 else 1686
            require(data.size >= minimumSize) { "rusEFI page 0x0000 deve ter pelo menos $minimumSize bytes" }
            val noiseFilterOffset = if (isF407Discovery) 768 else 776
            val noiseFilterBit = if (isF407Discovery) 19 else 17
            val secondaryFlagsOffset = if (isF407Discovery) 1344 else 1356

            val triggerTypeIndex = readBitsU32Le(data, if (isF407Discovery) 544 else 552, 0, 6)
            val customTotalTeeth = readS32Le(data, if (isF407Discovery) 548 else 556)
            val customMissingTeeth = readS32Le(data, if (isF407Discovery) 552 else 560)
            val triggerAngle = readF32Le(data, if (isF407Discovery) 484 else 488)
            val primaryInput = readBitsU16Le(data, if (isF407Discovery) 740 else 748, 0, 8)
            val secondaryInput = readBitsU16Le(data, if (isF407Discovery) 742 else 750, 0, 8)
            val noiseless = readBitU32Le(data, noiseFilterOffset, noiseFilterBit)
            val secondaryFalling = readBitU32Le(data, secondaryFlagsOffset, 14)
            val skippedWheelOnCam = readBitU32Le(data, secondaryFlagsOffset, 25)
            val vvtMode1 = readBitsU8(data, if (isF407Discovery) 1656 else 1684, 0, 5)
            val vvtMode2 = readBitsU8(data, if (isF407Discovery) 1657 else 1685, 0, 5)

            val inferredTeeth = inferTriggerTotalTeeth(triggerTypeIndex)
            val inferredMissing = inferTriggerMissingTeeth(triggerTypeIndex)
            val totalTeeth = if (customTotalTeeth > 0) customTotalTeeth else inferredTeeth
            val missingTeeth = if (customMissingTeeth >= 0) customMissingTeeth else inferredMissing

            return TriggerSettings(
                triggerAngleDeg = triggerAngle.toInt(),
                triggerAngleMultiplier = 1,
                triggerPattern = triggerTypeIndex,
                primaryBaseTeeth = totalTeeth,
                missingTeeth = missingTeeth,
                primaryTriggerSpeed = if (skippedWheelOnCam) TriggerSpeed.CAM else TriggerSpeed.CRANK,
                triggerEdge = SignalEdge.RISING,
                secondaryTriggerEdge = if (secondaryFalling) SignalEdge.FALLING else SignalEdge.RISING,
                secondaryTriggerType = secondaryInput,
                levelForFirstPhaseHigh = skippedWheelOnCam,
                skipRevolutions = 0,
                triggerFilter = if (noiseless) TriggerFilter.MEDIUM else TriggerFilter.OFF,
                reSyncEveryCycle = false,
                extraFields = linkedMapOf(
                    "rusefi_trigger_type" to triggerTypeName(triggerTypeIndex),
                    "rusefi_trigger_primary_input" to triggerInputName(primaryInput),
                    "rusefi_trigger_secondary_input" to triggerInputName(secondaryInput),
                    "rusefi_trigger_skipped_wheel_location" to if (skippedWheelOnCam) "On camshaft" else "On crankshaft",
                    "rusefi_trigger_noise_filter" to if (noiseless) "Enabled" else "Disabled",
                    "rusefi_trigger_custom_total" to customTotalTeeth.coerceAtLeast(0).toString(),
                    "rusefi_trigger_custom_missing" to customMissingTeeth.coerceAtLeast(0).toString(),
                    "rusefi_trigger_vvt_mode_1" to vvtModeName(vvtMode1),
                    "rusefi_trigger_vvt_mode_2" to vvtModeName(vvtMode2),
                ),
            )
        }

        private fun readU8(data: ByteArray, offset: Int): Int = data[offset].toInt() and 0xFF

        private fun readU16Le(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        }

        private fun readU32Le(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
        }

        private fun readU16Be(data: ByteArray, offset: Int): Int {
            return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        }

        private fun readS32Le(data: ByteArray, offset: Int): Int = readU32Le(data, offset)

        private fun readF32Le(data: ByteArray, offset: Int): Float {
            return Float.fromBits(readU32Le(data, offset))
        }

        private fun readS16Be(data: ByteArray, offset: Int): Int {
            val raw = readU16Be(data, offset)
            return if (raw and 0x8000 != 0) raw - 0x10000 else raw
        }

        private fun readBits(data: ByteArray, offset: Int, startBit: Int, endBit: Int): Int {
            val width = endBit - startBit + 1
            val mask = (1 shl width) - 1
            return (readU8(data, offset) shr startBit) and mask
        }

        private fun readBitsU8(data: ByteArray, offset: Int, startBit: Int, endBit: Int): Int {
            return (readU8(data, offset) shr startBit) and ((1 shl (endBit - startBit + 1)) - 1)
        }

        private fun readBitsU16Le(data: ByteArray, offset: Int, startBit: Int, endBit: Int): Int {
            return (readU16Le(data, offset) shr startBit) and ((1 shl (endBit - startBit + 1)) - 1)
        }

        private fun readBitsU32Le(data: ByteArray, offset: Int, startBit: Int, endBit: Int): Int {
            return (readU32Le(data, offset) ushr startBit) and ((1 shl (endBit - startBit + 1)) - 1)
        }

        private fun readBitU32Le(data: ByteArray, offset: Int, bit: Int): Boolean {
            return ((readU32Le(data, offset) ushr bit) and 0x01) == 1
        }

        private fun inferTriggerTotalTeeth(index: Int): Int = when (index) {
            8 -> 60
            9 -> 36
            23 -> 36
            48 -> 36
            69 -> 32
            70 -> 36
            71 -> 36
            else -> 0
        }

        private fun inferTriggerMissingTeeth(index: Int): Int = when (index) {
            8 -> 2
            9 -> 1
            23 -> 2
            48 -> 2
            69 -> 2
            70 -> 2
            71 -> 2
            else -> 0
        }

        private fun triggerTypeName(index: Int): String = when (index) {
            0 -> "Custom toothed wheel"
            8 -> "60-2"
            9 -> "36-1"
            11 -> "Single Tooth"
            23 -> "36-2-2-2"
            48 -> "36-2"
            57 -> "Kawa KX450F"
            69 -> "32-2"
            70 -> "36-2-1"
            71 -> "36-2-1-1"
            else -> "Trigger $index"
        }

        private fun triggerInputName(index: Int): String {
            return if (index == 0) "Not assigned" else "Input $index"
        }

        private fun vvtModeName(index: Int): String = when (index) {
            0 -> "Inactive"
            1 -> "Single Tooth"
            2 -> "Toyota 3 Tooth / 2JZ"
            3 -> "Miata NB2"
            9 -> "Nissan VQ"
            10 -> "Honda K Intake"
            16 -> "Honda K Exhaust"
            else -> "Mode $index"
        }

        private fun writeU8(data: ByteArray, offset: Int, value: Int) {
            data[offset] = value.coerceIn(0, 0xFF).toByte()
        }

        private fun writeU16Be(data: ByteArray, offset: Int, value: Int) {
            data[offset] = ((value shr 8) and 0xFF).toByte()
            data[offset + 1] = (value and 0xFF).toByte()
        }

        private fun writeS16Be(data: ByteArray, offset: Int, value: Int) {
            val clamped = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            writeU16Be(data, offset, clamped and 0xFFFF)
        }

        private fun writeBits(data: ByteArray, offset: Int, startBit: Int, endBit: Int, value: Int) {
            val width = endBit - startBit + 1
            val mask = ((1 shl width) - 1) shl startBit
            val current = readU8(data, offset)
            writeU8(data, offset, (current and mask.inv()) or ((value shl startBit) and mask))
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

    fun toMs2PageData(basePage: ByteArray): ByteArray {
        require(basePage.size >= 1024) { "MS2 page 0x04 deve ter 1024 bytes" }

        val data = basePage.copyOf()

        writeS16Be(data, 42, (triggerAngleDeg * 10).coerceIn(-900, 1800))
        writeU8(data, 1, skipRevolutions.coerceIn(0, 255))
        writeBits(data, 2, 0, 0, if (triggerEdge == SignalEdge.RISING) 1 else 0)
        writeU16Be(data, 966, primaryBaseTeeth.coerceIn(0, 255))
        writeU8(data, 968, missingTeeth.coerceIn(0, 4))
        writeBits(data, 988, 2, 3, secondaryTriggerType.coerceIn(0, 3))

        val secondaryEdgeBits = when (secondaryTriggerEdge) {
            SignalEdge.RISING -> 1
            SignalEdge.FALLING -> 2
        }
        writeBits(data, 988, 4, 5, secondaryEdgeBits)
        writeBits(data, 988, 1, 1, if (primaryTriggerSpeed == TriggerSpeed.CAM) 1 else 0)
        writeBits(data, 988, 0, 0, if (levelForFirstPhaseHigh) 1 else 0)
        writeBits(data, 989, 0, 5, triggerPattern.coerceIn(0, 63))
        writeBits(data, 577, 1, 1, if (reSyncEveryCycle) 1 else 0)

        val filterBits = when (triggerFilter) {
            TriggerFilter.OFF -> 0
            TriggerFilter.WEAK -> 2
            TriggerFilter.MEDIUM -> 2
            TriggerFilter.AGGRESSIVE -> 3
        }
        writeBits(data, 997, 4, 5, filterBits)

        return data
    }
}
