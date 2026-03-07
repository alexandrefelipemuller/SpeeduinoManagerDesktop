package com.speeduino.manager.model

/**
 * Pin layout detector based on Page 1 (offset 15).
 *
 * Mapping derived from reference/speeduino_202402.ini (pinLayouts define).
 * This is used for analytics and light heuristics only.
 */
enum class McuFamily(val analyticsValue: String) {
    AVR("atmega"),
    TEENSY("teensy"),
    STM32("stm32"),
    UNKNOWN("unknown")
}

data class PinLayoutInfo(
    val index: Int,
    val name: String?,
    val mcuFamily: McuFamily
)

object PinLayoutDetector {
    private val pinLayouts = listOf(
        "INVALID",
        "Speeduino v0.2",
        "Speeduino v0.3",
        "Speeduino v0.4",
        "INVALID",
        "INVALID",
        "01-05 MX5 PNP",
        "INVALID",
        "96-97 MX5 PNP",
        "NA6 MX5 PNP",
        "Turtana PCB",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "Plazomat I/O 0.1",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "Daz V6 Shield 0.1",
        "BMW PnP",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "NO2C",
        "UA4C",
        "BlitzboxBL49sp",
        "INVALID",
        "INVALID",
        "DIY-EFI CORE4 v1.0",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "dvjcodec Teensy RevA",
        "dvjcodec Teensy RevB",
        "INVALID",
        "JUICEBOX",
        "INVALID",
        "Drop Bear",
        "INVALID",
        "INVALID",
        "INVALID",
        "INVALID",
        "Black STM32F407VET6 V0.1",
        "INVALID",
        "INVALID",
        "INVALID",
        "\$invalid_x128",
        "\$invalid_x64"
    )

    fun fromPage1(data: ByteArray): PinLayoutInfo {
        require(data.size >= 16) { "Page 1 deve ter pelo menos 16 bytes" }
        val index = data[15].toInt() and 0xFF
        return fromIndex(index)
    }

    fun fromIndex(index: Int): PinLayoutInfo {
        val rawName = pinLayouts.getOrNull(index)
        val cleanedName = rawName
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { !it.equals("INVALID", ignoreCase = true) }
            ?.takeIf { !it.startsWith("\$invalid", ignoreCase = true) }

        val family = when {
            rawName?.contains("Teensy", ignoreCase = true) == true -> McuFamily.TEENSY
            rawName?.contains("STM32", ignoreCase = true) == true -> McuFamily.STM32
            cleanedName != null -> McuFamily.AVR
            else -> McuFamily.UNKNOWN
        }

        return PinLayoutInfo(
            index = index,
            name = cleanedName,
            mcuFamily = family
        )
    }
}
