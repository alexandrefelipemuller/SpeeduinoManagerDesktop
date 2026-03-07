package com.speeduino.manager.model

import com.speeduino.manager.definition.IniParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MegaSpeedIniTableDefinitionsTest {

    @Test
    fun buildsCatalogFromMegaSpeedIni() {
        val ini = """
            [MegaTune]
            queryCommand = "Q"
            signature = "MS2Extra MegaSpeed "

            [Constants]
            endianness = big
            nPages = 7
            pageSize = 1024,1024,1024,1024,1024,1024,1024
            pageIdentifier = "${'$'}tsCanId\x04","${'$'}tsCanId\x05","${'$'}tsCanId\x0a","${'$'}tsCanId\x08","${'$'}tsCanId\x09","${'$'}tsCanId\x0b","${'$'}tsCanId\x0c"
            ochBlockSize = 219

            page = 1
            afrTable1 = array, U08, 48, [12x12], "AFR", 0.1, 0, 1, 25, 1
            arpm_table1 = array, U16, 374, [12], "RPM", 1, 0, 0, 15000, 0
            amap_table1 = array, S16, 422, [12], "kPa", 0.1, 0, 0, 700, 1

            page = 3
            advanceTable1 = array, S16, 0, [12x12], "grau", 0.1, 0, -10, 90, 1
            srpm_table1 = array, U16, 576, [12], "RPM", 1, 0, 0, 15000, 0
            smap_table1 = array, S16, 624, [12], "kPa", 0.1, 0, 0, 700, 1

            page = 5
            veTable1 = array, U08, 0, [16x16], "%", 1, 0, 0, 255, 0
            ve1RpmBins = array, U16, 768, [16], "RPM", 1, 0, 0, 15000, 0
            ve1LoadBins = array, S16, 864, [16], "kPa", 0.1, 0, 0, 700, 1
        """.trimIndent()

        val definition = IniParser.parse("megaspeed.ini", ini)
        val catalog = MegaSpeedIniTableDefinitions.fromIni(definition)

        assertNotNull(catalog)
        assertEquals(219, catalog.tableDefinitions.ochBlockSize)
        assertEquals(0x09, catalog.veTable.metadata.page)
        assertEquals(0x0A, catalog.ignitionTable.metadata.page)
        assertEquals(0x04, catalog.afrTable.metadata.page)
        assertEquals(16 to 16, catalog.veTable.metadata.valuesShape)
        assertEquals(12 to 12, catalog.ignitionTable.metadata.valuesShape)
        assertEquals(12 to 12, catalog.afrTable.metadata.valuesShape)
    }
}
