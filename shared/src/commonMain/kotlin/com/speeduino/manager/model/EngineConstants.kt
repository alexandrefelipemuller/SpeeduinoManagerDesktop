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

    // Oddfire Angles (if Engine Type = Odd fire)
    val channel2Angle: Int = 180,     // Offset 51: oddfire2 (U16)
    val channel3Angle: Int = 270,     // Offset 53: oddfire3 (U16)
    val channel4Angle: Int = 360      // Offset 55: oddfire4 (U16)
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
}

// Enums

enum class Algorithm(val displayName: String) {
    SPEED_DENSITY("Speed Density (MAP)"),
    ALPHA_N("Alpha-N (TPS)"),
    IMAP_EMAP("IMAP/EMAP");

    companion object {
        fun fromBits(bits: Int): Algorithm {
            return when (bits) {
                0 -> SPEED_DENSITY
                1 -> ALPHA_N
                2 -> IMAP_EMAP
                else -> SPEED_DENSITY
            }
        }
    }

    fun toBits(): Int {
        return when (this) {
            SPEED_DENSITY -> 0
            ALPHA_N -> 1
            IMAP_EMAP -> 2
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
