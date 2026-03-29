package com.speeduino.manager.model

/**
 * Simplified idle control settings for Speeduino page 4.
 *
 * This intentionally exposes only the most useful basic fields:
 * control mode and the small set of PWM/Stepper knobs that are commonly
 * needed to get idle moving.
 */
data class IdleControlSettings(
    val controlMode: IdleControlMode = IdleControlMode.NONE,
    val idleTargetRpm: Int = 900,
    val fastIdleTempC: Int = 40,
    val pwmChannels: Int = 1,
    val idleValveFrequencyHz: Int = 120,
    val pwmDirectionReverse: Boolean = false,
    val runBeforeStart: Boolean = false,
    val stepTimeMs: Int = 3,
    val stepHomeSteps: Int = 100,
    val stepMinimumSteps: Int = 3,
) {
    val isPwmMode: Boolean
        get() = controlMode == IdleControlMode.ON_OFF ||
            controlMode == IdleControlMode.PWM_OPEN_LOOP ||
            controlMode == IdleControlMode.PWM_CLOSED_LOOP ||
            controlMode == IdleControlMode.PWM_CLOSED_PLUS_OPEN_LOOP

    val isStepperMode: Boolean
        get() = controlMode == IdleControlMode.STEPPER_OPEN_LOOP ||
            controlMode == IdleControlMode.STEPPER_CLOSED_LOOP ||
            controlMode == IdleControlMode.STEPPER_CLOSED_PLUS_OPEN_LOOP

    companion object {
        const val PAGE_NUMBER: Int = 4
        const val PAGE_LENGTH: Int = 128
        const val TARGET_PAGE_NUMBER: Int = 7
        const val TARGET_PAGE_LENGTH: Int = 240

        private const val OFFSET_IDLE_ALGORITHM = 116
        private const val OFFSET_IDLE_FAST_TEMP = 117
        private const val OFFSET_IDLE_STEP_HOME = 118
        private const val OFFSET_IDLE_STEP_HYSTER = 119
        private const val OFFSET_IDLE_PWM_RUN = 60
        private const val OFFSET_IDLE_FREQ = 47
        private const val OFFSET_CL_IDLE_TARGET = 64
        private const val CL_IDLE_TARGET_COUNT = 10

        fun fromPage4(data: ByteArray): IdleControlSettings {
            require(data.size >= PAGE_LENGTH) { "Page 4 deve ter 128 bytes" }

            val algorithmByte = data[OFFSET_IDLE_ALGORITHM].toInt() and 0xFF
            val controlMode = IdleControlMode.fromRaw(algorithmByte and 0x07)
            val stepTime = ((algorithmByte shr 3) and 0x07).coerceIn(1, 6)
            val channels = if ((algorithmByte and 0x40) != 0) 2 else 1
            val reverse = (algorithmByte and 0x80) != 0

            val fastTempRaw = data[OFFSET_IDLE_FAST_TEMP].toInt() and 0xFF
            val stepHome = data[OFFSET_IDLE_STEP_HOME].toInt() and 0xFF
            val stepHyster = data[OFFSET_IDLE_STEP_HYSTER].toInt() and 0xFF
            val pwmRun = (data[OFFSET_IDLE_PWM_RUN].toInt() and 0x02) != 0
            val idleFreqRaw = data[OFFSET_IDLE_FREQ].toInt() and 0xFF

            return IdleControlSettings(
                controlMode = controlMode,
                idleTargetRpm = 900,
                fastIdleTempC = fastTempRaw - 40,
                pwmChannels = channels,
                idleValveFrequencyHz = idleFreqRaw * 2,
                pwmDirectionReverse = reverse,
                runBeforeStart = pwmRun,
                stepTimeMs = stepTime,
                stepHomeSteps = stepHome,
                stepMinimumSteps = stepHyster,
            )
        }

        fun readTargetRpmFromPage7(data: ByteArray): Int {
            require(data.size >= TARGET_PAGE_LENGTH) { "Page 7 deve ter 240 bytes" }

            val rawValues = (0 until CL_IDLE_TARGET_COUNT).map { index ->
                data[OFFSET_CL_IDLE_TARGET + index].toInt() and 0xFF
            }
            val averageRaw = rawValues.average().toInt()
            return (averageRaw * 10).coerceIn(0, 2550)
        }
    }

    fun applyToPage4(original: ByteArray): ByteArray {
        require(original.size >= PAGE_LENGTH) { "Page 4 deve ter 128 bytes" }

        val data = original.copyOf()
        var algorithmByte = data[OFFSET_IDLE_ALGORITHM].toInt() and 0xFF
        algorithmByte = (algorithmByte and 0xF8) or (controlMode.rawValue and 0x07)
        algorithmByte = (algorithmByte and 0xC7) or ((stepTimeMs.coerceIn(1, 6) and 0x07) shl 3)
        algorithmByte = if (pwmChannels >= 2) {
            algorithmByte or 0x40
        } else {
            algorithmByte and 0xBF
        }
        algorithmByte = if (pwmDirectionReverse) {
            algorithmByte or 0x80
        } else {
            algorithmByte and 0x7F
        }
        data[OFFSET_IDLE_ALGORITHM] = algorithmByte.toByte()

        data[OFFSET_IDLE_FAST_TEMP] = (fastIdleTempC + 40).coerceIn(0, 255).toByte()
        data[OFFSET_IDLE_FREQ] = (idleValveFrequencyHz.coerceAtLeast(0) / 2).coerceIn(0, 255).toByte()

        var pwmRunByte = data[OFFSET_IDLE_PWM_RUN].toInt() and 0xFF
        pwmRunByte = if (runBeforeStart) {
            pwmRunByte or 0x02
        } else {
            pwmRunByte and 0xFD
        }
        data[OFFSET_IDLE_PWM_RUN] = pwmRunByte.toByte()

        data[OFFSET_IDLE_STEP_HOME] = stepHomeSteps.coerceIn(0, 255).toByte()
        data[OFFSET_IDLE_STEP_HYSTER] = stepMinimumSteps.coerceIn(0, 255).toByte()

        return data
    }

    fun applyTargetRpmToPage7(original: ByteArray): ByteArray {
        require(original.size >= TARGET_PAGE_LENGTH) { "Page 7 deve ter 240 bytes" }

        val data = original.copyOf()
        val rawValue = (idleTargetRpm.coerceIn(0, 2550) / 10).toByte()
        for (index in 0 until CL_IDLE_TARGET_COUNT) {
            data[OFFSET_CL_IDLE_TARGET + index] = rawValue
        }
        return data
    }
}

enum class IdleControlMode(val rawValue: Int) {
    NONE(0),
    ON_OFF(1),
    PWM_OPEN_LOOP(2),
    PWM_CLOSED_LOOP(3),
    STEPPER_OPEN_LOOP(4),
    STEPPER_CLOSED_LOOP(5),
    PWM_CLOSED_PLUS_OPEN_LOOP(6),
    STEPPER_CLOSED_PLUS_OPEN_LOOP(7);

    companion object {
        fun fromRaw(value: Int): IdleControlMode {
            return values().firstOrNull { it.rawValue == value } ?: NONE
        }
    }
}
