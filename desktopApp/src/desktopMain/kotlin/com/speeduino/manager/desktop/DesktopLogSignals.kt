package com.speeduino.manager.desktop

internal data class LogExportSignal(
    val key: String,
    val header: String,
    val labelKey: String
)

internal val LogExportSignals = listOf(
    LogExportSignal("rpm", "rpm", "label.rpm"),
    LogExportSignal("map", "map_kpa", "label.map"),
    LogExportSignal("tps", "tps", "label.tps"),
    LogExportSignal("coolant", "coolant_c", "label.coolant"),
    LogExportSignal("iat", "iat_c", "label.iat"),
    LogExportSignal("battery", "battery_v", "label.battery"),
    LogExportSignal("advance", "advance_deg", "label.advance"),
    LogExportSignal("afr", "afr", "label.afr"),
    LogExportSignal("afr_target", "afr_target", "label.afrTarget")
)

internal val DefaultSelectedLogSignals: Set<String> = LogExportSignals.map { it.key }.toSet()
