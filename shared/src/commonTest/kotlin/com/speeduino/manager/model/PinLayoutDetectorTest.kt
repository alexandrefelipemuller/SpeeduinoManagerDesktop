package com.speeduino.manager.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PinLayoutDetectorTest {

    @Test
    fun classifiesKnownStm32LayoutsWithoutLiteralStm32Token() {
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(41).mcuFamily) // UA4C
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(42).mcuFamily) // BlitzboxBL49sp
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(45).mcuFamily) // DIY-EFI CORE4 v1.0
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(53).mcuFamily) // JUICEBOX
        assertEquals(McuFamily.STM32, PinLayoutDetector.fromIndex(55).mcuFamily) // Drop Bear
    }

    @Test
    fun keepsKnownAvrAndTeensyClassification() {
        assertEquals(McuFamily.AVR, PinLayoutDetector.fromIndex(1).mcuFamily) // Speeduino v0.2
        assertEquals(McuFamily.TEENSY, PinLayoutDetector.fromIndex(50).mcuFamily) // dvjcodec Teensy RevA
    }

    @Test
    fun invalidIndexesRemainUnknown() {
        val info = PinLayoutDetector.fromIndex(0)
        assertEquals(McuFamily.UNKNOWN, info.mcuFamily)
        assertNull(info.name)
    }
}
