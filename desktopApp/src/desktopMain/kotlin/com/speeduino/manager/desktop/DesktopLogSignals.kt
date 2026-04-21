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
    LogExportSignal("afr_target", "afr_target", "label.afrTarget"),
    LogExportSignal("candidate_speed", "candidate_speed_kph", "label.candidateSpeed"),
    LogExportSignal("candidate_pedal", "candidate_pedal_pct", "label.candidatePedal"),
    LogExportSignal("candidate_gear", "candidate_gear", "label.candidateGear"),
    LogExportSignal("candidate_throttle_angle", "candidate_throttle_angle_deg", "label.candidateThrottleAngle"),
    LogExportSignal("candidate_ignition_advance", "candidate_ignition_advance_deg", "label.candidateIgnitionAdvance"),
    LogExportSignal("candidate_injection_ms", "candidate_inj_time_ms", "label.candidateInjectionTime"),
    LogExportSignal("candidate_injection_mirror_ms", "candidate_inj_time_mirror_ms", "label.candidateInjectionMirror")
)

internal val DefaultSelectedLogSignals: Set<String> = LogExportSignals.map { it.key }.toSet()
