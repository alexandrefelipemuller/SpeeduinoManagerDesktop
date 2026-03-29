package com.speeduino.manager.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClosedLoopCorrectionMapperTest {

    @Test
    fun parsesModernPage6IntoClosedLoopConfig() {
        val page = ByteArray(ClosedLoopCorrectionMapper.PAGE_SIZE)
        page[0] = 0b00001010
        page[1] = 15
        page[2] = 9
        page[3] = 4
        page[4] = 60
        page[5] = 7
        page[7] = 12
        page[8] = 140.toByte()
        page[9] = 158.toByte()
        page[10] = 8
        page[11] = 35
        page[12] = 24
        page[112] = 60
        page[113] = 18

        val parsed = ClosedLoopCorrectionMapper.fromPage(page, FirmwareEra.MODERN_2025)

        assertEquals(ClosedLoopSensorType.WIDE_BAND, parsed.sensorType)
        assertEquals(ClosedLoopStrategy.PID, parsed.strategy)
        assertEquals(7, parsed.ignitionEventsPerStep)
        assertEquals(12, parsed.authorityPercent)
        assertEquals(14.0, parsed.minAfr)
        assertEquals(15.8, parsed.maxAfr)
        assertEquals(0.952, parsed.minLambda, 0.001)
        assertEquals(1.075, parsed.maxLambda, 0.001)
        assertEquals(68, parsed.activeAboveCoolantF)
        assertEquals(3500, parsed.activeAboveRpm)
        assertEquals(12.0, parsed.activeBelowTpsPercent)
        assertEquals(120, parsed.activeBelowMapKpa)
        assertEquals(36, parsed.activeAboveMapKpa)
        assertEquals(8, parsed.delayAfterStartSeconds)
        assertEquals(15, parsed.pidProportional)
    }

    @Test
    fun applyToPageWritesOnlyClosedLoopOffsets() {
        val page = ByteArray(ClosedLoopCorrectionMapper.PAGE_SIZE) { 0x55.toByte() }
        val config = ClosedLoopCorrectionConfig(
            sensorType = ClosedLoopSensorType.WIDE_BAND,
            strategy = ClosedLoopStrategy.SIMPLE,
            ignitionEventsPerStep = 6,
            authorityPercent = 10,
            minAfr = 13.2,
            maxAfr = 15.4,
            minLambda = 0.0,
            maxLambda = 0.0,
            activeAboveCoolantF = 140,
            activeAboveRpm = 2500,
            activeBelowTpsPercent = 8.5,
            activeBelowMapKpa = 100,
            activeAboveMapKpa = 40,
            delayAfterStartSeconds = 12,
            pidProportional = 99,
            pidIntegral = 77,
            pidDerivative = 55,
        )

        val updated = ClosedLoopCorrectionMapper.applyToPage(page, config, FirmwareEra.MODERN_2024)

        assertEquals(0x55.toByte(), updated[40])
        assertEquals(0x55.toByte(), updated[80])
        assertEquals(0b00001000, updated[0].toInt() and 0x0F)
        assertEquals(6, updated[5].toInt() and 0xFF)
        assertEquals(10, updated[7].toInt() and 0xFF)
        assertEquals(132, updated[8].toInt() and 0xFF)
        assertEquals(154, updated[9].toInt() and 0xFF)
        assertEquals(12, updated[10].toInt() and 0xFF)
        assertEquals(25, updated[11].toInt() and 0xFF)
        assertEquals(17, updated[12].toInt() and 0xFF)
        assertEquals(50, updated[112].toInt() and 0xFF)
        assertEquals(20, updated[113].toInt() and 0xFF)
        assertEquals(0, updated[1].toInt() and 0xFF)
        assertEquals(0, updated[2].toInt() and 0xFF)
        assertEquals(0, updated[3].toInt() and 0xFF)
    }

    @Test
    fun syncFromLambdaKeepsAfrAndLambdaConsistent() {
        val synced = ClosedLoopCorrectionMapper.syncFromLambda(
            ClosedLoopCorrectionConfig(
                minAfr = 0.0,
                maxAfr = 0.0,
                minLambda = 0.90,
                maxLambda = 1.02,
            )
        )

        assertEquals(13.23, synced.minAfr, 0.01)
        assertEquals(14.99, synced.maxAfr, 0.01)
        assertEquals(0.90, synced.minLambda, 0.001)
        assertEquals(1.02, synced.maxLambda, 0.001)
    }

    @Test
    fun rejectsLegacyEra() {
        assertFailsWith<IllegalArgumentException> {
            ClosedLoopCorrectionMapper.fromPage(ByteArray(192), FirmwareEra.LEGACY)
        }
    }

    @Test
    fun sanitizeDisablesPidGainsOutsidePidStrategy() {
        val sanitized = ClosedLoopCorrectionMapper.sanitize(
            ClosedLoopCorrectionConfig(
                strategy = ClosedLoopStrategy.SIMPLE,
                pidProportional = 100,
                pidIntegral = 80,
                pidDerivative = 60,
                minAfr = 16.5,
                maxAfr = 14.0,
            )
        )

        assertEquals(0, sanitized.pidProportional)
        assertEquals(0, sanitized.pidIntegral)
        assertEquals(0, sanitized.pidDerivative)
        assertTrue(sanitized.minAfr <= sanitized.maxAfr)
    }
}
