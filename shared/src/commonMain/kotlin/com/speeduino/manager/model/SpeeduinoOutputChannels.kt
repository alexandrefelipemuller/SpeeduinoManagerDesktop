package com.speeduino.manager.model

import com.speeduino.manager.shared.Logger

/**
 * Speeduino Output Channels - Versionado por tamanho de blockSize
 *
 * Output Channels são os dados ao vivo (live data) enviados pela ECU.
 * O formato evoluiu ao longo das versões:
 * - Legacy (2016): 35 bytes
 * - Modern 2020: 114 bytes
 * - Modern 2024+: 130 bytes
 *
 * Este objeto define os offsets, tipos e escalas de cada campo baseado
 * no tamanho do bloco de dados (ochBlockSize).
 *
 * Usage:
 * ```
 * val fields = SpeeduinoOutputChannels.getDefinition(blockSize = 130)
 * val rpmField = fields.find { it.name == "rpm" }
 * val rpm = rpmField.parse(data)  // Extrai e converte RPM do byte array
 * ```
 */
object SpeeduinoOutputChannels {

    private const val TAG = "OutputChannels"
    private val runtimeDefinitions = mutableMapOf<Int, List<OutputField>>()

    val RUSEFI_2084_FIELDS = listOf(
        OutputField("rpm", 4, DataType.U16, units = "rpm"),
        OutputField("RPMValue", 4, DataType.U16, units = "rpm"),
        OutputField("coolantRaw", 16, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("coolant", 16, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("iatRaw", 18, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("intake", 18, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("tps", 24, DataType.S16, scale = 0.01, units = "%"),
        OutputField("TPSValue", 24, DataType.S16, scale = 0.01, units = "%"),
        OutputField("map", 34, DataType.U16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("MAPValue", 34, DataType.U16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("lambdaValue", 38, DataType.U16, scale = 0.0001, units = "lambda"),
        OutputField("afr", 38, DataType.U16, scale = 0.00147, units = "AFR"),
        OutputField("batteryVoltage", 40, DataType.U16, scale = 0.001, units = "V"),
        OutputField("VBatt", 40, DataType.U16, scale = 0.001, units = "V"),
        OutputField("oilPressure", 42, DataType.U16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("advance", 288, DataType.S16, scale = 0.02, units = "deg"),
        OutputField("ignitionAdvanceCyl1", 288, DataType.S16, scale = 0.02, units = "deg"),
        OutputField("gear", 108, DataType.U08),
        OutputField("detectedGear", 108, DataType.U08),
        OutputField("fuelPressure", 114, DataType.S16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("lowFuelPressure", 114, DataType.S16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("seconds", 88, DataType.U16, units = "s"),
        OutputField("ve", 86, DataType.U16, scale = 0.1, units = "%"),
        OutputField("targetLambda", 910, DataType.U16, scale = 0.0001, units = "lambda"),
        OutputField("afrTarget", 912, DataType.U16, scale = 0.001, units = "AFR"),
        OutputField("targetAFR", 912, DataType.U16, scale = 0.001, units = "AFR"),
        OutputField("vss", 738, DataType.U16, scale = 0.01, units = "km/h"),
        OutputField("vehicleSpeedKph", 738, DataType.U16, scale = 0.01, units = "km/h"),
    )

    val RUSEFI_2068_FIELDS = listOf(
        OutputField("rpm", 4, DataType.U16, units = "rpm"),
        OutputField("RPMValue", 4, DataType.U16, units = "rpm"),
        OutputField("coolantRaw", 14, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("coolant", 14, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("iatRaw", 16, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("intake", 16, DataType.S16, scale = 0.01, units = "°C"),
        OutputField("tps", 22, DataType.S16, scale = 0.01, units = "%"),
        OutputField("TPSValue", 22, DataType.S16, scale = 0.01, units = "%"),
        OutputField("map", 32, DataType.U16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("MAPValue", 32, DataType.U16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("lambdaValue", 36, DataType.U16, scale = 0.0001, units = "lambda"),
        OutputField("afr", 254, DataType.U16, scale = 0.00147, units = "AFR"),
        OutputField("AFRValue", 254, DataType.U16, scale = 0.00147, units = "AFR"),
        OutputField("batteryVoltage", 38, DataType.U16, scale = 0.001, units = "V"),
        OutputField("VBatt", 38, DataType.U16, scale = 0.001, units = "V"),
        OutputField("oilPressure", 40, DataType.U16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("advance", 288, DataType.S16, scale = 0.02, units = "deg"),
        OutputField("ignitionAdvanceCyl1", 288, DataType.S16, scale = 0.02, units = "deg"),
        OutputField("gear", 108, DataType.U08),
        OutputField("detectedGear", 108, DataType.U08),
        OutputField("fuelPressure", 114, DataType.S16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("lowFuelPressure", 114, DataType.S16, scale = 1.0 / 30.0, units = "kPa"),
        OutputField("ve", 86, DataType.U16, scale = 0.1, units = "%"),
    )

    val MS2_212_FIELDS = listOf(
        OutputField("secl", 0, DataType.U16, byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("pulseWidth1", 2, DataType.U16, scale = 0.000666, units = "ms", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("pulseWidth2", 4, DataType.U16, scale = 0.000666, units = "ms", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("rpm", 6, DataType.U16, units = "rpm", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("advance", 8, DataType.S16, scale = 0.1, units = "deg", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("squirt", 10, DataType.U08, units = "bits"),
        OutputField("engine", 11, DataType.U08, units = "bits"),
        OutputField("afrtgt1", 12, DataType.U08, scale = 0.1, units = "AFR"),
        OutputField("afrtgt2", 13, DataType.U08, scale = 0.1, units = "AFR"),
        OutputField("map", 18, DataType.S16, scale = 0.1, units = "kPa", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("tps", 24, DataType.S16, scale = 0.1, units = "%", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("batteryVoltage", 26, DataType.S16, scale = 0.1, units = "V", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("afr", 28, DataType.S16, scale = 0.1, units = "AFR", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("afr2", 30, DataType.S16, scale = 0.1, units = "AFR", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("egoCorrection1", 34, DataType.S16, scale = 0.1, units = "%", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("egoCorrection2", 36, DataType.S16, scale = 0.1, units = "%", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("warmupEnrich", 40, DataType.S16, units = "%", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("accelEnrich", 42, DataType.S16, scale = 0.1, units = "ms", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("baroCorrection", 46, DataType.S16, scale = 0.1, units = "%", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("veCurr1", 50, DataType.S16, scale = 0.1, units = "%", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("iacstep", 54, DataType.S16, byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("dwell", 62, DataType.U16, scale = 0.0666, units = "ms", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("mafload", 64, DataType.S16, scale = 0.1, units = "kPa", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("fuelload", 66, DataType.S16, scale = 0.1, byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("knockRetard", 71, DataType.U08, scale = 0.1, units = "deg"),
        OutputField("egoV", 74, DataType.S16, scale = 0.01, units = "V", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("egoV2", 76, DataType.S16, scale = 0.01, units = "V", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("status1", 78, DataType.U08),
        OutputField("status2", 79, DataType.U08),
        OutputField("status3", 80, DataType.U08),
        OutputField("status4", 81, DataType.U08),
        OutputField("looptime", 82, DataType.U16, scale = 0.6667, units = "us", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("tpsADC", 86, DataType.U16, byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("ignload", 90, DataType.S16, scale = 0.1, byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("synccnt", 94, DataType.U08),
        OutputField("boostduty", 138, DataType.U08, units = "%"),
        OutputField("inj_adv1", 142, DataType.S16, scale = 0.1, units = "deg", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("afrload1", 162, DataType.S16, scale = 0.1, byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("cl_idle_targ_rpm", 170, DataType.S16, units = "rpm", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("boost_targ", 180, DataType.S16, scale = 0.1, units = "kPa", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("adv1", 192, DataType.S16, scale = 0.1, units = "deg", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("adv2", 194, DataType.S16, scale = 0.1, units = "deg", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("adv3", 196, DataType.S16, scale = 0.1, units = "deg", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("oil", 212, DataType.S16, scale = 0.1, units = "bar", byteOrder = EcuByteOrder.BIG_ENDIAN),
    )

    val MEGASPEED_219_FIELDS = MS2_212_FIELDS + listOf(
        OutputField("fuel", 214, DataType.S16, scale = 0.1, units = "bar", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("runsecs", 216, DataType.U16, units = "s", byteOrder = EcuByteOrder.BIG_ENDIAN),
        OutputField("start_retard", 218, DataType.S16, scale = 0.1, units = "deg", byteOrder = EcuByteOrder.BIG_ENDIAN),
    )

    // ========================================
    // LEGACY FIELDS (35 bytes - 2016)
    // ========================================

    /**
     * Legacy Output Channels (35 bytes)
     * Used in Speeduino 2016 and earlier
     */
    val LEGACY_FIELDS = listOf(
        OutputField("secl", 0, DataType.U08, units = "sec"),
        OutputField("squirt", 1, DataType.U08, units = "bits"),  // Renamed to status1 in modern
        OutputField("engine", 2, DataType.U08, units = "bits"),
        OutputField("syncLossCounter", 3, DataType.U08),
        OutputField("map", 4, DataType.U16, units = "kPa"),      // 2 bytes
        OutputField("iatRaw", 6, DataType.U08, units = "°C"),
        OutputField("coolantRaw", 7, DataType.U08, units = "°C"),
        OutputField("batCorrection", 8, DataType.U08, units = "%"),
        OutputField("batteryVoltage", 9, DataType.U08, scale = 0.1, units = "V"),
        OutputField("afr", 10, DataType.U08, scale = 0.1, units = "O2"),
        OutputField("egoCorrection", 11, DataType.U08, units = "%"),
        OutputField("airCorrection", 12, DataType.U08, units = "%"),
        OutputField("warmupEnrich", 13, DataType.U08, units = "%"),
        OutputField("rpm", 14, DataType.U16, units = "rpm"),     // 2 bytes (LSB, MSB)
        OutputField("taeAmount", 16, DataType.U08),
        OutputField("corrections", 17, DataType.U08),
        OutputField("ve", 18, DataType.U08, units = "%"),
        OutputField("afrTarget", 19, DataType.U08, scale = 0.1, units = "AFR"),
        OutputField("pulseWidth", 20, DataType.U16, scale = 0.1, units = "ms"),  // 2 bytes
        OutputField("tpsDOT", 22, DataType.U08),
        OutputField("advance", 23, DataType.U08, units = "deg"),
        OutputField("tps", 24, DataType.U08, units = "%"),
        OutputField("loopsPerSecond", 25, DataType.U16),         // 2 bytes
        OutputField("freeRAM", 27, DataType.U16),                // 2 bytes
        OutputField("boostTarget", 29, DataType.U08),
        OutputField("boostDuty", 30, DataType.U08, units = "%"),
        OutputField("spark", 31, DataType.U08, units = "bits"),
        OutputField("rpmDOT", 32, DataType.U16),                 // 2 bytes
        OutputField("ethanolPct", 34, DataType.U08, units = "%")
    )

    // ========================================
    // MODERN 2020 FIELDS (114 bytes)
    // ========================================

    /**
     * Modern Output Channels (114 bytes)
     * Used in Speeduino 2020-08 and later
     *
     * Adds many new fields:
     * - syncLossCounter explicitly defined
     * - MAP as U16 (2 bytes)
     * - Auxiliary outputs (16 channels)
     * - VVT, flex fuel, etc.
     */
    val MODERN_2020_FIELDS = listOf(
        // Byte 0-3: Status and counters
        OutputField("secl", 0, DataType.U08, units = "sec"),
        OutputField("status1", 1, DataType.U08, units = "bits"),      // Renamed from squirt
        OutputField("engine", 2, DataType.U08, units = "bits"),
        OutputField("syncLossCounter", 3, DataType.U08),

        // Byte 4-5: MAP (U16 - 2 bytes!)
        OutputField("map", 4, DataType.U16, units = "kPa"),

        // Byte 6-13: Sensors
        OutputField("iatRaw", 6, DataType.U08, units = "°C"),
        OutputField("coolantRaw", 7, DataType.U08, units = "°C"),
        OutputField("batCorrection", 8, DataType.U08, units = "%"),
        OutputField("batteryVoltage", 9, DataType.U08, scale = 0.1, units = "V"),
        OutputField("afr", 10, DataType.U08, scale = 0.1, units = "O2"),
        OutputField("egoCorrection", 11, DataType.U08, units = "%"),
        OutputField("airCorrection", 12, DataType.U08, units = "%"),
        OutputField("warmupEnrich", 13, DataType.U08, units = "%"),

        // Byte 14-15: RPM (U16 - LSB first!)
        OutputField("rpm", 14, DataType.U16, units = "rpm"),

        // Byte 16-24: Fuel/Ignition
        OutputField("taeAmount", 16, DataType.U08),
        OutputField("corrections", 17, DataType.U08),
        OutputField("ve", 18, DataType.U08, units = "%"),
        OutputField("afrTarget", 19, DataType.U08, scale = 0.1, units = "AFR"),
        OutputField("pulseWidth", 20, DataType.U16, scale = 0.1, units = "ms"),
        OutputField("tpsDOT", 22, DataType.U08),
        OutputField("advance", 23, DataType.U08, units = "deg"),
        OutputField("tps", 24, DataType.U08, units = "%"),

        // Byte 25-28: Performance
        OutputField("loopsPerSecond", 25, DataType.U16),
        OutputField("freeRAM", 27, DataType.U16),

        // Byte 29-34: Boost/Spark/Ethanol
        OutputField("boostTarget", 29, DataType.U08),
        OutputField("boostDuty", 30, DataType.U08, units = "%"),
        OutputField("spark", 31, DataType.U08, units = "bits"),
        OutputField("rpmDOT", 32, DataType.U16),
        OutputField("ethanolPct", 34, DataType.U08, units = "%"),

        // Byte 35-36: Flex fuel
        OutputField("flexCorrection", 35, DataType.U08),
        OutputField("flexIgnCorrection", 36, DataType.U08),

        // Byte 37-38: Idle
        OutputField("idleLoad", 37, DataType.U08),
        OutputField("testOutputs", 38, DataType.U08),

        // Byte 39: O2_2
        OutputField("afr2", 39, DataType.U08, scale = 0.1, units = "O2"),

        // Byte 40-41: Baro
        OutputField("baro", 40, DataType.U08, units = "kPa"),
        OutputField("canin0", 41, DataType.U16),

        // Byte 43-58: CAN inputs (16 bytes)
        OutputField("canin1", 43, DataType.U16),
        OutputField("canin2", 45, DataType.U16),
        OutputField("canin3", 47, DataType.U16),
        OutputField("canin4", 49, DataType.U16),
        OutputField("canin5", 51, DataType.U16),
        OutputField("canin6", 53, DataType.U16),
        OutputField("canin7", 55, DataType.U16),
        OutputField("canin8", 57, DataType.U16),

        // Byte 59-74: Auxiliary outputs status (16 channels)
        OutputField("aux0", 59, DataType.U08),
        OutputField("aux1", 60, DataType.U08),
        OutputField("aux2", 61, DataType.U08),
        OutputField("aux3", 62, DataType.U08),
        OutputField("aux4", 63, DataType.U08),
        OutputField("aux5", 64, DataType.U08),
        OutputField("aux6", 65, DataType.U08),
        OutputField("aux7", 66, DataType.U08),
        OutputField("aux8", 67, DataType.U08),
        OutputField("aux9", 68, DataType.U08),
        OutputField("aux10", 69, DataType.U08),
        OutputField("aux11", 70, DataType.U08),
        OutputField("aux12", 71, DataType.U08),
        OutputField("aux13", 72, DataType.U08),
        OutputField("aux14", 73, DataType.U08),
        OutputField("aux15", 74, DataType.U08),

        // Byte 75-113: Advanced features
        OutputField("tpsADC", 75, DataType.U08),
        OutputField("errors", 76, DataType.U08),
        OutputField("vvt1Angle", 77, DataType.U08, units = "deg"),
        OutputField("vvt1TargetAngle", 78, DataType.U08, units = "deg"),
        OutputField("vvt1Duty", 79, DataType.U08, units = "%"),
        OutputField("flexBoostCorrection", 80, DataType.U16),
        OutputField("baroCorrection", 82, DataType.U08, units = "%"),
        OutputField("ASEValue", 83, DataType.U08, units = "%"),
        OutputField("vss", 84, DataType.U16, units = "km/h"),
        OutputField("gear", 86, DataType.U08),
        OutputField("fuelPressure", 87, DataType.U08, units = "kPa"),
        OutputField("oilPressure", 88, DataType.U08, units = "kPa"),
        OutputField("wmiPW", 89, DataType.U08, units = "ms"),
        OutputField("status4", 90, DataType.U08, units = "bits"),
        OutputField("vvt2Angle", 91, DataType.U08, units = "deg"),
        OutputField("vvt2TargetAngle", 92, DataType.U08, units = "deg"),
        OutputField("vvt2Duty", 93, DataType.U08, units = "%"),
        OutputField("outputsStatus", 94, DataType.U08, units = "bits"),
        OutputField("fuelTemp", 95, DataType.U08, units = "°C"),
        OutputField("fuelTempCorrection", 96, DataType.U08, units = "%"),
        OutputField("advance1", 97, DataType.U08, units = "deg"),
        OutputField("advance2", 98, DataType.U08, units = "deg"),
        OutputField("TS_SD_Status", 99, DataType.U08),
        OutputField("EMAP", 100, DataType.U16, units = "kPa"),
        OutputField("fanDuty", 102, DataType.U08, units = "%"),
        OutputField("airConStatus", 103, DataType.U08),
        OutputField("airConRequest", 104, DataType.U08),
        OutputField("airConRPM", 105, DataType.U16),
        OutputField("vvtAngle1", 107, DataType.S16, units = "deg"),  // Signed!
        OutputField("vvtAngle2", 109, DataType.S16, units = "deg"),  // Signed!
        OutputField("knockCount", 111, DataType.U08),
        OutputField("knockLevel", 112, DataType.U08),
        OutputField("knockRetard", 113, DataType.U08, units = "deg")
    )

    // ========================================
    // MODERN 2024 FIELDS (130 bytes)
    // ========================================

    /**
     * Modern Output Channels (130 bytes)
     * Used in Speeduino 2024-02 and later
     *
     * Extends 2020 fields with 16 more bytes (114 → 130)
     */
    val MODERN_2024_FIELDS = MODERN_2020_FIELDS + listOf(
        // Byte 114-129: Additional telemetry (16 bytes added)
        OutputField("boostPressure", 114, DataType.U16, units = "kPa"),
        OutputField("nitrousPW", 116, DataType.U08, units = "ms"),
        OutputField("nitrousStatus", 117, DataType.U08, units = "bits"),
        OutputField("n2o_addfuel", 118, DataType.U08, units = "%"),
        OutputField("n2o_retard", 119, DataType.U08, units = "deg"),
        OutputField("clutchEngaged", 120, DataType.U08),
        OutputField("clutchRPM", 121, DataType.U16),
        OutputField("fuelLoad", 123, DataType.U16),
        OutputField("ignLoad", 125, DataType.U16),
        OutputField("dwell", 127, DataType.U16, scale = 0.1, units = "ms"),
        OutputField("idleTargetRPM", 129, DataType.U08)
        // Total: 130 bytes (0-129)
    )

    // ========================================
    // FACTORY METHOD
    // ========================================

    /**
     * Get output channel definition based on block size
     *
     * @param blockSize Size of output channels block (35, 114, or 130)
     * @return List of OutputField definitions
     */
    /**
     * Get output channel definition based on block size
     *
     * EXPANDIDO: Suporta todas as versões de ochBlockSize identificadas
     * Baseado em análise de .ini files baixados (201609-202501)
     *
     * Mapeamento:
     * - 35 bytes:  Legacy (201609)
     * - 114 bytes: Modern 2020 (202008)
     * - 116 bytes: Modern 2020 (202012) → usa 114
     * - 122 bytes: Modern 2022 (202201-202207) → usa 114
     * - 125 bytes: Modern 2023 (202305-202310) → usa 114
     * - 127 bytes: Modern 2024 (202402) → usa 130
     * - 130 bytes: Modern 2025 (202501)
     *
     * Nota: Campos não mudam muito entre versões Modern, então
     * versões 114-125 podem usar MODERN_2020_FIELDS com segurança
     *
     * @param blockSize ochBlockSize from firmware
     * @return List of output channel field definitions
     */
    fun getDefinition(blockSize: Int): List<OutputField> {
        runtimeDefinitions[blockSize]?.let {
            Logger.d(TAG, "Using runtime output channels override ($blockSize bytes)")
            return it
        }
        return when {
            blockSize == 2068 -> {
                Logger.d(TAG, "Using rusEFI f407-discovery output channels ($blockSize bytes)")
                RUSEFI_2068_FIELDS
            }

            blockSize >= 2084 -> {
                Logger.d(TAG, "Using rusEFI output channels ($blockSize bytes)")
                RUSEFI_2084_FIELDS
            }

            blockSize >= 219 -> {
                Logger.d(TAG, "Using MegaSpeed output channels ($blockSize bytes)")
                MEGASPEED_219_FIELDS
            }

            blockSize >= 212 -> {
                Logger.d(TAG, "Using MS2 output channels ($blockSize bytes)")
                MS2_212_FIELDS
            }

            // Modern 2024-2025 (127-130 bytes)
            blockSize >= 127 -> {
                Logger.d(TAG, "Using MODERN_2024 output channels ($blockSize bytes)")
                MODERN_2024_FIELDS
            }

            // Modern 2020-2023 (114-125 bytes)
            blockSize >= 114 -> {
                Logger.d(TAG, "Using MODERN_2020 output channels ($blockSize bytes)")
                MODERN_2020_FIELDS
            }

            // Legacy (35 bytes)
            blockSize >= 35 -> {
                Logger.d(TAG, "Using LEGACY output channels (35 bytes)")
                LEGACY_FIELDS
            }

            // Unknown - fallback to legacy
            else -> {
                Logger.w(TAG, "Unknown block size: $blockSize - using LEGACY fields")
                LEGACY_FIELDS
            }
        }
    }

    /**
     * Catalog of all known output channels across legacy + modern definitions.
     *
     * This is useful for UI selection lists and settings screens.
     * Fields are de-duplicated by name, preferring the newest definition.
     */
    fun getCatalog(): List<OutputField> {
        val byName = LinkedHashMap<String, OutputField>()
        val definitions = runtimeDefinitions.values.toList() + listOf(RUSEFI_2084_FIELDS, RUSEFI_2068_FIELDS, MEGASPEED_219_FIELDS, MS2_212_FIELDS, MODERN_2024_FIELDS, MODERN_2020_FIELDS, LEGACY_FIELDS)
        definitions.forEach { fields ->
            fields.forEach { field ->
                if (!byName.containsKey(field.name)) {
                    byName[field.name] = field
                }
            }
        }
        return byName.values.sortedBy { it.name.lowercase() }
    }

    /**
     * Get specific field by name
     *
     * @param blockSize Block size
     * @param fieldName Field name (e.g., "rpm", "map", "tps")
     * @return OutputField or null if not found
     */
    fun getField(blockSize: Int, fieldName: String): OutputField? {
        return getDefinition(blockSize).find { it.name == fieldName }
    }

    fun registerRuntimeDefinition(blockSize: Int, fields: List<OutputField>) {
        runtimeDefinitions[blockSize] = fields
    }

    fun clearRuntimeDefinition(blockSize: Int) {
        runtimeDefinitions.remove(blockSize)
    }

    fun clearAllRuntimeDefinitions() {
        runtimeDefinitions.clear()
    }
}

/**
 * Output Channel Field Definition
 *
 * Defines a single field in the output channels block.
 *
 * @param name Field name (e.g., "rpm", "map", "tps")
 * @param offset Byte offset in the data block
 * @param type Data type (U08, U16, S08, S16)
 * @param scale Scale factor (raw value multiplied by this)
 * @param translate Translation offset (added after scaling)
 * @param units Unit string (e.g., "rpm", "kPa", "°C")
 */
data class OutputField(
    val name: String,
    val offset: Int,
    val type: DataType,
    val scale: Double = 1.0,
    val translate: Double = 0.0,
    val units: String = "",
    val byteOrder: EcuByteOrder = EcuByteOrder.LITTLE_ENDIAN,
) {
    /**
     * Parse this field from byte array
     *
     * @param data Byte array containing output channels data
     * @return Parsed and scaled value as Double
     */
    fun parse(data: ByteArray): Double {
        if (offset >= data.size) {
            Logger.w("OutputField", "Offset $offset out of bounds for field '$name' (data size: ${data.size})")
            return 0.0
        }

        val rawValue = when (type) {
            DataType.U08 -> {
                data[offset].toInt() and 0xFF
            }
            DataType.U16 -> {
                if (offset + 1 >= data.size) return 0.0
                val first = data[offset].toInt() and 0xFF
                val second = data[offset + 1].toInt() and 0xFF
                when (byteOrder) {
                    EcuByteOrder.LITTLE_ENDIAN -> first or (second shl 8)
                    EcuByteOrder.BIG_ENDIAN -> (first shl 8) or second
                }
            }
            DataType.S08 -> {
                data[offset].toInt()  // Signed byte
            }
            DataType.S16 -> {
                if (offset + 1 >= data.size) return 0.0
                val first = data[offset].toInt() and 0xFF
                val second = data[offset + 1].toInt() and 0xFF
                val unsigned = when (byteOrder) {
                    EcuByteOrder.LITTLE_ENDIAN -> first or (second shl 8)
                    EcuByteOrder.BIG_ENDIAN -> (first shl 8) or second
                }
                // Convert to signed
                if (unsigned >= 32768) unsigned - 65536 else unsigned
            }
        }

        // Apply scale and translate: userValue = (rawValue + translate) * scale
        return (rawValue + translate) * scale
    }

    /**
     * Parse this field as Int
     */
    fun parseInt(data: ByteArray): Int {
        return parse(data).toInt()
    }

    /**
     * Get formatted value with units
     */
    fun formatValue(data: ByteArray): String {
        val value = parse(data)
        return if (units.isNotEmpty()) {
            "%.1f %s".format(value, units)
        } else {
            "%.1f".format(value)
        }
    }
}
