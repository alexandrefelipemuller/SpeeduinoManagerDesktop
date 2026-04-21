package com.speeduino.manager.protocol

/**
 * Capacidades seriais do Speeduino.
 */
data class SerialCapability(
    val protocolVersion: Int,
    val blockingFactor: Int,
    val tableBlockingFactor: Int,
)
