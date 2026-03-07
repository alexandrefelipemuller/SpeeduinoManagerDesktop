package com.speeduino.manager.model

enum class ProtectionCut {
    OFF,
    SPARK_ONLY,
    FUEL_ONLY,
    BOTH
}

enum class CutMethod {
    FULL,
    ROLLING
}

data class EngineProtectionConfig(
    val protectionCut: ProtectionCut,
    val cutMethod: CutMethod,
    val engineProtectionRpmMin: Int,
    val engineProtectEnabled: Boolean,
    val revLimiterEnabled: Boolean,
    val boostLimitEnabled: Boolean,
    val oilPressureProtectionEnabled: Boolean,
    val afrProtectionEnabled: Boolean,
    val coolantProtectionEnabled: Boolean,
)

object EngineProtectionMapper {
    const val PAGE_NUMBER = 6
    const val PAGE_SIZE = 192

    private const val ENGINE_PROTECT_TYPE_OFFSET = 0
    private const val BOOST_CUT_ENABLED_OFFSET = 6
    private const val HARD_CUT_TYPE_OFFSET = 26
    private const val ENGINE_PROTECT_MAX_RPM_OFFSET = 120
    private const val OIL_PRESSURE_FLAGS_OFFSET = 135
    private const val HARD_REV_MODE_OFFSET = 166
    private const val AFR_PROTECT_ENABLED_OFFSET = 185

    fun fromPage(page: ByteArray, era: FirmwareEra): EngineProtectionConfig {
        val engineProtectType = readBits(page, ENGINE_PROTECT_TYPE_OFFSET, 6, 2)
        val protectionCut = when (engineProtectType) {
            1 -> ProtectionCut.SPARK_ONLY
            2 -> ProtectionCut.FUEL_ONLY
            3 -> ProtectionCut.BOTH
            else -> ProtectionCut.OFF
        }
        val engineProtectEnabled = protectionCut != ProtectionCut.OFF

        val hardCutType = readBits(page, HARD_CUT_TYPE_OFFSET, 3, 1)
        val cutMethod = if (hardCutType == 1) CutMethod.ROLLING else CutMethod.FULL

        val rpmRaw = readU8(page, ENGINE_PROTECT_MAX_RPM_OFFSET)
        val rpmMin = rpmRaw * 100

        val boostLimitEnabled = readBits(page, BOOST_CUT_ENABLED_OFFSET, 7, 1) == 1

        val oilEnable = readBits(page, OIL_PRESSURE_FLAGS_OFFSET, 1, 1) == 1
        val oilProtect = readBits(page, OIL_PRESSURE_FLAGS_OFFSET, 2, 1) == 1
        val oilPressureProtectionEnabled = oilEnable && oilProtect

        val hardRevMode = if (page.size > HARD_REV_MODE_OFFSET && era.isModern()) {
            readBits(page, HARD_REV_MODE_OFFSET, 0, 2)
        } else {
            0
        }
        val revLimiterEnabled = hardRevMode != 0
        val coolantProtectionEnabled = hardRevMode == 2

        val afrProtectEnabled = if (page.size > AFR_PROTECT_ENABLED_OFFSET && era.isModern()) {
            readBits(page, AFR_PROTECT_ENABLED_OFFSET, 0, 2) > 0
        } else {
            false
        }

        return EngineProtectionConfig(
            protectionCut = protectionCut,
            cutMethod = cutMethod,
            engineProtectionRpmMin = rpmMin,
            engineProtectEnabled = engineProtectEnabled,
            revLimiterEnabled = revLimiterEnabled,
            boostLimitEnabled = boostLimitEnabled,
            oilPressureProtectionEnabled = oilPressureProtectionEnabled,
            afrProtectionEnabled = afrProtectEnabled,
            coolantProtectionEnabled = coolantProtectionEnabled,
        )
    }

    fun applyToPage(
        base: ByteArray,
        config: EngineProtectionConfig,
        era: FirmwareEra
    ): ByteArray {
        val page = base.copyOf()

        val protectTypeValue = if (!config.engineProtectEnabled) {
            0
        } else {
            when (config.protectionCut) {
                ProtectionCut.SPARK_ONLY -> 1
                ProtectionCut.FUEL_ONLY -> 2
                ProtectionCut.BOTH -> 3
                ProtectionCut.OFF -> 0
            }
        }
        writeBits(page, ENGINE_PROTECT_TYPE_OFFSET, 6, 2, protectTypeValue)

        val hardCutValue = if (config.cutMethod == CutMethod.ROLLING) 1 else 0
        writeBits(page, HARD_CUT_TYPE_OFFSET, 3, 1, hardCutValue)

        val rpmRaw = (config.engineProtectionRpmMin / 100).coerceIn(0, 255)
        writeU8(page, ENGINE_PROTECT_MAX_RPM_OFFSET, rpmRaw)

        writeBits(page, BOOST_CUT_ENABLED_OFFSET, 7, 1, if (config.boostLimitEnabled) 1 else 0)

        val oilEnabledValue = if (config.oilPressureProtectionEnabled) 1 else 0
        writeBits(page, OIL_PRESSURE_FLAGS_OFFSET, 1, 1, oilEnabledValue)
        writeBits(page, OIL_PRESSURE_FLAGS_OFFSET, 2, 1, oilEnabledValue)

        if (page.size > HARD_REV_MODE_OFFSET && era.isModern()) {
            val hardRevMode = when {
                config.coolantProtectionEnabled -> 2
                config.revLimiterEnabled -> 1
                else -> 0
            }
            writeBits(page, HARD_REV_MODE_OFFSET, 0, 2, hardRevMode)
        }

        if (page.size > AFR_PROTECT_ENABLED_OFFSET && era.isModern()) {
            val afrMode = if (config.afrProtectionEnabled) 1 else 0
            writeBits(page, AFR_PROTECT_ENABLED_OFFSET, 0, 2, afrMode)
        }

        return page
    }

    private fun readU8(page: ByteArray, offset: Int): Int {
        return page.getOrNull(offset)?.toInt()?.and(0xFF) ?: 0
    }

    private fun writeU8(page: ByteArray, offset: Int, value: Int) {
        if (offset >= page.size) return
        page[offset] = (value and 0xFF).toByte()
    }

    private fun readBits(page: ByteArray, offset: Int, shift: Int, width: Int): Int {
        if (offset >= page.size) return 0
        val mask = (1 shl width) - 1
        return (page[offset].toInt() shr shift) and mask
    }

    private fun writeBits(page: ByteArray, offset: Int, shift: Int, width: Int, value: Int) {
        if (offset >= page.size) return
        val mask = ((1 shl width) - 1) shl shift
        val current = page[offset].toInt() and 0xFF
        val updated = (current and mask.inv()) or ((value shl shift) and mask)
        page[offset] = (updated and 0xFF).toByte()
    }
}
