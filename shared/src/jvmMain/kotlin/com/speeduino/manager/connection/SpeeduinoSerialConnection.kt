package com.speeduino.manager.connection

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SpeeduinoSerialConnection(
    private val portDescriptor: String,
    private val baudRate: Int = 115200,
    private val timeoutMs: Int = 1000
) : ISpeeduinoConnection {

    private var port: SerialPort? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    private var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    override suspend fun connect(): Unit = withContext(Dispatchers.IO) {
        try {
            val selected = SerialPort.getCommPort(portDescriptor)
            selected.baudRate = baudRate
            selected.numDataBits = 8
            selected.numStopBits = SerialPort.ONE_STOP_BIT
            selected.parity = SerialPort.NO_PARITY
            selected.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING,
                timeoutMs,
                timeoutMs
            )

            if (!selected.openPort()) {
                throw Exception("Nao foi possivel abrir a porta serial $portDescriptor")
            }

            port = selected
            inputStream = selected.inputStream
            outputStream = selected.outputStream
            isConnected = true
            onConnectionStateChanged?.invoke(true)
        } catch (e: Exception) {
            isConnected = false
            onConnectionStateChanged?.invoke(false)
            handleError("Falha na conexao serial: ${e.message}")
            throw e
        }
    }

    override fun disconnect() {
        isConnected = false
        try {
            inputStream?.close()
            outputStream?.close()
            port?.closePort()
        } catch (_: Exception) {
        } finally {
            inputStream = null
            outputStream = null
            port = null
        }
        onConnectionStateChanged?.invoke(false)
    }

    override fun send(data: ByteArray) {
        if (!isConnected) throw Exception("Nao conectado")
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            handleError("Erro ao enviar dados: ${e.message}")
            throw e
        }
    }

    override fun receive(size: Int): ByteArray {
        if (!isConnected) throw Exception("Nao conectado")
        return try {
            if (size > 0) {
                val buffer = ByteArray(size)
                var totalRead = 0
                while (totalRead < size) {
                    val read = inputStream?.read(buffer, totalRead, size - totalRead) ?: -1
                    if (read <= 0) break
                    totalRead += read
                }
                buffer.copyOf(totalRead)
            } else {
                val buffer = ByteArray(2048)
                val bytesRead = inputStream?.read(buffer) ?: 0
                buffer.copyOf(bytesRead)
            }
        } catch (e: Exception) {
            handleError("Erro ao receber dados: ${e.message}")
            throw e
        }
    }

    override fun isConnected(): Boolean = isConnected

    override fun getConnectionInfo(): String {
        return "Serial: $portDescriptor @ $baudRate (${if (isConnected) "Conectado" else "Desconectado"})"
    }

    override fun supportsModernProtocol(): Boolean = false

    override fun setOnConnectionStateChanged(callback: (Boolean) -> Unit) {
        onConnectionStateChanged = callback
    }

    override fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }

    override fun close() {
        disconnect()
    }

    private fun handleError(message: String) {
        isConnected = false
        onError?.invoke(message)
        onConnectionStateChanged?.invoke(false)
    }

    companion object {
        fun listPorts(): List<String> = SerialPort.getCommPorts().map { it.systemPortName }.toList()
    }
}
