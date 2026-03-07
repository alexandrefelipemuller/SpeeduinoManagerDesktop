package com.speeduino.manager.definition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IniParserTest {

    @Test
    fun parsesRusefiCoreMetadata() {
        val ini = """
            [MegaTune]
            signature = "rusEFI master.2025.10.18.f407-discovery.647208736"

            [TunerStudio]
            queryCommand = "S"
            versionInfo = "V"

            [Constants]
            nPages = 3
            pageIdentifier = "\x00\x00", "\x00\x01", "\x00\x02"
            pageSize = 22852, 256, 2048
            ochBlockSize = 2068
            page = 1
            ignitionTable = array, S16, 15136, [16x16], "deg", 0.1, 0, -20, 90, 1
            veTable = array, U16, 15712, [16x16], "%", 0.1, 0, 0, 999, 1

            [OutputChannels]
            RPMValue = scalar, U16, 4, "rpm", 1, 0
            coolant = scalar, S16, 14, "C", 0.01, 0
            lambdaTable = array, U08, 16288, [16x16], "afr", 0.1, 0, 0, 25, 1
        """.trimIndent()

        val parsed = IniParser.parse("rusefi.ini", ini)

        assertEquals("rusEFI master.2025.10.18.f407-discovery.647208736", parsed.signature)
        assertEquals("rusefi", parsed.family)
        assertEquals("S", parsed.queryCommand)
        assertEquals("V", parsed.versionInfoCommand)
        assertEquals(3, parsed.nPages)
        assertEquals(2068, parsed.ochBlockSize)
        assertEquals(3, parsed.pageDefinitions.size)
        assertEquals(0x0100, parsed.pageDefinitions[1].resolvedId)
        assertTrue(parsed.tableDefinitions.any { it.name == "ignitionTable" && it.offset == 15136 })
        assertTrue(parsed.tableDefinitions.any { it.name == "veTable" && it.offset == 15712 })
        assertTrue(parsed.outputChannels.any { it.name == "RPMValue" && it.offset == 4 })
        assertTrue(parsed.outputChannels.any { it.name == "lambdaTable" })
    }

    @Test
    fun parsesSpeeduinoPageMetadata() {
        val ini = """
            [MegaTune]
            signature = "speeduino 202501"

            [Constants]
            nPages = 2
            pageIdentifier = "${'$'}tsCanId\x01", "${'$'}tsCanId\x02"
            pageSize = 128, 288
            ochBlockSize = 130
            page = 1
            veTable = array, U08, 0, [16x16], "%", 1.0, 0.0, 0.0, 255.0, 0

            [OutputChannels]
            rpm = scalar, U16, 14, "rpm", 1, 0
            batteryVoltage = scalar, U08, 9, "V", 0.1, 0
        """.trimIndent()

        val parsed = IniParser.parse("speeduino.ini", ini)

        assertEquals("speeduino 202501", parsed.signature)
        assertEquals("speeduino", parsed.family)
        assertEquals(130, parsed.ochBlockSize)
        assertEquals(2, parsed.pageDefinitions.size)
        assertEquals(1, parsed.pageDefinitions[0].resolvedId)
        val veTable = parsed.tableDefinitions.firstOrNull { it.name == "veTable" }
        assertNotNull(veTable)
        assertEquals(16, veTable.shape.rows)
        assertEquals(16, veTable.shape.columns)
        assertTrue(parsed.outputChannels.any { it.name == "rpm" && it.offset == 14 })
    }
}
