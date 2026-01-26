package com.speeduino.manager.model

/**
 * Sanitizer/validator for Page 6 (O2/AFR and related settings).
 *
 * This is a defensive fixer used when the ECU rejects a raw restore
 * with RANGE_ERR. It clamps common ranges and enforces monotonic bins.
 */
object Page6Validator {
    data class Result(
        val data: ByteArray,
        val changed: Boolean
    )

    fun sanitize(data: ByteArray): Result {
        val result = data.copyOf()
        var changed = false

        fun getU8(offset: Int): Int {
            if (offset !in result.indices) return 0
            return result[offset].toInt() and 0xFF
        }

        fun setU8(offset: Int, value: Int) {
            if (offset !in result.indices) return
            val clamped = value.coerceIn(0, 255)
            val current = result[offset].toInt() and 0xFF
            if (current != clamped) {
                result[offset] = clamped.toByte()
                changed = true
            }
        }

        fun clampU8(offset: Int, min: Int, max: Int) {
            if (offset !in result.indices) return
            val current = result[offset].toInt() and 0xFF
            val clamped = current.coerceIn(min, max)
            if (current != clamped) {
                result[offset] = clamped.toByte()
                changed = true
            }
        }

        fun clampS8(offset: Int, min: Int, max: Int) {
            if (offset !in result.indices) return
            val raw = result[offset].toInt()
            val signed = if (raw > 127) raw - 256 else raw
            val clamped = signed.coerceIn(min, max)
            if (signed != clamped) {
                result[offset] = clamped.toByte()
                changed = true
            }
        }

        fun clampArrayU8(offset: Int, length: Int, min: Int, max: Int, monotonic: Boolean) {
            var previous = min
            for (i in 0 until length) {
                val index = offset + i
                if (index !in result.indices) {
                    break
                }
                var value = result[index].toInt() and 0xFF
                value = value.coerceIn(min, max)
                if (monotonic && value < previous) {
                    value = previous
                }
                val current = result[index].toInt() and 0xFF
                if (current != value) {
                    result[index] = value.toByte()
                    changed = true
                }
                if (monotonic) {
                    previous = value
                }
            }
        }

        // Ego limits and AFR min/max (scale 0.1 -> raw 70..250)
        clampU8(7, 0, 16)
        clampU8(8, 70, 250)
        clampU8(9, 70, 250)
        val egoMin = getU8(8)
        val egoMax = getU8(9)
        if (egoMin > egoMax) {
            setU8(8, egoMax)
            setU8(9, egoMin)
        }

        // Ego delay / RPM / TPS
        clampU8(10, 0, 120)
        clampU8(11, 1, 255)
        clampU8(12, 0, 200)

        // Voltage bins (0.1V) and air density bins
        clampArrayU8(15, 6, 60, 240, monotonic = true)
        clampArrayU8(27, 9, 0, 255, monotonic = true)

        // PWM frequencies (scale 2.0 -> min 10 => raw 5)
        clampU8(45, 5, 255)
        clampU8(46, 5, 255)
        clampU8(47, 5, 255)

        // Launch control and boost limits
        clampU8(49, 1, 255)
        clampS8(50, -30, 40)
        clampU8(51, 1, 255)
        clampU8(52, 0, 80)
        clampU8(56, 0, 255)
        clampU8(57, 0, 200)
        clampU8(58, 0, 200)
        clampU8(59, 0, 200)

        // Flat shift
        clampU8(61, 1, 255)
        clampS8(62, -30, 80)
        clampU8(63, 1, 255)

        // Idle control arrays and bins
        clampArrayU8(84, 10, 0, 100, monotonic = false)
        clampArrayU8(94, 10, 0, 255, monotonic = true)
        clampArrayU8(108, 4, 0, 100, monotonic = false)
        clampArrayU8(112, 4, 0, 255, monotonic = true)
        clampU8(118, 0, 255)
        clampU8(119, 1, 10)

        return Result(result, changed)
    }
}
