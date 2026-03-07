package com.speeduino.manager.connection

interface ConnectionTraceSink {
    fun onTx(transport: String, data: ByteArray)
    fun onRx(transport: String, data: ByteArray)
    fun onInfo(transport: String, message: String)
    fun onError(transport: String, message: String, throwable: Throwable? = null)
}

object ConnectionTrace {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var sink: ConnectionTraceSink? = null

    fun tx(transport: String, data: ByteArray) {
        if (!enabled) return
        sink?.onTx(transport, data)
    }

    fun rx(transport: String, data: ByteArray) {
        if (!enabled) return
        sink?.onRx(transport, data)
    }

    fun info(transport: String, message: String) {
        if (!enabled) return
        sink?.onInfo(transport, message)
    }

    fun error(transport: String, message: String, throwable: Throwable? = null) {
        if (!enabled) return
        sink?.onError(transport, message, throwable)
    }
}
