package com.speeduino.manager.model

import com.speeduino.manager.definition.IniParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpeeduinoIniDefinitionsTest {

    @Test
    fun buildsModernSpeeduinoCatalogFromIni() {
        val ini = """
            [MegaTune]
            signature = "speeduino 202501"

            [Constants]
            nPages = 15
            pageIdentifier = "${'$'}tsCanId\x01", "${'$'}tsCanId\x02", "${'$'}tsCanId\x03", "${'$'}tsCanId\x04", "${'$'}tsCanId\x05"
            pageSize = 128, 288, 288, 128, 288

            page = 2
            veTable = array, U08, 0, [16x16], "%", 1.0, 0.0, 0.0, 255.0, 0
            rpmBins = array, U08, 256, [16], "RPM", 100.0, 0.0, 100.0, 25500.0, 0
            fuelLoadBins = array, U08, 272, [16], "kPa", 2.0, 0.0, 0.0, 511.0, 0

            page = 3
            advTable1 = array, U08, 0, [16x16], "deg", 1.0, -40.0, -40.0, 70.0, 0
            rpmBins2 = array, U08, 256, [16], "RPM", 100.0, 0.0, 100.0, 25500.0, 0
            mapBins1 = array, U08, 272, [16], "kPa", 2.0, 0.0, 0.0, 511.0, 0

            page = 5
            lambdaTable = array, U08, 0, [16x16], "Lambda", 0.01, 0.0, 0.0, 2.0, 2
            afrTable = array, U08, lastOffset, [16x16], "AFR", 0.1, 0.0, 7.0, 25.5, 1
            rpmBinsAFR = array, U08, 256, [16], "RPM", 100.0, 0.0, 100.0, 25500.0, 0
            loadBinsAFR = array, U08, 272, [16], "kPa", 2.0, 0.0, 0.0, 511.0, 0

            [OutputChannels]
            ochBlockSize = 130
            secl = scalar, U08, 0, "sec", 1.0, 0.0
            engine = scalar, U08, 2, "bits", 1.0, 0.0
            map = scalar, U16, 4, "kPa", 1.0, 0.0
            coolantRaw = scalar, U08, 7, "C", 1.0, 0.0
            batteryVoltage = scalar, U08, 9, "V", 0.1, 0.0
            afr = scalar, U08, 10, "AFR", 0.1, 0.0
            rpm = scalar, U16, 14, "rpm", 1.0, 0.0
            advance = scalar, S08, 24, "deg", 1.0, 0.0
            tps = scalar, U08, 25, "%", 0.5, 0.0
            status2 = scalar, U08, 32, "bits", 1.0, 0.0
        """.trimIndent()

        val definition = IniParser.parse("speeduino.ini", ini)
        val catalog = SpeeduinoIniDefinitions.fromIni(definition)

        assertNotNull(catalog)
        assertEquals(130, catalog.tableDefinitions.ochBlockSize)
        assertEquals(2, catalog.tableDefinitions.veTable.page)
        assertEquals(3, catalog.tableDefinitions.ignitionTable.page)
        assertEquals(5, catalog.tableDefinitions.afrTable.page)
        assertEquals(288, catalog.tableDefinitions.afrTable.totalSize)
        assertTrue(catalog.outputFields.any { it.name == "rpm" && it.offset == 14 })
        assertTrue(catalog.outputFields.any { it.name == "spark" && it.offset == 32 })
    }

    @Test
    fun keepsLegacyVePageFromIni() {
        val ini = """
            [MegaTune]
            signature = "speeduino 201609"

            [Constants]
            nPages = 8
            pageIdentifier = "${'$'}tsCanId\x01", "${'$'}tsCanId\x03", "${'$'}tsCanId\x05"
            pageSize = 288, 288, 288

            page = 1
            veTable = array, U08, 0, [16x16], "%", 1.0, 0.0, 0.0, 255.0, 0

            page = 3
            advTable1 = array, U08, 0, [16x16], "deg", 1.0, 0.0, 0.0, 255.0, 0

            page = 5
            afrTable = array, U08, 0, [16x16], "AFR", 0.1, 0.0, 7.0, 25.5, 1

            [OutputChannels]
            ochBlockSize = 35
            rpm = scalar, U16, 13, "rpm", 1.0, 0.0
            spark = scalar, U08, 29, "bits", 1.0, 0.0
        """.trimIndent()

        val definition = IniParser.parse("speeduino_legacy.ini", ini)
        val catalog = SpeeduinoIniDefinitions.fromIni(definition)

        assertNotNull(catalog)
        assertEquals(1, catalog.tableDefinitions.veTable.page)
        assertEquals(FirmwareEra.LEGACY, catalog.tableDefinitions.era)
        assertEquals(35, catalog.tableDefinitions.ochBlockSize)
    }
}
