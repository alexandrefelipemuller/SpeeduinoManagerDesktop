package com.speeduino.manager.model

/**
 * Engine Constants - Page 1 (128 bytes)
 *
 * Configurações físicas do motor e parâmetros da ECU
 */
data class EngineConstants(
    // Required Fuel & Reference Voltage
    val reqFuel: Float,              // Offset 24: Required fuel (ms), scale 0.1
    val batteryVoltage: Float = 12.0f, // Reference voltage (not in page 1, using default)

    // Engine & Injection Parameters (Offset 36, 37)
    val algorithm: Algorithm,         // Control algorithm (Speed Density / Alpha-N)
    val squirtsPerCycle: Int,         // Offset 25: divider (1, 2, 4)
    val injectorStaging: InjectorStaging, // Offset 26 bit 0: Alternating/Simultaneous
    val engineStroke: EngineStroke,   // Offset 36 bit 2: Four-stroke/Two-stroke
    val numberOfCylinders: Int,       // Offset 36 bits 4-7: 1-12 cylinders
    val injectorPortType: InjectorPortType, // Offset 36 bit 3: Port/Throttle Body
    val numberOfInjectors: Int,       // Offset 37 bits 4-7: 1-8 injectors
    val engineType: EngineType = EngineType.EVEN_FIRE, // Even fire / Odd fire

    // Board & Advanced Settings
    val boardLayout: String = "Speeduino v0.4", // Not in page 1, using default
    val stoichiometricRatio: Float,   // Offset 50: Stoich ratio, scale 0.1
    val injectorLayout: InjectorLayout = InjectorLayout.SEQUENTIAL, // Not in page 1
    val mapSampleMethod: MapSampleMethod, // Offset 36 bits 0-1
    val mapSwitchPoint: Int = 4000,   // Not in page 1, using default
    val mapSampleEvents: Int = 1,
    val includeAfrTarget: Boolean = false,
    val engineDisplacementCc: Int = 2000,

    // Oddfire Angles (if Engine Type = Odd fire)
    val channel2Angle: Int = 180,     // Offset 51: oddfire2 (U16)
    val channel3Angle: Int = 270,     // Offset 53: oddfire3 (U16)
    val channel4Angle: Int = 360,      // Offset 55: oddfire4 (U16)
    val extraFields: Map<String, String> = emptyMap(),
) {
    companion object {
        /**
         * Parse Page 1 (128 bytes) into EngineConstants
         */
        fun fromPage1(data: ByteArray): EngineConstants {
            require(data.size >= 128) { "Page 1 deve ter 128 bytes" }

            // Offset 24: reqFuel (U08, scale 0.1)
            val reqFuel = (data[24].toInt() and 0xFF) * 0.1f

            // Offset 25: divider (squirts per cycle)
            val divider = data[25].toInt() and 0xFF

            // Offset 26: alternate (bit 0)
            val alternate = (data[26].toInt() and 0x01) != 0

            // Offset 36: Config1 byte
            val config1 = data[36].toInt() and 0xFF
            val mapSample = (config1 and 0x03) // bits 0-1
            val twoStroke = (config1 and 0x04) != 0 // bit 2
            val injType = (config1 and 0x08) != 0 // bit 3
            val nCylinders = (config1 shr 4) and 0x0F // bits 4-7

            // Offset 37: Config2 byte
            val config2 = data[37].toInt() and 0xFF
            val algorithmBits = config2 and 0x07 // bits 0-2
            val nInjectors = (config2 shr 4) and 0x0F // bits 4-7

            // Offset 50: stoich (U08, scale 0.1)
            val stoich = (data[50].toInt() and 0xFF) * 0.1f

            // Offset 51-56: oddfire angles (U16 little-endian)
            val oddfire2 = ((data[52].toInt() and 0xFF) shl 8) or (data[51].toInt() and 0xFF)
            val oddfire3 = ((data[54].toInt() and 0xFF) shl 8) or (data[53].toInt() and 0xFF)
            val oddfire4 = ((data[56].toInt() and 0xFF) shl 8) or (data[55].toInt() and 0xFF)

            return EngineConstants(
                reqFuel = reqFuel,
                algorithm = Algorithm.fromBits(algorithmBits),
                squirtsPerCycle = divider,
                injectorStaging = if (alternate) InjectorStaging.ALTERNATING else InjectorStaging.SIMULTANEOUS,
                engineStroke = if (twoStroke) EngineStroke.TWO_STROKE else EngineStroke.FOUR_STROKE,
                numberOfCylinders = mapCylinders(nCylinders),
                injectorPortType = if (injType) InjectorPortType.THROTTLE_BODY else InjectorPortType.PORT,
                numberOfInjectors = mapInjectors(nInjectors),
                stoichiometricRatio = stoich,
                mapSampleMethod = MapSampleMethod.fromBits(mapSample),
                channel2Angle = oddfire2,
                channel3Angle = oddfire3,
                channel4Angle = oddfire4
            )
        }

        /**
         * Parse MS2/Extra TunerStudio page 1 (internal page 0x04, 1024 bytes)
         * into the shared EngineConstants model used by the app.
         */
        fun fromMs2Page1(data: ByteArray): EngineConstants {
            require(data.size >= 1024) { "MS2 page 1 deve ter 1024 bytes" }

            val reqFuel = readU16Be(data, 608) * 0.001f
            val batteryVoltage = readS16Be(data, 522) * 0.1f
            val nCylinders = readBits(data, 0, 0, 4)
            val engineTypeOdd = readBits(data, 2, 3, 3) == 1
            val algorithmBits = readBits(data, 630, 0, 2)
            val divider = readU8(data, 610)
            val alternating = readBits(data, 611, 0, 0) == 1
            val strokeBits = readBits(data, 617, 0, 1)
            val nInjectors = readBits(data, 619, 0, 4)
            val oddFireAngle = (readU16Be(data, 620) * 0.1f).toInt()
            val mapSampleMode = readBits(data, 601, 2, 2)
            val mapSampleEvents = when (readBits(data, 601, 0, 1)) {
                1 -> 2
                2 -> 4
                else -> 1
            }
            val mapSampleAngle = (readS16Be(data, 584) * 0.1f).toInt()
            val loadStoich = readBits(data, 733, 3, 3) == 1
            val stoich = readS16Be(data, 662) * 0.1f
            val engineSize = readU16Be(data, 679)
            val boardType = when (readU8(data, 740)) {
                1 -> "MS2-X"
                2 -> "Router"
                3 -> "GPIO"
                else -> "MS2-X"
            }

            return EngineConstants(
                reqFuel = reqFuel,
                batteryVoltage = batteryVoltage,
                algorithm = Algorithm.fromMs2Bits(algorithmBits),
                squirtsPerCycle = divider.coerceAtLeast(1),
                injectorStaging = if (alternating) InjectorStaging.ALTERNATING else InjectorStaging.SIMULTANEOUS,
                engineStroke = if (strokeBits == 1) EngineStroke.TWO_STROKE else EngineStroke.FOUR_STROKE,
                numberOfCylinders = nCylinders.coerceAtLeast(1),
                injectorPortType = InjectorPortType.PORT,
                numberOfInjectors = nInjectors.coerceAtLeast(1),
                engineType = if (engineTypeOdd) EngineType.ODD_FIRE else EngineType.EVEN_FIRE,
                boardLayout = boardType,
                stoichiometricRatio = stoich,
                injectorLayout = InjectorLayout.SEQUENTIAL,
                mapSampleMethod = if (mapSampleMode == 1) {
                    MapSampleMethod.EVENT_AVERAGE
                } else {
                    MapSampleMethod.CYCLE_MINIMUM
                },
                mapSwitchPoint = mapSampleAngle,
                mapSampleEvents = mapSampleEvents,
                includeAfrTarget = loadStoich,
                engineDisplacementCc = engineSize,
                channel2Angle = oddFireAngle,
            )
        }

        /**
         * Parse rusEFI main configuration page (page 0x0000) into the shared model.
         * Extra rusEFI-only values are carried in [EngineConstants.extraFields].
         */
        fun fromRusefiMainPage(data: ByteArray, schemaId: String = "rusefi-main"): EngineConstants {
            val isF407Discovery = schemaId == "rusefi-f407-discovery"
            val minimumSize = if (isF407Discovery) 548 else 556
            require(data.size >= minimumSize) { "rusEFI page 0x0000 deve ter pelo menos $minimumSize bytes" }

            val engineTypeIndex = readBitsU16Le(data, 0, 0, 6)
            val injectorFlow = readF32Le(data, if (isF407Discovery) 72 else 76)
            val tpsMin = readS16Le(data, if (isF407Discovery) 196 else 200) * if (isF407Discovery) 0.0048828125f else 0.005f
            val tpsMax = readS16Le(data, if (isF407Discovery) 198 else 202) * if (isF407Discovery) 0.0048828125f else 0.005f
            val crankingRpm = readS16Le(data, if (isF407Discovery) 204 else 208)
            val displacementLiters = readF32Le(data, if (isF407Discovery) 432 else 436)
            val cylindersCount = readU32Le(data, 440).coerceIn(1, 12)
            val firingOrderIndex = readU8(data, 444)
            val injectionModeIndex = readBitsU8(data, if (isF407Discovery) 455 else 459, 0, 1)
            val ignitionModeIndex = readBitsU8(data, if (isF407Discovery) 472 else 476, 0, 1)
            val triggerAngleOffset = readF32Le(data, if (isF407Discovery) 484 else 488)
            val triggerTypeIndex = readBitsU32Le(data, if (isF407Discovery) 544 else 552, 0, 6)

            return EngineConstants(
                reqFuel = 0f,
                batteryVoltage = 12.0f,
                algorithm = Algorithm.SPEED_DENSITY,
                squirtsPerCycle = 1,
                injectorStaging = if (injectionModeIndex == 0) {
                    InjectorStaging.SIMULTANEOUS
                } else {
                    InjectorStaging.ALTERNATING
                },
                engineStroke = EngineStroke.FOUR_STROKE,
                numberOfCylinders = cylindersCount,
                injectorPortType = InjectorPortType.PORT,
                numberOfInjectors = cylindersCount,
                engineType = EngineType.EVEN_FIRE,
                boardLayout = "rusEFI",
                stoichiometricRatio = 14.7f,
                injectorLayout = when (injectionModeIndex) {
                    1 -> InjectorLayout.SEQUENTIAL
                    2 -> InjectorLayout.BATCH
                    else -> InjectorLayout.PAIRED
                },
                mapSampleMethod = MapSampleMethod.CYCLE_AVERAGE,
                mapSwitchPoint = triggerAngleOffset.toInt(),
                engineDisplacementCc = (displacementLiters * 1000f).toInt(),
                extraFields = linkedMapOf(
                    "rusefi_engine_type" to engineTypeName(engineTypeIndex),
                    "rusefi_injector_flow" to formatDecimal(injectorFlow, 2),
                    "rusefi_displacement_l" to formatDecimal(displacementLiters, 3),
                    "rusefi_firing_order" to firingOrderName(firingOrderIndex),
                    "rusefi_injection_mode" to injectionModeName(injectionModeIndex),
                    "rusefi_ignition_mode" to ignitionModeName(ignitionModeIndex),
                    "rusefi_trigger_angle" to formatDecimal(triggerAngleOffset, 1),
                    "rusefi_trigger_type" to triggerTypeName(triggerTypeIndex),
                    "rusefi_tps_min" to formatDecimal(tpsMin, 3),
                    "rusefi_tps_max" to formatDecimal(tpsMax, 3),
                    "rusefi_cranking_rpm" to crankingRpm.toString(),
                ),
            )
        }

        /**
         * Map cylinder bits to actual number
         * bits 4-7: "INVALID","1","2","3","4","5","6","INVALID","8",...
         */
        private fun mapCylinders(bits: Int): Int {
            return when (bits) {
                1 -> 1
                2 -> 2
                3 -> 3
                4 -> 4
                5 -> 5
                6 -> 6
                8 -> 8
                else -> 4 // default
            }
        }

        /**
         * Map injector bits to actual number
         * Same mapping as cylinders
         */
        private fun mapInjectors(bits: Int): Int {
            return when (bits) {
                1 -> 1
                2 -> 2
                3 -> 3
                4 -> 4
                5 -> 5
                6 -> 6
                8 -> 8
                else -> 4 // default
            }
        }

        /**
         * Map cylinder count to bits for encoding
         */
        private fun cylindersToBits(count: Int): Int {
            return when (count) {
                1 -> 1
                2 -> 2
                3 -> 3
                4 -> 4
                5 -> 5
                6 -> 6
                8 -> 8
                else -> 4 // default
            }
        }

        /**
         * Map injector count to bits for encoding
         */
        private fun injectorsToBits(count: Int): Int {
            return when (count) {
                1 -> 1
                2 -> 2
                3 -> 3
                4 -> 4
                5 -> 5
                6 -> 6
                8 -> 8
                else -> 4 // default
            }
        }

        private fun readU8(data: ByteArray, offset: Int): Int = data[offset].toInt() and 0xFF

        private fun readU16Be(data: ByteArray, offset: Int): Int {
            return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        }

        private fun readU16Le(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        }

        private fun readS16Be(data: ByteArray, offset: Int): Int {
            val raw = readU16Be(data, offset)
            return if (raw and 0x8000 != 0) raw - 0x10000 else raw
        }

        private fun readS16Le(data: ByteArray, offset: Int): Int {
            val raw = readU16Le(data, offset)
            return if (raw and 0x8000 != 0) raw - 0x10000 else raw
        }

        private fun readU32Le(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
        }

        private fun readF32Le(data: ByteArray, offset: Int): Float {
            return Float.fromBits(readU32Le(data, offset))
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

        private fun formatDecimal(value: Float, decimals: Int): String {
            return "%.${decimals}f".format(value)
        }

        private fun engineTypeName(index: Int): String = when (index) {
            92 -> "SIMULATOR_CONFIG"
            99 -> "MINIMAL_PINS"
            30 -> "PROTEUS_ANALOG_PWM_TEST"
            73 -> "PROTEUS_STIM_QC"
            103 -> "PROTEUS_NISSAN_VQ35"
            else -> "Type $index"
        }

        private fun firingOrderName(index: Int): String = when (index) {
            0 -> "One Cylinder"
            1 -> "1-3-4-2"
            2 -> "1-2-4-3"
            3 -> "1-3-2-4"
            8 -> "1-2"
            9 -> "1-2-3-4-5-6"
            17 -> "1-4-3-2"
            25 -> "1-2-3-4-5-6-7-8"
            else -> "Order $index"
        }

        private fun injectionModeName(index: Int): String = when (index) {
            0 -> "Simultaneous"
            1 -> "Sequential"
            2 -> "Batch"
            3 -> "Single Point"
            else -> "Mode $index"
        }

        private fun ignitionModeName(index: Int): String = when (index) {
            0 -> "Single Coil"
            1 -> "Individual Coils"
            2 -> "Wasted Spark"
            3 -> "Two Distributors"
            else -> "Mode $index"
        }

        private fun triggerTypeName(index: Int): String = when (index) {
            0 -> "Custom toothed wheel"
            8 -> "60-2"
            9 -> "36-1"
            11 -> "Single Tooth"
            23 -> "36-2-2-2"
            48 -> "36-2"
            69 -> "32-2"
            70 -> "36-2-1"
            71 -> "36-2-1-1"
            else -> "Trigger $index"
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
            val encoded = (current and mask.inv()) or ((value shl startBit) and mask)
            writeU8(data, offset, encoded)
        }
    }

    /**
     * Convert EngineConstants back to Page 1 byte array (128 bytes)
     */
    fun toPage1(): ByteArray {
        val data = ByteArray(128) { 0 }
        return applyToPage1(data)
    }

    /**
     * Apply EngineConstants changes on top of an existing Page 1 buffer.
     * This avoids zeroing unrelated settings stored in the same page.
     */
    fun applyToPage1(basePage: ByteArray): ByteArray {
        require(basePage.size >= 128) { "Page 1 deve ter 128 bytes" }

        val data = basePage.copyOf()

        // Offset 24: reqFuel (U08, scale 0.1)
        data[24] = (reqFuel / 0.1f).toInt().coerceIn(0, 255).toByte()

        // Offset 25: divider (squirts per cycle)
        data[25] = squirtsPerCycle.coerceIn(0, 255).toByte()

        // Offset 26: alternate (bit 0)
        val stagingBit = if (injectorStaging == InjectorStaging.ALTERNATING) 0x01 else 0x00
        data[26] = ((data[26].toInt() and 0xFE) or stagingBit).toByte()

        // Offset 36: Config1 byte
        var config1 = 0
        config1 = config1 or mapSampleMethod.toBits()
        if (engineStroke == EngineStroke.TWO_STROKE) config1 = config1 or 0x04
        if (injectorPortType == InjectorPortType.THROTTLE_BODY) config1 = config1 or 0x08
        config1 = config1 or (cylindersToBits(numberOfCylinders) shl 4)
        data[36] = config1.toByte()

        // Offset 37: Config2 byte
        val config2Base = data[37].toInt() and 0xFF
        var config2 = config2Base and 0x08 // Preserve unknown bit 3
        config2 = config2 or algorithm.toBits()
        config2 = config2 or (injectorsToBits(numberOfInjectors) shl 4)
        data[37] = config2.toByte()

        // Offset 50: stoich (U08, scale 0.1)
        data[50] = (stoichiometricRatio / 0.1f).toInt().coerceIn(0, 255).toByte()

        // Offset 51-56: oddfire angles (U16 little-endian)
        data[51] = (channel2Angle and 0xFF).toByte()
        data[52] = ((channel2Angle shr 8) and 0xFF).toByte()
        data[53] = (channel3Angle and 0xFF).toByte()
        data[54] = ((channel3Angle shr 8) and 0xFF).toByte()
        data[55] = (channel4Angle and 0xFF).toByte()
        data[56] = ((channel4Angle shr 8) and 0xFF).toByte()

        return data
    }

    /**
     * Apply the shared EngineConstants model on top of the MS2/Extra page 1
     * calibration buffer (TS page 1 / internal 0x04).
     */
    fun applyToMs2Page1(basePage: ByteArray): ByteArray {
        require(basePage.size >= 1024) { "MS2 page 1 deve ter 1024 bytes" }

        val data = basePage.copyOf()

        writeU16Be(data, 608, (reqFuel / 0.001f).toInt())
        writeS16Be(data, 522, (batteryVoltage / 0.1f).toInt())
        writeBits(data, 0, 0, 4, numberOfCylinders.coerceIn(1, 16))
        writeBits(data, 2, 3, 3, if (engineType == EngineType.ODD_FIRE) 1 else 0)
        writeBits(data, 630, 0, 2, algorithm.toMs2Bits())
        writeU8(data, 610, squirtsPerCycle.coerceIn(1, 255))
        writeBits(data, 611, 0, 0, if (injectorStaging == InjectorStaging.ALTERNATING) 1 else 0)
        writeBits(data, 617, 0, 1, if (engineStroke == EngineStroke.TWO_STROKE) 1 else 0)
        writeBits(data, 619, 0, 4, numberOfInjectors.coerceIn(1, 16))
        writeU16Be(data, 620, (channel2Angle.coerceIn(0, 720) * 10))
        writeBits(data, 601, 2, 2, if (mapSampleMethod == MapSampleMethod.EVENT_AVERAGE) 1 else 0)
        val sampleEventsBits = when (mapSampleEvents) {
            2 -> 1
            4 -> 2
            else -> 0
        }
        writeBits(data, 601, 0, 1, sampleEventsBits)
        writeS16Be(data, 584, mapSwitchPoint * 10)
        writeBits(data, 733, 3, 3, if (includeAfrTarget) 1 else 0)
        writeS16Be(data, 662, (stoichiometricRatio / 0.1f).toInt())
        writeU16Be(data, 679, engineDisplacementCc.coerceIn(0, 65535))
        val boardType = when (boardLayout) {
            "Router" -> 2
            "GPIO" -> 3
            "MS2", "MS2-X" -> 1
            else -> 1
        }
        writeU8(data, 740, boardType)

        return data
    }
}

// Enums

enum class Algorithm(val displayName: String) {
    SPEED_DENSITY("Speed Density (MAP)"),
    ALPHA_N("Alpha-N (TPS)"),
    IMAP_EMAP("IMAP/EMAP"),
    PERCENT_BARO("Percent Baro"),
    MAF("MAF"),
    ITB("ITB");

    companion object {
        fun fromBits(bits: Int): Algorithm {
            return when (bits) {
                0 -> SPEED_DENSITY
                1 -> ALPHA_N
                2 -> IMAP_EMAP
                else -> SPEED_DENSITY
            }
        }

        fun fromMs2Bits(bits: Int): Algorithm {
            return when (bits) {
                2 -> PERCENT_BARO
                3 -> ALPHA_N
                5 -> MAF
                6 -> ITB
                else -> SPEED_DENSITY
            }
        }
    }

    fun toBits(): Int {
        return when (this) {
            SPEED_DENSITY -> 0
            ALPHA_N -> 1
            IMAP_EMAP -> 2
            PERCENT_BARO -> 0
            MAF -> 0
            ITB -> 0
        }
    }

    fun toMs2Bits(): Int {
        return when (this) {
            SPEED_DENSITY -> 1
            PERCENT_BARO -> 2
            ALPHA_N -> 3
            MAF -> 5
            ITB -> 6
            IMAP_EMAP -> 1
        }
    }
}

enum class InjectorStaging(val displayName: String) {
    SIMULTANEOUS("Simultaneous"),
    ALTERNATING("Alternating")
}

enum class EngineStroke(val displayName: String) {
    FOUR_STROKE("Four-stroke"),
    TWO_STROKE("Two-stroke")
}

enum class InjectorPortType(val displayName: String) {
    PORT("Port"),
    THROTTLE_BODY("Throttle Body")
}

enum class EngineType(val displayName: String) {
    EVEN_FIRE("Even fire"),
    ODD_FIRE("Odd fire")
}

enum class InjectorLayout(val displayName: String) {
    SEQUENTIAL("Sequential"),
    SEMI_SEQUENTIAL("Semi-Sequential"),
    PAIRED("Paired"),
    BATCH("Batch")
}

enum class MapSampleMethod(val displayName: String) {
    INSTANTANEOUS("Instantaneous"),
    CYCLE_AVERAGE("Cycle Average"),
    CYCLE_MINIMUM("Cycle Minimum"),
    EVENT_AVERAGE("Event Average");

    companion object {
        fun fromBits(bits: Int): MapSampleMethod {
            return when (bits) {
                0 -> INSTANTANEOUS
                1 -> CYCLE_AVERAGE
                2 -> CYCLE_MINIMUM
                3 -> EVENT_AVERAGE
                else -> CYCLE_AVERAGE
            }
        }
    }

    fun toBits(): Int {
        return when (this) {
            INSTANTANEOUS -> 0
            CYCLE_AVERAGE -> 1
            CYCLE_MINIMUM -> 2
            EVENT_AVERAGE -> 3
        }
    }
}
