package com.speeduino.manager.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableValidatorTest {

    @Test
    fun nonMonotonicLoadBinsAreWarningsOnly() {
        val metadata = SpeeduinoTableDefinitions.VE_TABLE_MODERN
        val baseTable = VeTable.createDefault()
        val tableWithLoadIssues = baseTable.copy(
            loadBins = baseTable.loadBins.toMutableList().apply {
                this[12] = 200
                this[13] = 195
                this[14] = 250
                this[15] = 250
            }
        )

        val result = TableValidator(metadata).validate(tableWithLoadIssues)

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Load bins are not strictly increasing") })
        assertEquals(0, result.errors.size)
    }

    @Test
    fun nonMonotonicRpmBinsAreWarningsOnly() {
        val metadata = SpeeduinoTableDefinitions.VE_TABLE_MODERN
        val baseTable = VeTable.createDefault()
        val tableWithRpmIssues = baseTable.copy(
            rpmBins = baseTable.rpmBins.toMutableList().apply {
                this[5] = this[4]
                this[6] = this[4]
            }
        )

        val result = TableValidator(metadata).validate(tableWithRpmIssues)

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("RPM bins are not strictly increasing") })
        assertEquals(0, result.errors.size)
    }
}
