package com.speeduino.manager

import com.speeduino.manager.connection.ISpeeduinoConnection
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SpeeduinoClientFirmwareHandshakeTest {

    @Test
    fun connectFallsBackToLegacySWhenQIsGarbage() = runBlocking {
        val connection = FakeConnection(
            responses = mapOf(
                'Q'.code.toByte() to arrayDequeOf(
                    byteArrayOf(0xFF.toByte())
                ),
                'S'.code.toByte() to arrayDequeOf(
                    "@@A speeduino 202501\u0000A??".toByteArray(Charsets.US_ASCII),
                    "ByRocha1\u0000".toByteArray(Charsets.US_ASCII),
                ),
                'p'.code.toByte() to arrayDequeOf(ByteArray(16)),
            )
        )

        val client = SpeeduinoClient(
            connection = connection,
            onDataReceived = {},
            onConnectionStateChanged = {},
            onError = {},
        )

        client.connect()

        val firmwareInfo = assertNotNull(client.getFirmwareInfoCached())
        assertEquals("speeduino 202501", firmwareInfo.signature)
        assertEquals("ByRocha1", firmwareInfo.productString)
        assertEquals(listOf('Q', 'S', 'S', 'p'), connection.sentCommands)
    }

    @Test
    fun connectRetriesLegacyHandshakeAfterUnreadableCandidate() = runBlocking {
        val connection = FakeConnection(
            responses = mapOf(
                'Q'.code.toByte() to arrayDequeOf(
                    byteArrayOf(0xFF.toByte()),
                    "speeduino 202501\u0000".toByteArray(Charsets.US_ASCII),
                ),
                'S'.code.toByte() to arrayDequeOf(
                    byteArrayOf(0xFE.toByte()),
                    "ByRocha1\u0000".toByteArray(Charsets.US_ASCII),
                ),
                'p'.code.toByte() to arrayDequeOf(ByteArray(16)),
            )
        )

        val client = SpeeduinoClient(
            connection = connection,
            onDataReceived = {},
            onConnectionStateChanged = {},
            onError = {},
        )

        client.connect()

        val firmwareInfo = assertNotNull(client.getFirmwareInfoCached())
        assertEquals("speeduino 202501", firmwareInfo.signature)
        assertEquals(2, connection.clearInputBufferCalls)
    }

    private class FakeConnection(
        responses: Map<Byte, ArrayDeque<ByteArray>>
    ) : ISpeeduinoConnection {
        private val queuedResponses = responses
            .mapValues { (_, queue) -> ArrayDeque(queue) }
            .toMutableMap()
        private var connected = false
        private var lastCommand: Byte? = null

        val sentCommands = mutableListOf<Char>()
        var clearInputBufferCalls = 0
            private set

        override suspend fun connect() {
            connected = true
        }

        override fun disconnect() {
            connected = false
        }

        override fun send(data: ByteArray) {
            lastCommand = data.firstOrNull()
            lastCommand?.let { sentCommands += it.toInt().toChar() }
        }

        override fun receive(size: Int): ByteArray {
            val command = lastCommand ?: error("receive() without previous send()")
            val queue = queuedResponses[command] ?: ArrayDeque()
            val response = queue.removeFirstOrNull() ?: ByteArray(0)
            queuedResponses[command] = queue
            return response
        }

        override fun isConnected(): Boolean = connected

        override fun getConnectionInfo(): String = "fake"

        override fun supportsModernProtocol(): Boolean = false

        override fun supportsModernProtocolFallback(): Boolean = false

        override fun prefersLegacyProtocol(): Boolean = true

        override fun legacyFirmwareHandshakeAttempts(): Int = 2

        override fun setOnConnectionStateChanged(callback: (Boolean) -> Unit) = Unit

        override fun setOnError(callback: (String) -> Unit) = Unit

        override fun clearInputBuffer() {
            clearInputBufferCalls += 1
        }
    }

    private fun arrayDequeOf(vararg values: ByteArray): ArrayDeque<ByteArray> = ArrayDeque(values.toList())
}
