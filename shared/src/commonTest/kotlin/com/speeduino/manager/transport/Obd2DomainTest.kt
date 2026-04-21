package com.speeduino.manager.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Obd2DomainTest {
    @Test
    fun classifiesBrandHintsFromVinAndCalibration() {
        assertEquals(VehicleBrandHint.PSA, classifyVehicleBrandHint("VF7ABC1234567890", null))
        assertEquals(VehicleBrandHint.RENAULT, classifyVehicleBrandHint(null, "Renault Sirius"))
        assertEquals(VehicleBrandHint.VAG, classifyVehicleBrandHint("WVWZZZ1JZXW000001", null))
        assertEquals(VehicleBrandHint.UNKNOWN, classifyVehicleBrandHint("1HGCM82633A000000", "Generic"))
    }

    @Test
    fun resolvesProfileKeyUsingVinFirst() {
        assertEquals(
            "vin:VF7ABC1234567890|cal:PSA-ECU",
            resolveObd2ProfileKey(
                connectionInfo = "ignored",
                vin = "VF7ABC1234567890",
                calibrationId = "PSA-ECU",
            )
        )
        assertEquals(
            "cal:CAL123|conn:elm327",
            resolveObd2ProfileKey(
                connectionInfo = "elm327",
                vin = null,
                calibrationId = "CAL123",
            )
        )
    }

    @Test
    fun convertsOptimizationFeaturesToMaskAndBack() {
        val features = setOf(
            Obd2OptimizationFeature.FIXED_PROTOCOL,
            Obd2OptimizationFeature.FAST_TIMEOUT,
            Obd2OptimizationFeature.FAST_POLL_INTERVAL,
        )

        val mask = features.toMask()
        assertTrue(mask and (1 shl Obd2OptimizationFeature.FIXED_PROTOCOL.bitIndex) != 0)
        assertTrue(mask and (1 shl Obd2OptimizationFeature.FAST_TIMEOUT.bitIndex) != 0)
        assertTrue(mask and (1 shl Obd2OptimizationFeature.FAST_POLL_INTERVAL.bitIndex) != 0)
        assertEquals(features, featuresFromMask(mask))
    }

    @Test
    fun computesOptimizationProgressFraction() {
        val status = Obd2OptimizationStatus(
            appliedMask = 0,
            testedCount = 2,
            totalCount = 4,
            currentFeature = null,
            message = "Testing",
            testing = true,
        )

        assertEquals(0.5f, status.progressFraction)
        assertFalse(status.fallbackOccurred)
    }
}
