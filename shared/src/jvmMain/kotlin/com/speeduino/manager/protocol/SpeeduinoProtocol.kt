package com.speeduino.manager.protocol

import com.speeduino.manager.formatPageId
import com.speeduino.manager.shared.Logger
import com.speeduino.manager.connection.ISpeeduinoConnection
import com.speeduino.manager.model.EcuFamily
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.jvm.Synchronized

/**
 * Implementação do protocolo de comunicação Speeduino
 *
 * Independente do transporte - funciona com qualquer implementação de ISpeeduinoConnection.
 * Suporta comandos Legacy (single-byte) e Modern (CRC32-based).
 *
 * Based on Speeduino firmware 202402
 */
class SpeeduinoProtocol(
    private val connection: ISpeeduinoConnection
) {

    companion object {
        // Master switch apenas para debug local. Manter false em produção:
        // USB/BT já forçam legado via supportsModernProtocol() = false
        // e TCP/Wi-Fi precisa de modern protocol (simulador e firmwares novos).
        private const val FORCE_LEGACY_PROTOCOL = false

        // Response codes
        const val SERIAL_RC_OK = 0x00.toByte()
        const val SERIAL_RC_BURN_OK = 0x04.toByte()
        const val SERIAL_RC_TIMEOUT = 0x80.toByte()
        const val SERIAL_RC_CRC_ERR = 0x82.toByte()
        const val SERIAL_RC_UKWN_ERR = 0x83.toByte()
        const val SERIAL_RC_RANGE_ERR = 0x84.toByte()  // Value out of range (bins não crescentes, etc)
        const val SERIAL_RC_BUSY_ERR = 0x85.toByte()

        // Commands
        const val CMD_LIVE_DATA = 'A'
        const val CMD_PROTOCOL_VERSION = 'F'
        const val CMD_FIRMWARE_VERSION = 'Q'
        const val CMD_PRODUCT_STRING = 'S'
        const val CMD_CAN_ID = 'I'
        const val CMD_CAPABILITY = 'f'
        const val CMD_PAGE_CRC = 'd'
        const val CMD_PAGE_READ = 'p'
        const val CMD_PAGE_WRITE = 'M'
        const val CMD_PAGE_SET = 'P'
        const val CMD_PAGE_WRITE_LEGACY = 'W'
        const val CMD_BURN = 'B'
        const val CMD_OUTPUT_CHANNELS = 'r'

        const val LIVE_DATA_SIZE = 128
    }

    private var lastLegacyWrittenPage: Byte? = null

    private fun isModernEnabled(): Boolean = !FORCE_LEGACY_PROTOCOL && connection.supportsModernProtocol()

    /**
     * Obtém informações do firmware (comando 'Q')
     */
    suspend fun getFirmwareInfo(): String {
        return queryStringCandidates(
            commands = listOf('Q'.code.toByte(), 'S'.code.toByte()),
            label = "firmware info"
        )
    }

    /**
     * Consulta estrita legacy para assinatura de firmware (somente comando 'Q').
     * Usado no caminho de compatibilidade USB/serial para manter comportamento antigo.
     */
    suspend fun getFirmwareInfoLegacyStrict(): String {
        val response = sendLegacyCommand('Q'.code.toByte())
        if (response.isEmpty()) {
            throw Exception("Legacy firmware info returned empty response")
        }
        val parsed = parseLegacyStringResponse(response, "firmware info")
        if (parsed.isBlank()) {
            throw Exception("Legacy firmware info returned blank response")
        }
        return parsed
    }

    /**
     * Obtém string do produto (comando 'S')
     */
    suspend fun getProductString(signatureHint: String? = null): String {
        val commands = if (signatureHint?.trim()?.startsWith("rusEFI", ignoreCase = true) == true) {
            listOf('V'.code.toByte(), 'S'.code.toByte())
        } else {
            listOf('S'.code.toByte(), 'V'.code.toByte())
        }
        return queryStringCandidates(commands, "product string")
    }

    /**
     * Obtém capacidades seriais (comando 'f')
     */
    suspend fun getSerialCapability(): SerialCapability {
        if (!isModernEnabled()) {
            Logger.w("SpeeduinoProtocol", "Modern protocol disabled, skipping serial capability query")
            return SerialCapability(0, 0, 0)
        }

        return try {
            val response = sendModernCommand('f'.code.toByte(), byteArrayOf(0x00))

            if (response.size >= 6 && response[0] == SERIAL_RC_OK) {
                val protocolVersion = response[1].toInt() and 0xFF
                val blockingFactor = ((response[2].toInt() and 0xFF) shl 8) or (response[3].toInt() and 0xFF)
                val tableBlockingFactor = ((response[4].toInt() and 0xFF) shl 8) or (response[5].toInt() and 0xFF)
                SerialCapability(protocolVersion, blockingFactor, tableBlockingFactor)
            } else {
                fallbackProtocolCapability()
            }
        } catch (e: Exception) {
            Logger.w("SpeeduinoProtocol", "Modern serial capability query failed, trying protocol-version fallback: ${e.message}")
            fallbackProtocolCapability()
        }
    }

    /**
     * Lê CRC32 de uma página (comando 'd')
     */
    suspend fun getPageCRC(pageNum: Byte): Long {
        if (!isModernEnabled()) {
            val response = sendLegacyCommand(
                'd'.code.toByte(),
                payload = byteArrayOf(0x00, pageNum),
                responseSize = 4
            )
            return if (response.size == 4) {
                ByteBuffer.wrap(response)
                    .order(ByteOrder.BIG_ENDIAN)
                    .int
                    .toLong() and 0xFFFFFFFF
            } else {
                0L
            }
        }

        val response = sendModernCommand('d'.code.toByte(), byteArrayOf(pageNum))

        return if (response.size >= 5 && response[0] == SERIAL_RC_OK) {
            val crc = ByteBuffer.wrap(response, 1, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .int
                .toLong() and 0xFFFFFFFF
            crc
        } else {
            0L
        }
    }

    /**
     * Lê uma página completa (comando 'p')
     * @param pageNum Número da página (0-15)
     * @param offset Offset inicial
     * @param length Tamanho a ler
     */
    suspend fun readPage(pageNum: Byte, offset: Int, length: Int): ByteArray {
        if (!isModernEnabled()) {
            val payload = ByteArray(6)
            payload[0] = 0x00  // Padding byte
            payload[1] = pageNum
            payload[2] = (offset and 0xFF).toByte()
            payload[3] = ((offset shr 8) and 0xFF).toByte()
            payload[4] = (length and 0xFF).toByte()
            payload[5] = ((length shr 8) and 0xFF).toByte()

            Logger.d("SpeeduinoProtocol", "readPage (LEGACY): pageNum=${formatPageId(pageNum)}, offset=$offset, length=$length")
            sendLegacyCommand('p'.code.toByte(), payload = payload, expectResponse = false)
            return connection.receive(length)
        }

        val payload = ByteArray(6)
        payload[0] = 0x00  // Padding byte
        payload[1] = pageNum

        // Offset (2 bytes, little-endian)
        payload[2] = (offset and 0xFF).toByte()           // LSB first
        payload[3] = ((offset shr 8) and 0xFF).toByte()   // MSB second

        // Length (2 bytes, little-endian)
        payload[4] = (length and 0xFF).toByte()           // LSB first
        payload[5] = ((length shr 8) and 0xFF).toByte()   // MSB second

        Logger.d("SpeeduinoProtocol", "readPage: pageNum=${formatPageId(pageNum)}, offset=$offset, length=$length")
        Logger.d("SpeeduinoProtocol", "Payload bytes: ${payload.joinToString(" ") { "0x%02X".format(it) }}")

        val response = sendModernCommand('p'.code.toByte(), payload)

        Logger.d("SpeeduinoProtocol", "Response size: ${response.size} bytes")
        Logger.d("SpeeduinoProtocol", "Response first bytes: ${response.take(10).joinToString(" ") { "0x%02X".format(it) }}")

        return if (response.isNotEmpty() && response[0] == SERIAL_RC_OK) {
            response.copyOfRange(1, response.size)
        } else {
            throw Exception("Erro ao ler página ${formatPageId(pageNum)}")
        }
    }

    /**
     * Lê uma tabela/genpage via protocolo moderno ('r' + table id).
     * Usado por ECUs MS3/newserial.
     */
    suspend fun readTable(tableId: Byte, offset: Int, length: Int): ByteArray {
        return readTable(tableId.toInt() and 0xFF, offset, length, EcuFamily.MS3)
    }

    suspend fun readTable(tableId: Int, offset: Int, length: Int, family: EcuFamily): ByteArray {
        if (!isModernEnabled()) {
            throw Exception("Modern protocol disabled for table read")
        }

        val response = if (family == EcuFamily.RUSEFI) {
            val payload = byteArrayOf(
                (tableId and 0xFF).toByte(),
                ((tableId shr 8) and 0xFF).toByte(),
                (offset and 0xFF).toByte(),
                ((offset shr 8) and 0xFF).toByte(),
                (length and 0xFF).toByte(),
                ((length shr 8) and 0xFF).toByte(),
            )
            sendModernCommand('R'.code.toByte(), payload)
        } else {
            val payload = ByteArray(6)
            payload[0] = 0x00
            payload[1] = (tableId and 0xFF).toByte()
            payload[2] = ((offset shr 8) and 0xFF).toByte()
            payload[3] = (offset and 0xFF).toByte()
            payload[4] = ((length shr 8) and 0xFF).toByte()
            payload[5] = (length and 0xFF).toByte()
            sendModernCommand('r'.code.toByte(), payload)
        }

        return if (response.isNotEmpty() && response[0] == SERIAL_RC_OK) {
            response.copyOfRange(1, response.size)
        } else {
            throw Exception("Erro ao ler tabela ${formatPageId(tableId)}")
        }
    }

    /**
     * Lê dados em tempo real (comando 'A' legacy)
     * ⚠️ DEPRECATED: Speeduino modernas (202402+) podem ter isso desabilitado
     */
    suspend fun readLiveData(expectedSize: Int = LIVE_DATA_SIZE): ByteArray {
        require(expectedSize > 0) { "Tamanho inválido de live data: $expectedSize" }
        val response = try {
            sendLegacyCommand('A'.code.toByte(), responseSize = expectedSize)
        } catch (e: Exception) {
            if (!isZeroByteTimeout(e, expectedSize)) {
                throw e
            }

            Logger.w(
                "SpeeduinoProtocol",
                "LiveData timeout 0 bytes (expected=$expectedSize), flushing input and retrying once"
            )
            connection.clearInputBuffer()
            delay(25)
            sendLegacyCommand('A'.code.toByte(), responseSize = expectedSize)
        }

        return if (response.size >= expectedSize) {
            response
        } else {
            throw Exception("Resposta de live data incompleta: ${response.size}/$expectedSize bytes")
        }
    }

    private fun isZeroByteTimeout(error: Exception, expectedSize: Int): Boolean {
        val message = error.message ?: return false
        return message.contains("Timeout: expected $expectedSize bytes, received 0")
    }

    /**
     * Lê dados em tempo real usando Modern Protocol (comando 'r' + Output Channels)
     * ✅ RECOMENDADO: Comando usado pelo TunerStudio
     *
     * Formato: 'r' + CAN_ID + Subcmd + Offset(LSB/MSB) + Length(LSB/MSB)
     *          0x72  0x00    0x30    0x0000        0x007F (127 bytes)
     */
    suspend fun readLiveDataModern(length: Int): ByteArray {
        if (!isModernEnabled()) {
            throw Exception("Modern protocol disabled for this connection")
        }
        require(length > 0) { "Live data length inválido: $length" }

        val lengthLsb = (length and 0xFF).toByte()
        val lengthMsb = ((length shr 8) and 0xFF).toByte()

        // Payload: 'r' + CAN_ID(0x00) + Subcmd(0x30) + Offset(0x0000) + Length
        val payload = byteArrayOf(
            0x00,        // CAN ID (always 0 for standard Speeduino)
            0x30.toByte(), // Subcmd: Output Channels (48 decimal)
            0x00,        // Offset LSB (start at byte 0)
            0x00,        // Offset MSB
            lengthLsb,   // Length LSB
            lengthMsb    // Length MSB
        )

        val response = sendModernCommand('r'.code.toByte(), payload)

        return if (response.isNotEmpty() && response[0] == SERIAL_RC_OK) {
            // Retorna dados sem o response code
            response.sliceArray(1 until response.size)
        } else {
            val code = response.getOrNull(0)?.toInt()?.and(0xFF)
            throw Exception("Erro ao ler live data modern: response code = ${code?.let { "0x${it.toString(16)}" } ?: "null"}")
        }
    }

    suspend fun readRusefiOutputChannels(length: Int, offset: Int = 0): ByteArray {
        if (!isModernEnabled()) {
            throw Exception("Modern protocol disabled for rusEFI output channels")
        }
        require(length > 0) { "rusEFI output length inválido: $length" }

        val payload = byteArrayOf(
            (offset and 0xFF).toByte(),
            ((offset shr 8) and 0xFF).toByte(),
            (length and 0xFF).toByte(),
            ((length shr 8) and 0xFF).toByte(),
        )

        val response = sendModernCommand(
            'O'.code.toByte(),
            payload,
            maxResponseSize = length + 1
        )
        return if (response.isNotEmpty() && response[0] == SERIAL_RC_OK) {
            response.copyOfRange(1, response.size)
        } else {
            val code = response.getOrNull(0)?.toInt()?.and(0xFF)
            throw Exception("Erro ao ler output channels rusEFI: ${code?.let { "0x${it.toString(16)}" } ?: "null"}")
        }
    }

    /**
     * Envia comando legacy (single-byte)
     */
    // Base de conhecimento (USB serial/OTG): sem serialização global, chamadas paralelas
    // (ex.: stream + write/refresh) podem misturar TX/RX no mesmo canal e corromper frame.
    @Synchronized
    private fun sendLegacyCommand(
        cmd: Byte,
        payload: ByteArray = byteArrayOf(),
        responseSize: Int? = null,
        expectResponse: Boolean = true
    ): ByteArray {
        if (!connection.isConnected()) {
            throw Exception("Não conectado")
        }

        val packet = if (payload.isEmpty()) {
            byteArrayOf(cmd)
        } else {
            byteArrayOf(cmd) + payload
        }

        connection.send(packet)
        if (!expectResponse) {
            return ByteArray(0)
        }

        return if (responseSize != null) {
            connection.receive(responseSize)
        } else {
            connection.receive()
        }
    }

    private fun fallbackLegacyString(cmd: Byte, label: String): String {
        val response = sendLegacyCommand(cmd)
        if (response.isEmpty()) {
            Logger.w("SpeeduinoProtocol", "Legacy $label returned empty response")
            return "Unknown"
        }

        val cleaned = parseLegacyStringResponse(response, label)

        Logger.w("SpeeduinoProtocol", "Legacy $label response: ${response.joinToString(" ") { "0x%02X".format(it) }}")
        return cleaned.ifBlank { "Unknown" }
    }

    private fun parseLegacyStringResponse(response: ByteArray, label: String): String {
        val framedString = parseModernFrameString(response, label)
        if (framedString != null) {
            return framedString
        }

        val zeroIndex = response.indexOf(0)
        val length = if (zeroIndex >= 0) zeroIndex else response.size
        return String(response, 0, length, Charsets.US_ASCII).trim()
    }

    private fun parseModernFrameString(response: ByteArray, label: String): String? {
        if (response.size < 7) {
            return null
        }

        val length = ((response[0].toInt() and 0xFF) shl 8) or (response[1].toInt() and 0xFF)
        if (length <= 0 || length > 2048) {
            return null
        }

        val expectedSize = 2 + length + 4
        if (response.size < expectedSize) {
            Logger.w("SpeeduinoProtocol", "Legacy $label frame incomplete: ${response.size}/$expectedSize bytes")
            return null
        }

        val payload = response.copyOfRange(2, 2 + length)
        val crcBytes = response.copyOfRange(2 + length, 2 + length + 4)
        val receivedCrc = ByteBuffer.wrap(crcBytes)
            .order(ByteOrder.BIG_ENDIAN)
            .int
            .toLong() and 0xFFFFFFFF
        val calculatedCrc = calculateCRC32(payload)

        if (receivedCrc != 0L && receivedCrc != calculatedCrc) {
            Logger.w(
                "SpeeduinoProtocol",
                "Legacy $label frame CRC mismatch (received=0x${receivedCrc.toString(16)}, calculated=0x${calculatedCrc.toString(16)})"
            )
        }

        val payloadText = if (payload.isNotEmpty() && payload[0] == SERIAL_RC_OK) {
            payload.copyOfRange(1, payload.size)
        } else {
            payload
        }

        val zeroIndex = payloadText.indexOf(0)
        val lengthText = if (zeroIndex >= 0) zeroIndex else payloadText.size
        return String(payloadText, 0, lengthText, Charsets.US_ASCII).trim()
    }

    private fun safeLegacyString(cmd: Byte, label: String): String {
        return try {
            fallbackLegacyString(cmd, label)
        } catch (e: Exception) {
            Logger.w("SpeeduinoProtocol", "Legacy $label failed: ${e.message}")
            "Unknown"
        }
    }

    private fun queryStringCandidates(commands: List<Byte>, label: String): String {
        commands.forEach { cmd ->
            val modernValue = runCatching { queryModernString(cmd) }
                .onFailure { Logger.w("SpeeduinoProtocol", "Modern $label cmd=${cmd.toInt().toChar()} failed: ${it.message}") }
                .getOrNull()
            if (!modernValue.isNullOrBlank()) {
                return modernValue
            }

            val legacyValue = runCatching { queryLegacyString(cmd, label) }
                .onFailure { Logger.w("SpeeduinoProtocol", "Legacy $label cmd=${cmd.toInt().toChar()} failed: ${it.message}") }
                .getOrNull()
            if (!legacyValue.isNullOrBlank() && !legacyValue.equals("Unknown", ignoreCase = true)) {
                return legacyValue
            }
        }

        return "Unknown"
    }

    private fun queryModernString(cmd: Byte): String? {
        if (!isModernEnabled()) return null

        val response = sendModernCommand(cmd, byteArrayOf())
        if (response.isEmpty() || response[0] != SERIAL_RC_OK || response.size <= 1) {
            return null
        }

        return String(response, 1, response.size - 1).trim().takeIf { it.isNotBlank() }
    }

    private fun queryLegacyString(cmd: Byte, label: String): String? {
        val response = sendLegacyCommand(cmd)
        if (response.isEmpty()) {
            Logger.w("SpeeduinoProtocol", "Legacy $label returned empty response")
            return null
        }

        return parseLegacyStringResponse(response, label).takeIf { it.isNotBlank() }
    }

    private fun fallbackProtocolCapability(): SerialCapability {
        val versionText = queryStringCandidates(listOf('F'.code.toByte()), "protocol version")
        val versionNumber = versionText.filter { it.isDigit() }.toIntOrNull() ?: 1
        return SerialCapability(
            protocolVersion = versionNumber,
            blockingFactor = 1024,
            tableBlockingFactor = 1024,
        )
    }

    /**
     * Envia comando modern (CRC32-based)
     */
    // Mantém atomicidade send+receive também no caminho modern.
    @Synchronized
    private fun sendModernCommand(
        cmd: Byte,
        extraPayload: ByteArray,
        maxResponseSize: Int = 2048
    ): ByteArray {
        if (!isModernEnabled()) {
            throw Exception("Modern protocol disabled for this connection")
        }
        if (!connection.isConnected()) {
            throw Exception("Não conectado")
        }

        val payload = byteArrayOf(cmd) + extraPayload
        val crc = calculateCRC32(payload)

        // Length (2 bytes, big-endian)
        val lengthBytes = ByteBuffer.allocate(2)
            .order(ByteOrder.BIG_ENDIAN)
            .putShort(payload.size.toShort())
            .array()

        // CRC32 (4 bytes, big-endian)
        val crcBytes = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(crc.toInt())
            .array()

        // Send: length + payload + crc
        val packet = lengthBytes + payload + crcBytes
        connection.send(packet)

        return readModernResponse(cmd, maxResponseSize)
    }

    /**
     * Lê resposta modern (length + payload + crc32)
     */
    private fun readModernResponse(cmd: Byte, maxResponseSize: Int = 2048): ByteArray {
        // Read length (2 bytes, big-endian)
        val lengthBytes = connection.receive(2)
        if (lengthBytes.size < 2) {
            throw IncompleteResponseException("length", 2, lengthBytes.size, cmd)
        }
        Logger.d("SpeeduinoProtocol", "Length bytes: ${lengthBytes.joinToString(" ") { "0x%02X".format(it) }}")

        val length = ByteBuffer.wrap(lengthBytes)
            .order(ByteOrder.BIG_ENDIAN)
            .short
            .toInt() and 0xFFFF

        Logger.d("SpeeduinoProtocol", "Payload length: $length bytes")

        if (length > maxResponseSize) {
            connection.clearInputBuffer()
            throw Exception("Resposta muito grande: $length bytes")
        }

        // Read payload
        val payload = connection.receive(length)
        if (payload.size < length) {
            throw IncompleteResponseException("payload", length, payload.size, cmd)
        }
        Logger.d("SpeeduinoProtocol", "Payload bytes: ${payload.joinToString(" ") { "0x%02X".format(it) }}")

        // Read CRC32 (4 bytes, big-endian)
        val crcBytes = connection.receive(4)
        if (crcBytes.size < 4) {
            throw IncompleteResponseException("crc", 4, crcBytes.size, cmd)
        }
        Logger.d("SpeeduinoProtocol", "CRC bytes: ${crcBytes.joinToString(" ") { "0x%02X".format(it) }}")

        val receivedCrc = ByteBuffer.wrap(crcBytes)
            .order(ByteOrder.BIG_ENDIAN)
            .int
            .toLong() and 0xFFFFFFFF

        val calculatedCrc = calculateCRC32(payload)

        Logger.d("SpeeduinoProtocol", "CRC received: 0x${receivedCrc.toString(16)}, calculated: 0x${calculatedCrc.toString(16)}")

        // ⚠️ IMPORTANTE: Ignorar validação se CRC = 0 (Speeduino não envia CRC em alguns comandos)
        if (receivedCrc != 0L && receivedCrc != calculatedCrc) {
            Logger.w("SpeeduinoProtocol", "CRC mismatch, mas continuando (received=0x${receivedCrc.toString(16)}, calculated=0x${calculatedCrc.toString(16)})")
            // throw Exception("CRC error: received=0x${receivedCrc.toString(16)}, calculated=0x${calculatedCrc.toString(16)}")
        }

        return payload
    }

    class IncompleteResponseException(
        private val stage: String,
        private val expected: Int,
        private val received: Int,
        private val cmd: Byte
    ) : Exception(
        "Incomplete modern response ($stage) for cmd=0x${cmd.toInt().and(0xFF).toString(16)}: expected $expected bytes, received $received"
    )

    /**
     * Grava dados em uma página (comando 'M' - MODERN PROTOCOL)
     *
     * ⚠️ BREAKING CHANGE: Speeduino 202501+ EXIGE Modern Protocol (com CRC wrapper)!
     *
     * Formato: M + pageId(2 bytes) + offset(2 LSB) + length(2 LSB) + [dados]
     * Enviado via Modern Protocol: length + payload + CRC32
     *
     * @param pageNum Número da página (1-15)
     * @param offset Offset inicial
     * @param data Dados a gravar
     */
    suspend fun writePage(pageNum: Byte, offset: Int, data: ByteArray) {
        if (!isModernEnabled()) {
            writePageLegacy(pageNum, offset, data)
            return
        }

        // Criar payload (M + pageId(2 bytes) + offset + length + data)
        val extraPayload = ByteArray(6 + data.size)
        extraPayload[0] = 0x00 // Page identifier high byte (always 0)
        extraPayload[1] = pageNum

        // Offset (2 bytes, little-endian)
        extraPayload[2] = (offset and 0xFF).toByte()           // LSB first
        extraPayload[3] = ((offset shr 8) and 0xFF).toByte()   // MSB second

        // Length (2 bytes, little-endian)
        val length = data.size
        extraPayload[4] = (length and 0xFF).toByte()           // LSB first
        extraPayload[5] = ((length shr 8) and 0xFF).toByte()   // MSB second

        // Data bytes
        data.copyInto(extraPayload, 6)

        Logger.d("SpeeduinoProtocol", "writePage (MODERN): pageNum=${formatPageId(pageNum)}, offset=$offset, length=$length")
        Logger.d("SpeeduinoProtocol", "Payload bytes: ${extraPayload.take(10).joinToString(" ") { "0x%02X".format(it) }}... (${extraPayload.size} total)")

        // Enviar via Modern Protocol (com CRC wrapper)
        val response = sendModernCommand('M'.code.toByte(), extraPayload)

        // Verificar resposta (Speeduino 202501+ retorna resposta)
        if (response.isEmpty()) {
            Logger.w("SpeeduinoProtocol", "⚠️  Write page não retornou resposta (compatibilidade legacy)")
        } else if (response[0] != SERIAL_RC_OK) {
            val responseCode = response[0].toInt() and 0xFF
            val isRangeError = responseCode == (SERIAL_RC_RANGE_ERR.toInt() and 0xFF) || responseCode == 0x80
            val errorMsg = when (response[0]) {
                SERIAL_RC_RANGE_ERR -> "RANGE_ERR (0x84): Valor fora do range ou bins não estão em ordem crescente. Verifique RPM/Load bins!"
                SERIAL_RC_CRC_ERR -> "CRC_ERR (0x82): Erro de CRC"
                SERIAL_RC_UKWN_ERR -> "UKWN_ERR (0x83): Comando desconhecido"
                SERIAL_RC_BUSY_ERR -> "BUSY_ERR (0x85): ECU ocupada"
                else -> if (isRangeError) {
                    "RANGE_ERR (0x${responseCode.toString(16)}): Valor fora do range ou tamanho de página incompatível."
                } else {
                    "response code = 0x${responseCode.toString(16)}"
                }
            }
            throw Exception("Erro ao gravar página ${formatPageId(pageNum)}: $errorMsg")
        } else {
            val responseCode = response[0].toInt() and 0xFF
            Logger.d("SpeeduinoProtocol", "✅ Page ${formatPageId(pageNum)} gravada com resposta (code: 0x${responseCode.toString(16)})")
        }
    }

    /**
     * Grava uma tabela/newserial page via protocolo MS3 ('w').
     */
    suspend fun writeTable(tableId: Byte, offset: Int, data: ByteArray) {
        writeTable(tableId.toInt() and 0xFF, offset, data, EcuFamily.MS3)
    }

    suspend fun writeTable(tableId: Int, offset: Int, data: ByteArray, family: EcuFamily) {
        if (!isModernEnabled()) {
            throw Exception("Modern protocol disabled for table write")
        }

        val response = if (family == EcuFamily.RUSEFI) {
            val payload = ByteArray(6 + data.size)
            payload[0] = (tableId and 0xFF).toByte()
            payload[1] = ((tableId shr 8) and 0xFF).toByte()
            payload[2] = (offset and 0xFF).toByte()
            payload[3] = ((offset shr 8) and 0xFF).toByte()
            payload[4] = (data.size and 0xFF).toByte()
            payload[5] = ((data.size shr 8) and 0xFF).toByte()
            data.copyInto(payload, 6)
            sendModernCommand('C'.code.toByte(), payload)
        } else {
            val payload = ByteArray(6 + data.size)
            payload[0] = 0x00
            payload[1] = (tableId and 0xFF).toByte()
            payload[2] = ((offset shr 8) and 0xFF).toByte()
            payload[3] = (offset and 0xFF).toByte()
            payload[4] = ((data.size shr 8) and 0xFF).toByte()
            payload[5] = (data.size and 0xFF).toByte()
            data.copyInto(payload, 6)
            sendModernCommand('w'.code.toByte(), payload)
        }

        if (response.isEmpty() || response[0] != SERIAL_RC_OK) {
            val responseCode = response.getOrNull(0)?.toInt()?.and(0xFF)
            throw Exception(
                "Erro ao gravar tabela ${formatPageId(tableId)}: " +
                    (responseCode?.let { "response code=0x${it.toString(16)}" } ?: "sem resposta")
            )
        }
    }

    /**
     * Executa burn de uma tabela MS3/newserial ('b').
     */
    suspend fun burnTable(tableId: Byte) {
        burnTable(tableId.toInt() and 0xFF, EcuFamily.MS3)
    }

    suspend fun burnTable(tableId: Int, family: EcuFamily) {
        if (!isModernEnabled()) {
            throw Exception("Modern protocol disabled for table burn")
        }

        val response = if (family == EcuFamily.RUSEFI) {
            val payload = byteArrayOf(
                (tableId and 0xFF).toByte(),
                ((tableId shr 8) and 0xFF).toByte(),
            )
            sendModernCommand('B'.code.toByte(), payload)
        } else {
            val payload = byteArrayOf(0x00, (tableId and 0xFF).toByte())
            sendModernCommand('b'.code.toByte(), payload)
        }
        if (response.isEmpty()) {
            throw Exception("Burn da tabela ${formatPageId(tableId)} sem resposta")
        }

        val responseCode = response[0].toInt() and 0xFF
        if (responseCode != (SERIAL_RC_OK.toInt() and 0xFF) &&
            responseCode != (SERIAL_RC_BURN_OK.toInt() and 0xFF) &&
            responseCode != 0x80) {
            throw Exception(
                "Erro no burn da tabela ${formatPageId(tableId)}: code=0x${responseCode.toString(16)}"
            )
        }
    }

    /**
     * Grava (burn) configurações na EEPROM (comando 'B')
     */
    suspend fun burnConfig() {
        if (!isModernEnabled()) {
            val pageToBurn = lastLegacyWrittenPage
                ?: throw Exception("Burn legacy requer a página gravada anteriormente")
            sendLegacyCommand(
                'b'.code.toByte(),
                payload = byteArrayOf(0x00, pageToBurn),
                expectResponse = false
            )
            Logger.d("SpeeduinoProtocol", "Legacy burn solicitado para page $pageToBurn")
            return
        }

        val response = sendModernCommand('B'.code.toByte(), byteArrayOf())

        // Speeduino pode retornar diferentes códigos de sucesso:
        // - 0x00 (OK) em versões antigas
        // - 0x04 (BURN_OK) em versões recentes
        // - 0x80 (undocumented) em algumas implementações
        if (response.isEmpty()) {
            throw Exception("Burn: nenhuma resposta recebida")
        }

        val responseCode = response[0].toInt() and 0xFF
        Logger.d("SpeeduinoProtocol", "Burn response code: 0x${responseCode.toString(16)}")

        // Aceitar 0x00, 0x04 ou 0x80 como sucesso
        if (responseCode != (SERIAL_RC_OK.toInt() and 0xFF) &&
            responseCode != (SERIAL_RC_BURN_OK.toInt() and 0xFF) &&
            responseCode != 0x80) {
            val errorMsg = when (response[0]) {
                SERIAL_RC_RANGE_ERR -> "RANGE_ERR (0x84): Valor fora do range ou bins nÇœo estÇœo em ordem crescente."
                SERIAL_RC_CRC_ERR -> "CRC_ERR (0x82): Erro de CRC"
                SERIAL_RC_UKWN_ERR -> "UKWN_ERR (0x83): Comando desconhecido"
                SERIAL_RC_BUSY_ERR -> "BUSY_ERR (0x85): ECU ocupada"
                SERIAL_RC_TIMEOUT -> "TIMEOUT (0x80): ECU nÇœo respondeu"
                else -> "response code = 0x${responseCode.toString(16)}"
            }
            throw Exception("Erro ao fazer burn: $errorMsg")
        }

        Logger.d("SpeeduinoProtocol", "✅ Burn executado com sucesso (code: 0x${responseCode.toString(16)})")
    }

    private fun writePageLegacy(pageNum: Byte, offset: Int, data: ByteArray) {
        val pageChar = legacyPageChar(pageNum)
        sendLegacyCommand(CMD_PAGE_SET.code.toByte(), payload = byteArrayOf(pageChar), expectResponse = false)

        val useExtendedOffset = (offset + data.size) > 0xFF
        for (i in data.indices) {
            val valueOffset = offset + i
            val payload = if (useExtendedOffset) {
                byteArrayOf(
                    (valueOffset and 0xFF).toByte(),
                    ((valueOffset shr 8) and 0xFF).toByte(),
                    data[i]
                )
            } else {
                byteArrayOf(
                    (valueOffset and 0xFF).toByte(),
                    data[i]
                )
            }
            sendLegacyCommand(CMD_PAGE_WRITE_LEGACY.code.toByte(), payload = payload, expectResponse = false)
        }

        lastLegacyWrittenPage = pageNum
        Logger.d("SpeeduinoProtocol", "writePage (LEGACY): pageNum=${formatPageId(pageNum)}, offset=$offset, length=${data.size}")
    }

    private fun legacyPageChar(pageNum: Byte): Byte {
        val value = pageNum.toInt() and 0xFF
        return when {
            value in 0..9 -> ('0'.code + value).toByte()
            value in 10..15 -> ('A'.code + (value - 10)).toByte()
            else -> '0'.code.toByte()
        }
    }

    /**
     * Calcula CRC32 checksum
     */
    private fun calculateCRC32(data: ByteArray): Long {
        val crc32 = CRC32()
        crc32.update(data)
        return crc32.value
    }
}

/**
 * Capacidades seriais do Speeduino
 */
data class SerialCapability(
    val protocolVersion: Int,
    val blockingFactor: Int,
    val tableBlockingFactor: Int
)
