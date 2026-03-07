package com.speeduino.manager.model

import com.speeduino.manager.definition.IniParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RusefiIniTableDefinitionsTest {

    @Test
    fun buildsCatalogFromRusefiIni() {
        val ini = """
            [MegaTune]
            signature = "rusEFI master.2026.03.02.proteus_f7.123456"

            [Constants]
            nPages = 3
            pageIdentifier = "\x00\x00", "\x00\x01", "\x00\x02"
            pageSize = 64020, 256, 2048
            ochBlockSize = 2084
            page = 1
            ignitionTable = array, S16, 56296, [16x16], "deg", 0.1, 0, -20, 90, 1
            ignLoadBins = array, U16, 56808, [16], "kPa", 1, 0, 0, 1000, 0
            ignRpmBins = array, U16, 56840, [16], "RPM", 1, 0, 0, 18000, 0
            veTable = array, U16, 56872, [16x16], "%", 0.1, 0, 0, 999, 1
            veLoadBins = array, U16, 57384, [16], "kPa", 1, 0, 0, 1000, 0
            veRpmBins = array, U16, 57416, [16], "RPM", 1, 0, 0, 18000, 0
            lambdaTable = array, U08, 57448, [16x16], "AFR", 0.1, 0, 0, 25, 1
            lambdaLoadBins = array, U16, 57704, [16], "kPa", 1, 0, 0, 1000, 0
            lambdaRpmBins = array, U16, 57736, [16], "RPM", 1, 0, 0, 18000, 0

            [OutputChannels]
            RPMValue = scalar, U16, 4, "RPM", 1, 0
            coolant = scalar, S16, 16, "deg C", 0.01, 0
            intake = scalar, S16, 18, "deg C", 0.01, 0
            TPSValue = scalar, S16, 24, "%", 0.01, 0
            MAPValue = scalar, U16, 34, "kPa", 0.03333333333333333, 0
            VBatt = scalar, U16, 40, "V", 0.001, 0
            AFRValue = scalar, U16, 254, "AFR", 0.001, 0
            ignitionAdvanceCyl1 = scalar, S16, 288, "deg", 0.02, 0
        """.trimIndent()

        val definition = IniParser.parse("rusefi.ini", ini)
        val catalog = RusefiIniTableDefinitions.fromIni(definition)

        assertNotNull(catalog)
        assertEquals(2084, catalog.tableDefinitions.ochBlockSize)
        assertEquals(0x0000, catalog.veTable.metadata.page)
        assertEquals(0x0000, catalog.ignitionTable.metadata.page)
        assertEquals(0x0000, catalog.afrTable.metadata.page)
        assertEquals(16 to 16, catalog.veTable.metadata.valuesShape)
        assertTrue(catalog.outputFields.any { it.name == "rpm" && it.offset == 4 })
        assertTrue(catalog.outputFields.any { it.name == "map" && it.offset == 34 })
        assertTrue(catalog.outputFields.any { it.name == "batteryVoltage" && it.offset == 40 })
        assertTrue(catalog.outputFields.any { it.name == "advance" && it.offset == 288 })
    }
}
