package com.speeduino.manager

/**
 * Dados em tempo real do Speeduino.
 */
data class SpeeduinoLiveData(
    val secl: Int,
    val rpm: Int,
    val coolantTemp: Int,
    val intakeTemp: Int,
    val mapPressure: Int,
    val tps: Int,
    val batteryVoltage: Double,
    val advance: Int,
    val o2: Int,
    val engineStatus: Int,
    val sparkStatus: Int,
    val candidateSpeedKph: Int? = null,
    val candidateAccelPedalPosPct: Int? = null,
    val candidateGear: Int? = null,
    val candidateThrottleAngleDeg: Double? = null,
    val candidateIgnitionAdvanceDeg: Double? = null,
    val candidateInjectionDurationMs: Double? = null,
    val candidateInjectionDurationMirrorMs: Double? = null,
    val gpsSpeedKph: Double? = null,
    val outputChannelBlockSize: Int = 0,
    val outputChannelData: ByteArray? = null
) {
    val isEngineRunning: Boolean
        get() = (engineStatus and 0x01) != 0

    val hasSpark: Boolean
        get() = (sparkStatus and 0x01) != 0
}
