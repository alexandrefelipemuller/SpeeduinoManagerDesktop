package com.speeduino.manager.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Implementação TCP/IP para comunicação com Speeduino
 *
 * Conecta via socket TCP para comunicar com Speeduino ou simulador.
 * Suporta tanto emuladores Android (10.0.2.2) quanto dispositivos físicos (IP real).
 */
class SpeeduinoTcpConnection(
    private val host: String,
    private val port: Int,
    private val timeoutMs: Int = 5000
) : ISpeeduinoConnection {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var isConnected = false

    private var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    override suspend fun connect(): Unit = withContext(Dispatchers.IO) {
        try {
            println("[TCP] Tentando conectar a $host:$port (timeout: ${timeoutMs}ms)")

            socket = Socket(host, port).apply {
                soTimeout = timeoutMs
                tcpNoDelay = true // Disable Nagle's algorithm for low latency
                keepAlive = true
            }

            println("[TCP] Socket conectado: ${socket?.isConnected}")

            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()

            println("[TCP] Streams criados - InputStream: ${inputStream != null}, OutputStream: ${outputStream != null}")

            isConnected = true
            onConnectionStateChanged?.invoke(true)

            println("[TCP] Conexão estabelecida com sucesso")

        } catch (e: Exception) {
            println("[TCP] ERRO na conexão: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            isConnected = false
            onConnectionStateChanged?.invoke(false)
            throw Exception("Falha na conexão TCP: ${e.message}")
        }
    }

    override fun disconnect() {
        isConnected = false

        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            // Ignore close errors
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }

        onConnectionStateChanged?.invoke(false)
    }

    override fun send(data: ByteArray) {
        if (!isConnected) {
            throw Exception("Não conectado")
        }

        try {
            println("[TCP] Enviando ${data.size} bytes: ${data.joinToString(" ") { "%02X".format(it) }}")
            outputStream?.write(data)
            outputStream?.flush()
            println("[TCP] Dados enviados com sucesso")
        } catch (e: Exception) {
            println("[TCP] ERRO ao enviar: ${e.javaClass.simpleName} - ${e.message}")
            handleError("Erro ao enviar dados: ${e.message}")
            throw e
        }
    }

    override fun receive(size: Int): ByteArray {
        if (!isConnected) {
            throw Exception("Não conectado")
        }

        return try {
            println("[TCP] Aguardando receber ${if (size > 0) "$size bytes" else "dados disponíveis"}")

            val result = if (size > 0) {
                // Read exact number of bytes
                val buffer = ByteArray(size)
                var totalRead = 0
                val startTime = System.currentTimeMillis()

                while (totalRead < size) {
                    val remaining = size - totalRead
                    println("[TCP] Lendo $remaining bytes restantes (já leu $totalRead/$size)")

                    val read = inputStream?.read(buffer, totalRead, remaining) ?: -1
                    if (read == -1) {
                        println("[TCP] Stream encerrado (-1)")
                        break
                    }
                    totalRead += read

                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed > timeoutMs) {
                        println("[TCP] Timeout excedido: ${elapsed}ms > ${timeoutMs}ms")
                        break
                    }
                }
                buffer.copyOf(totalRead)
            } else {
                // Read until timeout
                val buffer = ByteArray(2048)
                val bytesRead = inputStream?.read(buffer) ?: 0
                println("[TCP] Leu $bytesRead bytes disponíveis")
                buffer.copyOf(bytesRead)
            }

            println("[TCP] Recebeu ${result.size} bytes: ${result.joinToString(" ") { "%02X".format(it) }}")
            result
        } catch (e: Exception) {
            println("[TCP] ERRO ao receber: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            handleError("Erro ao receber dados: ${e.message}")
            throw e
        }
    }

    override fun isConnected(): Boolean = isConnected

    override fun getConnectionInfo(): String {
        return "TCP: $host:$port (${if (isConnected) "Conectado" else "Desconectado"})"
    }

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
}
