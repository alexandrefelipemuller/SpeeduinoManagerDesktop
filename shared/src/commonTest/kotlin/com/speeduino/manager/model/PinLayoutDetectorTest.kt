package com.speeduino.manager.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PinLayoutDetectorTest {

    @Test
    fun classifiesKnownStm32LayoutsWithoutLiteralStm32Token() {
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(41).mcuFamily)
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(42).mcuFamily)
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(45).mcuFamily)
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(53).mcuFamily)
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(55).mcuFamily)
    }

    @Test
    fun keepsKnownAvrAndTeensyClassification() {
        assertEquals(McuFamily.AVR, PinLayoutDetector.fromIndex(1).mcuFamily)
        assertEquals(McuFamily.TEENSY, PinLayoutDetector.fromIndex(50).mcuFamily)
    }

    @Test
    fun invalidIndexesRemainUnknown() {
        val info = PinLayoutDetector.fromIndex(0)
        assertEquals(McuFamily.UNKNOWN, info.mcuFamily)
        assertNull(info.name)
    }
}
