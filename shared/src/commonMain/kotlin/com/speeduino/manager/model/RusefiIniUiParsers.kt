package com.speeduino.manager.model

import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.definition.IniFieldDefinition

object RusefiIniUiParsers {
    private val ENGINE_FIELDS = listOf(
        "injector_flow",
        "tpsMin",
        "tpsMax",
        "cranking_rpm",
        "displacement",
        "cylindersCount",
        "firingOrder",
        "injectionMode",
        "ignitionMode",
        "globalTriggerAngleOffset",
        "trigger_type",
    )

    private val TRIGGER_FIELDS = listOf(
        "trigger_type",
        "trigger_customTotalToothCount",
        "trigger_customSkippedToothCount",
        "globalTriggerAngleOffset",
        "triggerInputPins1",
        "triggerInputPins2",
        "useNoiselessTriggerDecoder",
        "invertSecondaryTriggerSignal",
        "skippedWheelOnCam",
        "vvtMode1",
        "vvtMode2",
    )

    private val IO_FIELDS = listOf(
        "triggerInputPins1",
        "triggerInputPins2",
        "mainRelayPin",
        "mainRelayPinMode",
        "tachOutputPin",
        "tachOutputPinMode",
        "fuelPumpPin",
        "fuelPumpPinMode",
        "malfunctionIndicatorPin",
        "malfunctionIndicatorPinMode",
        "alternatorControlPin",
        "alternatorControlPinMode",
        "fanPin",
        "fanPinMode",
        "idleSolenoidPin",
        "idleSolenoidPinMode",
        "boostPin",
        "boostPinMode",
        "o2heaterPin",
        "o2heaterPinMode",
        "vvtPins1",
        "vvtPins2",
    ) + (1..12).flatMap { index ->
        listOf("injectionPins$index", "ignitionPins$index")
    }

    fun requiredBytesForEngine(definition: IniDefinition): Int = requiredBytes(definition, ENGINE_FIELDS)

    fun requiredBytesForTrigger(definition: IniDefinition): Int = requiredBytes(definition, TRIGGER_FIELDS)

    fun requiredBytesForInputOutput(definition: IniDefinition): Int = requiredBytes(definition, IO_FIELDS)

    fun parseEngineConstants(definition: IniDefinition, data: ByteArray): EngineConstants {
        val reader = Reader(definition, data)

        val injectorFlow = reader.float("injector_flow") ?: 330f
        val tpsMin = reader.number("tpsMin") ?: 0.5
        val tpsMax = reader.number("tpsMax") ?: 4.5
        val crankingRpm = reader.int("cranking_rpm") ?: 350
        val displacement = reader.float("displacement") ?: 2.0f
        val cylinders = (reader.int("cylindersCount") ?: 4).coerceIn(1, 12)
        val firingOrder = reader.label("firingOrder") ?: "1-3-4-2"
        val injectionModeLabel = reader.label("injectionMode") ?: "Sequential"
        val ignitionModeLabel = reader.label("ignitionMode") ?: "Wasted Spark"
        val triggerAngle = reader.float("globalTriggerAngleOffset") ?: 0f
        val triggerType = reader.label("trigger_type") ?: "36-1"

        return EngineConstants(
            reqFuel = 0f,
            batteryVoltage = 12.0f,
            algorithm = Algorithm.SPEED_DENSITY,
            squirtsPerCycle = 1,
            injectorStaging = if (injectionModeLabel.equals("Simultaneous", true)) {
                InjectorStaging.SIMULTANEOUS
            } else {
                InjectorStaging.ALTERNATING
            },
            engineStroke = EngineStroke.FOUR_STROKE,
            numberOfCylinders = cylinders,
            injectorPortType = InjectorPortType.PORT,
            numberOfInjectors = cylinders,
            engineType = EngineType.EVEN_FIRE,
            boardLayout = "rusEFI",
            stoichiometricRatio = 14.7f,
            injectorLayout = when {
                injectionModeLabel.equals("Sequential", true) -> InjectorLayout.SEQUENTIAL
                injectionModeLabel.equals("Batch", true) -> InjectorLayout.BATCH
                else -> InjectorLayout.PAIRED
            },
            mapSampleMethod = MapSampleMethod.CYCLE_AVERAGE,
            mapSwitchPoint = triggerAngle.toInt(),
            engineDisplacementCc = (displacement * 1000f).toInt(),
            extraFields = linkedMapOf(
                "rusefi_engine_type" to "rusEFI",
                "rusefi_injector_flow" to formatDecimal(injectorFlow.toDouble(), 2),
                "rusefi_displacement_l" to formatDecimal(displacement.toDouble(), 3),
                "rusefi_firing_order" to firingOrder,
                "rusefi_injection_mode" to injectionModeLabel,
                "rusefi_ignition_mode" to ignitionModeLabel,
                "rusefi_trigger_angle" to formatDecimal(triggerAngle.toDouble(), 1),
                "rusefi_trigger_type" to triggerType,
                "rusefi_tps_min" to formatDecimal(tpsMin, 3),
                "rusefi_tps_max" to formatDecimal(tpsMax, 3),
                "rusefi_cranking_rpm" to crankingRpm.toString(),
            ),
        )
    }

    fun parseTriggerSettings(definition: IniDefinition, data: ByteArray): TriggerSettings {
        val reader = Reader(definition, data)
        val triggerTypeIndex = reader.int("trigger_type") ?: 9
        val customTotal = reader.int("trigger_customTotalToothCount") ?: 0
        val customMissing = reader.int("trigger_customSkippedToothCount") ?: 0
        val triggerAngle = reader.float("globalTriggerAngleOffset") ?: 0f
        val primaryInput = reader.int("triggerInputPins1") ?: 0
        val secondaryInput = reader.int("triggerInputPins2") ?: 0
        val noiseless = reader.bool("useNoiselessTriggerDecoder")
        val secondaryFalling = (reader.label("invertSecondaryTriggerSignal") ?: "Rising").equals("Falling", true)
        val skippedWheelLocation = reader.label("skippedWheelOnCam") ?: "On crankshaft"
        val vvtMode1 = reader.label("vvtMode1") ?: "Inactive"
        val vvtMode2 = reader.label("vvtMode2") ?: "Inactive"
        val totalTeeth = if (customTotal > 0) customTotal else inferTriggerTotalTeeth(triggerTypeIndex)
        val missingTeeth = if (customMissing > 0) customMissing else inferTriggerMissingTeeth(triggerTypeIndex)

        return TriggerSettings(
            triggerAngleDeg = triggerAngle.toInt(),
            triggerAngleMultiplier = 1,
            triggerPattern = triggerTypeIndex,
            primaryBaseTeeth = totalTeeth,
            missingTeeth = missingTeeth,
            primaryTriggerSpeed = if (skippedWheelLocation.contains("cam", true)) TriggerSettings.TriggerSpeed.CAM else TriggerSettings.TriggerSpeed.CRANK,
            triggerEdge = TriggerSettings.SignalEdge.RISING,
            secondaryTriggerEdge = if (secondaryFalling) TriggerSettings.SignalEdge.FALLING else TriggerSettings.SignalEdge.RISING,
            secondaryTriggerType = secondaryInput,
            levelForFirstPhaseHigh = skippedWheelLocation.contains("cam", true),
            skipRevolutions = 0,
            triggerFilter = if (noiseless) TriggerSettings.TriggerFilter.MEDIUM else TriggerSettings.TriggerFilter.OFF,
            reSyncEveryCycle = false,
            extraFields = linkedMapOf(
                "rusefi_trigger_type" to (reader.label("trigger_type") ?: "36-1"),
                "rusefi_trigger_primary_input" to triggerInputName(primaryInput),
                "rusefi_trigger_secondary_input" to triggerInputName(secondaryInput),
                "rusefi_trigger_skipped_wheel_location" to skippedWheelLocation,
                "rusefi_trigger_noise_filter" to if (noiseless) "Enabled" else "Disabled",
                "rusefi_trigger_custom_total" to customTotal.coerceAtLeast(0).toString(),
                "rusefi_trigger_custom_missing" to customMissing.coerceAtLeast(0).toString(),
                "rusefi_trigger_vvt_mode_1" to vvtMode1,
                "rusefi_trigger_vvt_mode_2" to vvtMode2,
            ),
        )
    }

    fun parseInputOutputSnapshot(definition: IniDefinition, data: ByteArray): RusefiInputOutputSnapshot {
        val reader = Reader(definition, data)

        val inputs = buildList {
            add(RusefiIoEntry("Primary Trigger Input", pinLabel(reader.int("triggerInputPins1") ?: 0, "Input")))
            add(RusefiIoEntry("Secondary Trigger Input", pinLabel(reader.int("triggerInputPins2") ?: 0, "Input")))
        }

        val fuelOutputs = buildList {
            for (index in 1..12) {
                val pin = reader.int("injectionPins$index") ?: continue
                add(RusefiIoEntry("Injection Output $index", pinLabel(pin, "Output")))
            }
        }

        val ignitionOutputs = buildList {
            for (index in 1..12) {
                val pin = reader.int("ignitionPins$index") ?: continue
                add(RusefiIoEntry("Ignition Output $index", pinLabel(pin, "Output")))
            }
        }

        val auxiliaryOutputs = buildList {
            addModeEntry(this, reader, "Fuel Pump", "fuelPumpPin", "fuelPumpPinMode")
            addModeEntry(this, reader, "MIL / Check Engine", "malfunctionIndicatorPin", "malfunctionIndicatorPinMode")
            addModeEntry(this, reader, "Alternator Control", "alternatorControlPin", "alternatorControlPinMode")
            addModeEntry(this, reader, "Main Relay", "mainRelayPin", "mainRelayPinMode")
            addModeEntry(this, reader, "Tach Output", "tachOutputPin", "tachOutputPinMode")
            addModeEntry(this, reader, "O2 Heater", "o2heaterPin", "o2heaterPinMode")
            addModeEntry(this, reader, "Idle Solenoid", "idleSolenoidPin", "idleSolenoidPinMode")
            addModeEntry(this, reader, "Boost Solenoid", "boostPin", "boostPinMode")
            addModeEntry(this, reader, "Fan Output", "fanPin", "fanPinMode")
            addPinEntry(this, reader, "VVT Solenoid 1", "vvtPins1")
            addPinEntry(this, reader, "VVT Solenoid 2", "vvtPins2")
        }

        return RusefiInputOutputSnapshot(
            inputs = inputs,
            fuelOutputs = fuelOutputs,
            ignitionOutputs = ignitionOutputs,
            auxiliaryOutputs = auxiliaryOutputs,
        )
    }

    private fun addModeEntry(target: MutableList<RusefiIoEntry>, reader: Reader, label: String, pinName: String, modeName: String) {
        val pin = reader.int(pinName) ?: return
        val modeLabel = reader.label(modeName) ?: "Push-pull"
        target.add(RusefiIoEntry(label, "${pinLabel(pin, "Output")} ($modeLabel)"))
    }

    private fun addPinEntry(target: MutableList<RusefiIoEntry>, reader: Reader, label: String, pinName: String) {
        val pin = reader.int(pinName) ?: return
        target.add(RusefiIoEntry(label, pinLabel(pin, "Output")))
    }

    private fun requiredBytes(definition: IniDefinition, names: List<String>): Int {
        var max = 0
        names.forEach { name ->
            val field = definition.fields.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return@forEach
            val size = fieldSize(field)
            val offset = field.offset ?: 0
            max = maxOf(max, offset + size)
        }
        return max.coerceAtLeast(64)
    }

    private fun fieldSize(field: IniFieldDefinition): Int {
        return when (field.dataType.trim().uppercase()) {
            "U08", "S08" -> 1
            "U16", "S16" -> 2
            "U32", "S32", "F32" -> 4
            else -> 1
        }
    }

    private fun pinLabel(pin: Int, prefix: String): String {
        return if (pin == 0) "Not assigned" else "$prefix $pin"
    }

    private fun triggerInputName(pin: Int): String = if (pin == 0) "Not assigned" else "Input $pin"

    private fun inferTriggerTotalTeeth(index: Int): Int = when (index) {
        8 -> 60
        9 -> 36
        48 -> 36
        69 -> 36
        73 -> 3
        else -> 0
    }

    private fun inferTriggerMissingTeeth(index: Int): Int = when (index) {
        8, 9, 48, 69 -> 1
        73 -> 0
        else -> 0
    }

    private fun formatDecimal(value: Double, decimals: Int): String {
        return "%.${decimals}f".format(value)
    }

    private class Reader(
        private val definition: IniDefinition,
        private val data: ByteArray,
    ) {
        fun int(name: String): Int? {
            val field = findField(name) ?: return null
            return readRaw(field)
        }

        fun float(name: String): Float? {
            val field = findField(name) ?: return null
            return readNumber(field)?.toFloat()
        }

        fun number(name: String): Double? {
            val field = findField(name) ?: return null
            return readNumber(field)
        }

        fun bool(name: String): Boolean {
            val field = findField(name) ?: return false
            val label = label(name)
            return label.equals("yes", true) || label.equals("enabled", true) || label.equals("on camshaft", true) || (readRaw(field) ?: 0) != 0
        }

        fun label(name: String): String? {
            val field = findField(name) ?: return null
            val raw = readRaw(field) ?: return null
            val options = enumOptions(field)
            return options.getOrNull(raw) ?: options.getOrNull(raw.coerceAtLeast(0)) ?: raw.toString()
        }

        private fun readNumber(field: IniFieldDefinition): Double? {
            val raw = readRaw(field) ?: return null
            return if (field.dataType.equals("F32", true)) {
                Float.fromBits(raw).toDouble()
            } else {
                (raw + (field.translate ?: 0.0)) * (field.scale ?: 1.0)
            }
        }

        private fun readRaw(field: IniFieldDefinition): Int? {
            val offset = field.offset ?: return null
            val raw = when (field.dataType.trim().uppercase()) {
                "U08" -> data.getOrNull(offset)?.toInt()?.and(0xFF)
                "S08" -> data.getOrNull(offset)?.toInt()
                "U16" -> if (offset + 1 < data.size) (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8) else null
                "S16" -> if (offset + 1 < data.size) {
                    val value = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                    if (value >= 0x8000) value - 0x10000 else value
                } else null
                "U32", "S32", "F32" -> if (offset + 3 < data.size) {
                    (data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8) or
                        ((data[offset + 2].toInt() and 0xFF) shl 16) or
                        ((data[offset + 3].toInt() and 0xFF) shl 24)
                } else null
                else -> null
            } ?: return null

            val bitRange = bitRange(field)
            return if (bitRange != null) {
                val width = (bitRange.second - bitRange.first + 1).coerceAtLeast(1)
                (raw shr bitRange.first) and ((1 shl width) - 1)
            } else {
                raw
            }
        }

        private fun findField(name: String): IniFieldDefinition? {
            return definition.fields.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

        private fun bitRange(field: IniFieldDefinition): Pair<Int, Int>? {
            val token = split(field.rawDefinition).getOrNull(3)?.trim() ?: return null
            val match = Regex("""\[(\d+):(\d+)]""").matchEntire(token) ?: return null
            return match.groupValues[1].toInt() to match.groupValues[2].toInt()
        }

        private fun enumOptions(field: IniFieldDefinition): List<String> {
            val tokens = split(field.rawDefinition)
            val start = if (field.rawDefinition.trimStart().startsWith("bits", true)) 4 else return emptyList()
            return tokens.drop(start).mapNotNull { token ->
                val cleaned = token.trim().removePrefix("\"").removeSuffix("\"")
                cleaned.takeUnless { it.isBlank() || it.startsWith("$") }
            }
        }

        private fun split(value: String): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var bracketDepth = 0
            var braceDepth = 0
            var parenDepth = 0
            value.forEach { char ->
                when (char) {
                    '"' -> {
                        inQuotes = !inQuotes
                        current.append(char)
                    }
                    '[' -> {
                        bracketDepth++
                        current.append(char)
                    }
                    ']' -> {
                        bracketDepth--
                        current.append(char)
                    }
                    '{' -> {
                        braceDepth++
                        current.append(char)
                    }
                    '}' -> {
                        braceDepth--
                        current.append(char)
                    }
                    '(' -> {
                        parenDepth++
                        current.append(char)
                    }
                    ')' -> {
                        parenDepth--
                        current.append(char)
                    }
                    ',' -> {
                        if (!inQuotes && bracketDepth == 0 && braceDepth == 0 && parenDepth == 0) {
                            parts += current.toString().trim()
                            current.clear()
                        } else {
                            current.append(char)
                        }
                    }
                    else -> current.append(char)
                }
            }
            if (current.isNotEmpty()) parts += current.toString().trim()
            return parts
        }
    }
}
