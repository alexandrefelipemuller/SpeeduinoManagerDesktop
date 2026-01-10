package com.speeduino.manager.connection

import com.fazecast.jSerialComm.SerialPort
import com.speeduino.manager.shared.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SpeeduinoSerialConnection(
    private val portDescriptor: String,
    private val baudRate: Int = 115200,
    private val timeoutMs: Int = 1000,
    private val enableModernProtocol: Boolean = true
) : ISpeeduinoConnection {

    companion object {
        private const val TAG = "SpeeduinoSerial"

        fun listPorts(): List<String> = SerialPort.getCommPorts().map { it.systemPortName }.toList()
    }

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
            selected.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)
            selected.setComPortTimeouts(
                SerialPort.TIMEOUT_NONBLOCKING,
                0,
                timeoutMs
            )

            if (!selected.openPort()) {
                throw Exception("Nao foi possivel abrir a porta serial $portDescriptor")
            }

            selected.flushIOBuffers()
            val dtrCleared = selected.clearDTR()
            val rtsCleared = selected.clearRTS()
            Logger.d(TAG, "DTR/RTS cleared: dtr=$dtrCleared rts=$rtsCleared")
            delay(50)
            val dtrSet = selected.setDTR()
            val rtsSet = selected.setRTS()
            Logger.d(TAG, "DTR/RTS set: dtr=$dtrSet rts=$rtsSet")
            delay(1000)

            port = selected
            inputStream = selected.inputStream
            outputStream = selected.outputStream
            isConnected = true
            onConnectionStateChanged?.invoke(true)
            Logger.d(TAG, "Conexao serial aberta: $portDescriptor @ $baudRate")
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
            Logger.d(TAG, "Send ${data.size} bytes: ${data.previewHex()}")
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
                readExact(size)
            } else {
                readAvailable()
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

    override fun supportsModernProtocol(): Boolean = enableModernProtocol

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

    private fun readExact(size: Int): ByteArray {
        Logger.d(TAG, "Receive exact $size bytes (timeout ${timeoutMs}ms)")
        val buffer = ByteArray(size)
        var totalRead = 0
        val deadline = System.currentTimeMillis() + timeoutMs
        val selected = port ?: return ByteArray(0)

        while (totalRead < size) {
            val available = selected?.bytesAvailable() ?: -1
            if (available > 0) {
                val toRead = minOf(available, size - totalRead)
                val read = selected.readBytes(buffer, toRead, totalRead)
                if (read > 0) {
                    totalRead += read
                    continue
                }
            }
            if (System.currentTimeMillis() >= deadline) {
                Logger.w(TAG, "Receive timeout after ${timeoutMs}ms (got $totalRead/$size)")
                break
            }
            Thread.sleep(5)
        }

        val result = buffer.copyOf(totalRead)
        Logger.d(TAG, "Receive exact got ${result.size} bytes: ${result.previewHex()}")
        return result
    }

    private fun readAvailable(): ByteArray {
        val selected = port ?: return ByteArray(0)
        val available = selected?.bytesAvailable() ?: 0
        if (available <= 0) {
            return ByteArray(0)
        }

        val buffer = ByteArray(minOf(available, 2048))
        val read = selected.readBytes(buffer, buffer.size)
        val result = buffer.copyOf(maxOf(read, 0))
        Logger.d(TAG, "Receive (read avail) ${result.size} bytes: ${result.previewHex()}")
        return result
    }

    private fun ByteArray.previewHex(max: Int = 32): String {
        val shown = take(max).joinToString(" ") { "0x%02X".format(it) }
        return if (size > max) "$shown ..." else shown
    }
}
