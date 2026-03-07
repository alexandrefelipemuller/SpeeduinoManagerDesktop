package com.speeduino.manager.model

import com.speeduino.manager.definition.IniParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RusefiIniUiParsersTest {

    @Test
    fun parsesEngineTriggerAndIoFromIniFields() {
        val ini = """
            [MegaTune]
            signature = "rusEFI master.test"

            [Constants]
            page = 1
            injector_flow = scalar, F32, 76, "", 1, 0
            tpsMin = scalar, S16, 200, "V", 0.005, 0
            tpsMax = scalar, S16, 202, "V", 0.005, 0
            cranking_rpm = scalar, S16, 208, "RPM", 1, 0
            displacement = scalar, F32, 436, "L", 1, 0
            cylindersCount = scalar, U32, 444, "", 1, 0
            firingOrder = bits, U08, 448, [0:6], "One Cylinder", "1-3-4-2", "1-2-4-3"
            injectionMode = bits, U08, 459, [0:1], "Simultaneous", "Sequential", "Batch", "Single Point"
            ignitionMode = bits, U08, 476, [0:1], "Single Coil", "Individual Coils", "Wasted Spark", "Two Distributors"
            globalTriggerAngleOffset = scalar, F32, 488, "deg btdc", 1, 0
            trigger_type = bits, U32, 552, [0:6], "custom toothed wheel", "Ford Aspire", "Dodge Neon 1995", "Miata NA", "INVALID", "GM_7X", "Daihatsu 3 cylinder", "Mazda SOHC 4", "60-2", "36-1"
            trigger_customTotalToothCount = scalar, S32, 556, "number", 1, 0
            trigger_customSkippedToothCount = scalar, S32, 560, "number", 1, 0
            injectionPins1 = bits, U16, 656, [0:8], ${'$'}output_pin_e_list
            ignitionPins1 = bits, U16, 680, [0:8], ${'$'}output_pin_e_list
            fuelPumpPin = bits, U16, 706, [0:8], ${'$'}output_pin_e_list
            fuelPumpPinMode = bits, U08, 708, [0:1], "Push-pull", "Open drain", "Inverted"
            triggerInputPins1 = bits, U16, 748, [0:8], ${'$'}brain_input_pin_e_list
            triggerInputPins2 = bits, U16, 750, [0:8], ${'$'}brain_input_pin_e_list
            useNoiselessTriggerDecoder = bits, U32, 776, [17:17], "no", "yes"
            mainRelayPinMode = bits, U08, 788, [0:1], "Push-pull", "Open drain", "Inverted"
            mainRelayPin = bits, U16, 56, [0:8], ${'$'}output_pin_e_list
            tachOutputPin = bits, U16, 68, [0:8], ${'$'}output_pin_e_list
            tachOutputPinMode = bits, U08, 70, [0:1], "Push-pull", "Open drain", "Inverted"
            fanPin = bits, U16, 500, [0:8], ${'$'}output_pin_e_list
            fanPinMode = bits, U08, 502, [0:1], "Push-pull", "Open drain", "Inverted"
            invertSecondaryTriggerSignal = bits, U32, 1356, [14:14], "Rising", "Falling"
            skippedWheelOnCam = bits, U32, 1356, [25:25], "On crankshaft", "On camshaft"
            vvtMode1 = bits, U08, 1684, [0:5], "Inactive", "Single Tooth"
            vvtMode2 = bits, U08, 1685, [0:5], "Inactive", "Single Tooth"
        """.trimIndent()

        val definition = IniParser.parse("rusefi.ini", ini)
        val data = ByteArray(1686)
        writeF32Le(data, 76, 330f)
        writeS16Le(data, 200, 100)
        writeS16Le(data, 202, 900)
        writeS16Le(data, 208, 350)
        writeF32Le(data, 436, 2.0f)
        writeU32Le(data, 444, 4)
        data[448] = 1
        data[459] = 1
        data[476] = 2
        writeF32Le(data, 488, 12f)
        writeU32Le(data, 552, 9)
        writeU32Le(data, 556, 36)
        writeU32Le(data, 560, 1)
        writeU16Le(data, 656, 10)
        writeU16Le(data, 680, 20)
        writeU16Le(data, 706, 30)
        data[708] = 1
        writeU16Le(data, 748, 40)
        writeU16Le(data, 750, 41)
        writeU32Le(data, 776, 1 shl 17)
        writeU16Le(data, 56, 31)
        data[788] = 2
        writeU16Le(data, 68, 32)
        data[70] = 1
        writeU16Le(data, 500, 33)
        data[502] = 0
        writeU32Le(data, 1356, (1 shl 14) or (1 shl 25))
        data[1684] = 1
        data[1685] = 0

        val engine = RusefiIniUiParsers.parseEngineConstants(definition, data)
        val trigger = RusefiIniUiParsers.parseTriggerSettings(definition, data)
        val io = RusefiIniUiParsers.parseInputOutputSnapshot(definition, data)

        assertEquals(4, engine.numberOfCylinders)
        assertEquals("1-3-4-2", engine.extraFields["rusefi_firing_order"])
        assertEquals("Sequential", engine.extraFields["rusefi_injection_mode"])
        assertEquals("Wasted Spark", engine.extraFields["rusefi_ignition_mode"])
        assertEquals("36-1", engine.extraFields["rusefi_trigger_type"])

        assertEquals(36, trigger.primaryBaseTeeth)
        assertEquals(1, trigger.missingTeeth)
        assertEquals("Input 40", trigger.extraFields["rusefi_trigger_primary_input"])
        assertEquals("On camshaft", trigger.extraFields["rusefi_trigger_skipped_wheel_location"])
        assertEquals("Enabled", trigger.extraFields["rusefi_trigger_noise_filter"])

        assertTrue(io.fuelOutputs.any { it.label == "Injection Output 1" && it.value.contains("10") })
        assertTrue(io.ignitionOutputs.any { it.label == "Ignition Output 1" && it.value.contains("20") })
        assertTrue(io.auxiliaryOutputs.any { it.label == "Fuel Pump" && it.value.contains("Open drain") })
    }

    private fun writeU16Le(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeS16Le(target: ByteArray, offset: Int, value: Int) {
        writeU16Le(target, offset, value and 0xFFFF)
    }

    private fun writeU32Le(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value shr 8) and 0xFF).toByte()
        target[offset + 2] = ((value shr 16) and 0xFF).toByte()
        target[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeF32Le(target: ByteArray, offset: Int, value: Float) {
        writeU32Le(target, offset, value.toBits())
    }
}
