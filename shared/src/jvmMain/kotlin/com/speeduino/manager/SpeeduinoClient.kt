package com.speeduino.manager

import com.speeduino.manager.shared.Logger
import com.speeduino.manager.connection.ISpeeduinoConnection
import com.speeduino.manager.model.FirmwareEra
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.Algorithm
import com.speeduino.manager.model.OutputField
import com.speeduino.manager.model.SpeeduinoOutputChannels
import com.speeduino.manager.model.SpeeduinoTableDefinitions
import com.speeduino.manager.model.TableDefinitions
import com.speeduino.manager.model.TableValidator
import com.speeduino.manager.model.UnsupportedFirmwareException
import com.speeduino.manager.model.ValidationException
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.protocol.SerialCapability
import com.speeduino.manager.protocol.SpeeduinoProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cliente principal para comunicação com Speeduino ECU
 *
 * Fachada de alto nível que combina:
 * - Camada de conexão (TCP, Bluetooth, USB, etc)
 * - Camada de protocolo (comandos Legacy e Modern)
 * - Stream contínuo de dados em tempo real
 *
 * Exemplo de uso:
 * ```
 * val connection = SpeeduinoTcpConnection("10.0.2.2", 5555)
 * val client = SpeeduinoClient(connection, onDataReceived, onConnectionStateChanged, onError)
 *
 * client.connect()
 * client.startLiveDataStream()
 * ```
 */
class SpeeduinoClient(
    private val connection: ISpeeduinoConnection,
    private val onDataReceived: (SpeeduinoLiveData) -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "SpeeduinoClient"
    }

    private val protocol = SpeeduinoProtocol(connection)

    private var _isStreaming = false
    private var streamJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Firmware info and table definitions (set after connect)
    private var firmwareInfo: FirmwareInfo? = null
    private var tableDefinitions: TableDefinitions? = null
    private var outputChannelFields: List<OutputField>? = null
    private var outputChannelBlockSize: Int? = null
    private var cachedEngineConstants: EngineConstants? = null
    private val connectMutex = Mutex()

    init {
        connection.setOnConnectionStateChanged(onConnectionStateChanged)
        connection.setOnError(onError)
    }

    /**
     * Conecta ao Speeduino e valida firmware
     *
     * @throws UnsupportedFirmwareException se firmware não for suportado
     */
    suspend fun connect() {
        connectMutex.withLock {
            if (connection.isConnected()) {
                Logger.d(TAG, "Conexao ja estabelecida, ignorando novo connect")
                return
            }

            // 1. Estabelecer conexão física
            connection.connect()

            // 2. Obter informações do firmware
            val firmwareSignature = protocol.getFirmwareInfo()
            val productString = try {
                protocol.getProductString()
            } catch (e: Exception) {
                Logger.w(TAG, "Não foi possível obter product string: ${e.message}")
                "Unknown"
            }

            Logger.d(TAG, "Firmware detectado: $firmwareSignature")
            Logger.d(TAG, "Product string: $productString")

            // 3. Validar compatibilidade e carregar definitions
            try {
                val definitions = SpeeduinoTableDefinitions.getDefinitions(firmwareSignature)
                val era = SpeeduinoTableDefinitions.detectFirmwareEra(firmwareSignature)

                // Armazenar informações
                firmwareInfo = FirmwareInfo(
                    signature = firmwareSignature,
                    productString = productString,
                    era = era
                )
                tableDefinitions = definitions

                // 4. Detect output channels block size and load field definitions
                val blockSize = detectOutputChannelsBlockSize(firmwareSignature, era)
                outputChannelBlockSize = blockSize
                outputChannelFields = SpeeduinoOutputChannels.getDefinition(blockSize)

                Logger.i(TAG, "✅ Firmware compatível: $firmwareSignature (era: $era)")
                Logger.i(TAG, "✅ Table definitions carregadas: VE=Page${definitions.veTable.page}, Ignition=Page${definitions.ignitionTable.page}")
                Logger.i(TAG, "✅ Output channels: $blockSize bytes, ${outputChannelFields?.size} fields")

            } catch (e: UnsupportedFirmwareException) {
                // Desconectar se firmware incompatível
                connection.disconnect()

                val supportedVersions = SpeeduinoTableDefinitions.getSupportedVersions()
                val errorMessage = """
                ❌ Firmware não suportado: $firmwareSignature

                Versões suportadas:
                ${supportedVersions.joinToString("\n") { "  • $it" }}

                Por favor, atualize o firmware da ECU ou entre em contato.
            """.trimIndent()

                Logger.e(TAG, errorMessage)
                throw UnsupportedFirmwareException(errorMessage)
            }
        }
    }

    /**
     * Obtém informações do firmware conectado
     * @return FirmwareInfo ou null se não conectado
     */
    fun getFirmwareInfoCached(): FirmwareInfo? = firmwareInfo

    /**
     * Obtém table definitions carregadas
     * @return TableDefinitions ou null se não conectado
     */
    fun getTableDefinitions(): TableDefinitions? = tableDefinitions

    /**
     * Obtém output channel fields carregados
     * @return List<OutputField> ou null se não conectado
     */
    fun getOutputChannelFields(): List<OutputField>? = outputChannelFields

    /**
     * Detect output channels block size based on firmware version
     *
     * Block size evolution:
     * - Legacy (2016): 35 bytes
     * - Modern 2020: 114 bytes
     * - Modern 2024+: 130 bytes
     *
     * @param firmwareSignature Firmware signature (e.g., "speeduino 202402")
     * @param era Firmware era (LEGACY or MODERN)
     * @return Block size in bytes
     */
    private fun detectOutputChannelsBlockSize(firmwareSignature: String, era: FirmwareEra): Int {
        // Extract version number from signature
        val versionMatch = Regex("""speeduino\s+(\d{6})""").find(firmwareSignature)
        val versionNumber = versionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return when {
            // Modern 2024+ (202402 and later) → 130 bytes
            versionNumber >= 202402 -> 130

            // Modern 2020-2023 (202001 to 202401) → 114 bytes
            versionNumber >= 202001 -> 114

            // Legacy (2016-2019) → 35 bytes
            else -> 35
        }
    }

    /**
     * Desconecta do Speeduino
     */
    fun disconnect() {
        stopLiveDataStream()
        connection.disconnect()
        // NÃO cancelar o scope - apenas o job do stream
        // Isso permite reconectar e reiniciar o stream
    }

    /**
     * Verifica se está conectado
     */
    fun isConnected(): Boolean = connection.isConnected()

    /**
     * Verifica se o stream de dados está ativo
     */
    fun isStreaming(): Boolean = _isStreaming

    /**
     * Obtém informações da conexão
     */
    fun getConnectionInfo(): String = connection.getConnectionInfo()

    // ==================== Protocol Commands ====================

    /**
     * Obtém informações do firmware (comando 'Q')
     */
    suspend fun getFirmwareInfo(): String {
        return protocol.getFirmwareInfo()
    }

    /**
     * Obtém string do produto (comando 'S')
     */
    suspend fun getProductString(): String {
        return protocol.getProductString()
    }

    /**
     * Obtém capacidades seriais (comando 'f')
     */
    suspend fun getSerialCapability(): SerialCapability {
        return protocol.getSerialCapability()
    }

    /**
     * Lê CRC32 de uma página (comando 'd')
     */
    suspend fun getPageCRC(pageNum: Byte): Long {
        return protocol.getPageCRC(pageNum)
    }

    /**
     * Lê uma página completa (comando 'p')
     * @param pageNum Número da página (0-15)
     * @param offset Offset inicial
     * @param length Tamanho a ler
     */
    suspend fun readPage(pageNum: Byte, offset: Int, length: Int): ByteArray {
        return protocol.readPage(pageNum, offset, length)
    }

    /**
     * Lê página completa em blocos (para páginas grandes)
     */
    suspend fun readFullPage(pageNum: Byte, pageSize: Int, blockSize: Int): ByteArray = withContext(Dispatchers.IO) {
        val pageData = ByteArray(pageSize)
        var offset = 0

        while (offset < pageSize) {
            val chunkSize = minOf(blockSize, pageSize - offset)
            val chunk = readPage(pageNum, offset, chunkSize)

            chunk.copyInto(pageData, offset)
            offset += chunkSize

            // Small delay to avoid overwhelming the ECU
            delay(10)
        }

        pageData
    }

    /**
     * Lê Engine Constants (Page 1 - 128 bytes)
     */
    suspend fun readEngineConstants(): EngineConstants {
        Logger.d(TAG, "Lendo Engine Constants (Page 1)...")
        val pageData = readPage(pageNum = 1, offset = 0, length = 128)
        Logger.d(TAG, "Page 1 recebida: ${pageData.size} bytes")
        val constants = EngineConstants.fromPage1(pageData)
        cachedEngineConstants = constants
        return constants
    }

    /**
     * Lê Trigger Settings (Page 4 - 128 bytes)
     */
    suspend fun readTriggerSettings(): TriggerSettings {
        Logger.d(TAG, "Lendo Trigger Settings (Page 4)...")
        val pageData = readPage(
            pageNum = TriggerSettings.PAGE_NUMBER.toByte(),
            offset = 0,
            length = TriggerSettings.PAGE_LENGTH
        )
        Logger.d(TAG, "Trigger Settings recebidos: ${pageData.size} bytes")
        return TriggerSettings.fromPageData(pageData)
    }

    /**
     * Grava Trigger Settings (Page 4) + Burn
     */
    suspend fun writeTriggerSettings(settings: TriggerSettings, burn: Boolean = true) {
        Logger.d(TAG, "Gravando Trigger Settings (Page 4)...")
        val basePage = readPage(
            pageNum = TriggerSettings.PAGE_NUMBER.toByte(),
            offset = 0,
            length = TriggerSettings.PAGE_LENGTH
        )
        val updatedData = settings.toPageData(basePage)
        protocol.writePage(
            pageNum = TriggerSettings.PAGE_NUMBER.toByte(),
            offset = 0,
            data = updatedData
        )
        if (burn) {
            delay(300)
            protocol.burnConfig()
            Logger.d(TAG, "Trigger Settings gravados e burn executado")
        } else {
            Logger.d(TAG, "Trigger Settings gravados (sem burn)")
        }
    }

    /**
     * Lê VE Table usando definitions dinâmicas
     *
     * IMPORTANT: Uses dynamic page number based on firmware version!
     * - Legacy (2016): Page 1
     * - Modern (2020+): Page 2
     *
     * Format:
     * - Modern firmware (288 bytes): VE values first, axes stored as single-byte bins
     * - Legacy firmware (304 bytes): RPM bins (U16) + load bins precede the table
     *
     * @throws IllegalStateException if not connected
     */
    suspend fun readVeTable(): VeTable {
        val defs = tableDefinitions
            ?: throw IllegalStateException("Not connected! Call connect() first.")

        val metadata = defs.veTable

        Logger.d(TAG, "Lendo VE Table (Page ${metadata.page}, offset ${metadata.offset}, ${metadata.totalSize} bytes)...")
        val pageData = readPage(
            pageNum = metadata.page.toByte(),
            offset = metadata.offset,
            length = metadata.totalSize
        )
        Logger.d(TAG, "VE Table recebida: ${pageData.size} bytes")

        val storageFormat = VeTable.StorageFormat.fromTotalSize(metadata.totalSize)
        val loadType = if (isMapLoad()) VeTable.LoadType.MAP else VeTable.LoadType.TPS
        return VeTable.fromPageData(pageData, storageFormat, loadType)
    }

    /**
     * Lê Ignition Table usando definitions dinâmicas
     *
     * IMPORTANT: Uses dynamic page number based on firmware version!
     * - All versions: Page 3 (stable across all versions)
     *
     * Format:
     * - Modern firmware (288 bytes): table first, axes compacted em 1 byte
     * - Legacy firmware (304 bytes): axes antes da tabela, com RPM em U16
     *
     * @throws IllegalStateException if not connected
     */
    suspend fun readIgnitionTable(): IgnitionTable {
        val defs = tableDefinitions
            ?: throw IllegalStateException("Not connected! Call connect() first.")

        val metadata = defs.ignitionTable

        Logger.d(TAG, "Lendo Ignition Table (Page ${metadata.page}, offset ${metadata.offset}, ${metadata.totalSize} bytes)...")
        val pageData = readPage(
            pageNum = metadata.page.toByte(),
            offset = metadata.offset,
            length = metadata.totalSize
        )
        Logger.d(TAG, "Ignition Table recebida: ${pageData.size} bytes")

        val storageFormat = IgnitionTable.StorageFormat.fromTotalSize(metadata.totalSize)
        val loadType = if (isMapLoad()) IgnitionTable.LoadType.MAP else IgnitionTable.LoadType.TPS
        return IgnitionTable.fromPageData(pageData, storageFormat, loadType)
    }

    /**
     * Grava Engine Constants (Page 1 - 128 bytes) + Burn
     */
    suspend fun writeEngineConstants(engineConstants: EngineConstants) {
        Logger.d(TAG, "Gravando Engine Constants (Page 1)...")

        // Preserve existing settings on Page 1 and update only known fields
        val basePage = readPage(pageNum = 1, offset = 0, length = 128)
        val pageData = engineConstants.applyToPage1(basePage)
        Logger.d(TAG, "Page 1 serializada: ${pageData.size} bytes")

        // Write to ECU
        protocol.writePage(pageNum = 1, offset = 0, data = pageData)
        Logger.d(TAG, "Page 1 gravada com sucesso")

        // Burn to EEPROM
        protocol.burnConfig()
        Logger.d(TAG, "Burn executado com sucesso")
        cachedEngineConstants = engineConstants
    }

    /**
     * Grava uma página completa de configuração (backup/restore).
     */
    suspend fun writeRawPage(pageNum: Byte, data: ByteArray) {
        protocol.writePage(pageNum = pageNum, offset = 0, data = data)
        protocol.burnConfig()
        Logger.d(TAG, "Página $pageNum gravada via backup e burn executado")
    }

    /**
     * Grava uma página completa sem executar burn (para restauração em lote).
     */
    suspend fun writeRawPageWithoutBurn(pageNum: Byte, data: ByteArray) {
        protocol.writePage(pageNum = pageNum, offset = 0, data = data)
        Logger.d(TAG, "Página $pageNum gravada via backup (sem burn)")
    }

    /**
     * Executa burn após gravações em lote.
     */
    suspend fun burnConfigs() {
        protocol.burnConfig()
        Logger.d(TAG, "Burn executado após restauração em lote")
    }

    /**
     * Grava VE Table (Page 1 - offset 0, 304 bytes) + Burn
     *
     * IMPORTANT: This method validates the table before writing.
     * If validation fails, it throws ValidationException.
     *
     * @param veTable VE Table to write
     * @throws ValidationException if table has critical errors
     */
    suspend fun writeVeTable(veTable: VeTable) {
        Logger.d(TAG, "Gravando VE Table (Page 1)...")

        // 1. CRITICAL: Validate table before writing
        val defs = tableDefinitions
            ?: throw IllegalStateException("Not connected! Call connect() first.")

        val validator = TableValidator(defs.veTable)
        val validationResult = validator.validateBeforeWrite(veTable)

        if (!validationResult.isValid) {
            Logger.e(TAG, "❌ VE Table validation FAILED!")
            throw ValidationException(validationResult)
        }

        if (validationResult.warnings.isNotEmpty()) {
            Logger.w(TAG, "⚠️  VE Table has ${validationResult.warnings.size} warnings - proceeding anyway")
        }

        val storageFormat = VeTable.StorageFormat.fromTotalSize(defs.veTable.totalSize)

        // 2. Convert model to bytes usando formato correto
        val pageData = if (storageFormat != null) {
            veTable.toByteArray(storageFormat)
        } else {
            veTable.toByteArray()
        }
        Logger.d(TAG, "VE Table serializada: ${pageData.size} bytes")

        // 3. Write to ECU using dynamic page number (fire-and-forget, não aguarda resposta)
        protocol.writePage(
            pageNum = defs.veTable.page.toByte(),
            offset = defs.veTable.offset,
            data = pageData
        )
        Logger.d(TAG, "VE Table enviada para Page ${defs.veTable.page}")

        // ⚠️ CRÍTICO: Delay MAIOR para Speeduino processar write completo
        // Write page é assíncrono - 304 bytes levam tempo para gravar na RAM
        // Delay conservador: ~3ms por byte @ 115200 baud = ~900ms + margem
        delay(1000) // 1 segundo de delay (conservador)
        Logger.d(TAG, "Aguardou 1s para processamento do write")

        // Burn to EEPROM (também demora - grava na flash/EEPROM)
        protocol.burnConfig()
        Logger.d(TAG, "✅ Burn executado com sucesso!")
    }

    /**
     * Grava Ignition Table (Page 3 - offset 0, 304 bytes) + Burn
     *
     * CRITICAL: This method validates the table before writing.
     * Dangerous ignition advance values (>45°) will cause validation to FAIL.
     *
     * @param ignitionTable Ignition Table to write
     * @throws ValidationException if table has critical errors (esp. dangerous advance)
     */
    suspend fun writeIgnitionTable(ignitionTable: IgnitionTable) {
        Logger.d(TAG, "Gravando Ignition Table (Page 3)...")

        // 1. CRITICAL: Validate table before writing (prevent engine damage!)
        val defs = tableDefinitions
            ?: throw IllegalStateException("Not connected! Call connect() first.")

        val validator = TableValidator(defs.ignitionTable)
        val validationResult = validator.validateBeforeWrite(ignitionTable)

        if (!validationResult.isValid) {
            Logger.e(TAG, "❌ Ignition Table validation FAILED!")
            Logger.e(TAG, "🚨 NOT writing to ECU - dangerous ignition advance detected!")
            throw ValidationException(validationResult)
        }

        if (validationResult.warnings.isNotEmpty()) {
            Logger.w(TAG, "⚠️  Ignition Table has ${validationResult.warnings.size} warnings")
            validationResult.warnings.forEach { Logger.w(TAG, "    - $it") }
            Logger.w(TAG, "⚠️  Proceeding with write - MONITOR ENGINE FOR KNOCK!")
        }

        val storageFormat = IgnitionTable.StorageFormat.fromTotalSize(defs.ignitionTable.totalSize)

        // 2. Convert model to bytes usando formato correto
        val pageData = if (storageFormat != null) {
            ignitionTable.toByteArray(storageFormat)
        } else {
            ignitionTable.toByteArray()
        }
        Logger.d(TAG, "Ignition Table serializada: ${pageData.size} bytes")

        // 3. Write to ECU (fire-and-forget, não aguarda resposta)
        protocol.writePage(pageNum = defs.ignitionTable.page.toByte(), offset = 0, data = pageData)
        Logger.d(TAG, "Ignition Table enviada")

        // 4. Delay para processar write
        delay(1000)
        Logger.d(TAG, "Aguardou 1s para processamento do write")

        // 5. Burn to EEPROM
        protocol.burnConfig()
        Logger.d(TAG, "✅ Burn executado com sucesso!")
    }

    /**
     * Lê AFR Target Table usando definitions dinâmicas
     *
     * IMPORTANT: Uses dynamic page number based on firmware version!
     * - All versions: Page 5 (stable across all versions)
     *
     * Format:
     * - Modern firmware (288 bytes): valores primeiro, eixos compactados
     * - Legacy firmware (304 bytes): eixos de 16 bits antes dos valores
     *
     * @throws IllegalStateException if not connected
     */
    suspend fun readAfrTable(): AfrTable {
        val defs = tableDefinitions
            ?: throw IllegalStateException("Not connected! Call connect() first.")

        val metadata = defs.afrTable

        Logger.d(TAG, "Lendo AFR Table (Page ${metadata.page}, offset ${metadata.offset}, ${metadata.totalSize} bytes)...")
        val pageData = readPage(
            pageNum = metadata.page.toByte(),
            offset = metadata.offset,
            length = metadata.totalSize
        )
        Logger.d(TAG, "AFR Table recebida: ${pageData.size} bytes")

        val storageFormat = AfrTable.StorageFormat.fromTotalSize(metadata.totalSize)
        val loadType = if (isMapLoad()) AfrTable.LoadType.MAP else AfrTable.LoadType.TPS
        return AfrTable.fromPageData(pageData, storageFormat, loadType)
    }

    /**
     * Grava AFR Target Table (Page 5 - offset 0, 304 bytes) + Burn
     *
     * Note: AFR Table doesn't require as strict validation as Ignition,
     * but extreme values (too rich/lean) will generate warnings.
     *
     * @param afrTable AFR Target Table to write
     * @throws IllegalStateException if not connected
     */
    suspend fun writeAfrTable(afrTable: AfrTable) {
        val defs = tableDefinitions
            ?: throw IllegalStateException("Not connected! Call connect() first.")

        val metadata = defs.afrTable

        Logger.d(TAG, "Gravando AFR Table (Page ${metadata.page})...")

        val storageFormat = AfrTable.StorageFormat.fromTotalSize(metadata.totalSize)

        // Convert model to bytes (respecting firmware layout)
        val pageData = if (storageFormat != null) {
            afrTable.toByteArray(storageFormat)
        } else {
            afrTable.toByteArray()
        }
        Logger.d(TAG, "AFR Table serializada: ${pageData.size} bytes")

        // Write to ECU using dynamic page number
        protocol.writePage(
            pageNum = metadata.page.toByte(),
            offset = metadata.offset,
            data = pageData
        )
        Logger.d(TAG, "AFR Table enviada para Page ${metadata.page}")

        // Delay para processar write
        delay(1000)
        Logger.d(TAG, "Aguardou 1s para processamento do write")

        // Burn to EEPROM
        protocol.burnConfig()
        Logger.d(TAG, "✅ Burn executado com sucesso!")
    }

    /**
     * Lê dados em tempo real
     * Tenta Modern Protocol ('n') primeiro, fallback para Legacy ('A') se falhar
     */
    suspend fun readLiveData(): SpeeduinoLiveData = withContext(Dispatchers.IO) {
        var usedModern = false
        val data = if (connection.supportsModernProtocol()) {
            try {
                // ✅ Tenta Modern Protocol primeiro (Speeduino 202402+)
                usedModern = true
                protocol.readLiveDataModern(outputChannelBlockSize ?: 127)
            } catch (e: Exception) {
                // ⚠️ Se timeout ou conexão perdida, NÃO tentar fallback
                if (!connection.isConnected()) {
                    Logger.e(TAG, "Conexão perdida durante leitura de live data")
                    throw Exception("Não conectado")
                }

                Logger.w(TAG, "Modern Protocol falhou, tentando Legacy: ${e.message}")
                // ⚠️ Fallback para Legacy Protocol (versões antigas)
                usedModern = false
                protocol.readLiveData()
            }
        } else {
            protocol.readLiveData()
        }

        if (usedModern) {
            parseModernLiveData(data)
        } else {
            SpeeduinoLiveDataParser.fromLegacyFrame(data)
        }
    }

    // ==================== Live Data Streaming ====================

    /**
     * Inicia stream contínuo de dados em tempo real
     */
    fun startLiveDataStream(intervalMs: Long = 100) {
        if (_isStreaming) {
            Logger.w(TAG, "Stream já está ativo, ignorando nova requisição")
            return
        }

        Logger.d(TAG, "Iniciando live data stream (intervalo: ${intervalMs}ms)")
        _isStreaming = true

        // Cancelar job anterior se existir
        streamJob?.cancel()

        streamJob = scope.launch {
            var packetCount = 0
            while (_isStreaming && connection.isConnected()) {
                try {
                    val liveData = readLiveData()
                    onDataReceived(liveData)
                    packetCount++

                    // Log a cada 50 pacotes (~5 segundos com intervalo de 100ms)
                    if (packetCount % 50 == 0) {
                        Logger.d(TAG, "Stream ativo: $packetCount pacotes recebidos (RPM: ${liveData.rpm})")
                    }

                    delay(intervalMs)
                } catch (e: Exception) {
                    if (_isStreaming) { // Only report error if still streaming
                        Logger.e(TAG, "Erro no stream: ${e.message}", e)
                        onError("Erro no stream: ${e.message}")

                        if (!connection.isConnected()) {
                            Logger.w(TAG, "🔴 Conexão perdida detectada durante stream")
                        }
                    }
                    break
                }
            }
            Logger.d(TAG, "Stream finalizado (total: $packetCount pacotes)")
            _isStreaming = false
        }
    }

    /**
     * Para stream de dados
     */
    fun stopLiveDataStream() {
        Logger.d(TAG, "Parando live data stream...")
        _isStreaming = false
        streamJob?.cancel()
        streamJob = null
    }

    private suspend fun isMapLoad(): Boolean {
        val constants = cachedEngineConstants ?: run {
            try {
                readEngineConstants()
            } catch (e: Exception) {
                Logger.w(TAG, "NÇœo foi possÇðvel ler Engine Constants para detectar loadType: ${e.message}")
                null
            }
        }
        return constants?.algorithm != Algorithm.ALPHA_N
    }

    /**
     * Pausa stream aguardando o ciclo atual finalizar para evitar respostas pendentes.
     */
    suspend fun pauseLiveDataStream(timeoutMs: Long = 1500) {
        if (!_isStreaming) {
            return
        }

        Logger.d(TAG, "Pausando live data stream...")
        _isStreaming = false

        val job = streamJob
        if (job != null) {
            val finished = withTimeoutOrNull(timeoutMs) {
                job.join()
                true
            } ?: false

            if (!finished) {
                Logger.w(TAG, "Timeout aguardando stream, cancelando job")
                job.cancel()
            }
        }

        streamJob = null
    }

    // ==================== Data Parsing ====================

    /**
     * Parse live data packet (127 bytes)
     * Based on Speeduino logger.cpp getTSLogEntry function
     */
    private fun parseLiveData(data: ByteArray): SpeeduinoLiveData {
        // There's a 1-byte offset - RPM is at positions 15-16, not 14-15
        val rpmLsb = data[15].toInt() and 0xFF
        val rpmMsb = data[16].toInt() and 0xFF
        val rpm = rpmLsb or (rpmMsb shl 8)


        return SpeeduinoLiveData(
            secl = data[1].toInt() and 0xFF,  // Also offset by 1
            rpm = rpm,
            coolantTemp = (data[8].toInt() and 0xFF) - 40,  // Offset by 1
            intakeTemp = (data[7].toInt() and 0xFF) - 40,   // Offset by 1
            mapPressure = ((data[6].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF),  // Offset by 1
            tps = data[26].toInt() and 0xFF,  // Offset by 1
            batteryVoltage = (data[10].toInt() and 0xFF) / 10.0,  // Offset by 1
            advance = data[25].toInt() and 0xFF,  // Offset by 1
            o2 = data[11].toInt() and 0xFF,  // Offset by 1
            engineStatus = data[3].toInt() and 0xFF,  // Offset by 1
            sparkStatus = data[33].toInt() and 0xFF  // Offset by 1
        )
    }

    private fun parseModernLiveData(data: ByteArray): SpeeduinoLiveData {
        val parsedOutputChannels = SpeeduinoLiveDataParser.fromOutputChannels(data)
        val parsedLegacy = SpeeduinoLiveDataParser.fromLegacyFrame(data)

        val outputScore = liveDataPlausibilityScore(parsedOutputChannels)
        val legacyScore = liveDataPlausibilityScore(parsedLegacy)

        return when {
            outputScore > legacyScore -> parsedOutputChannels
            legacyScore > outputScore -> parsedLegacy
            else -> {
                val expectedBlockSize = outputChannelBlockSize
                if (expectedBlockSize != null && data.size == expectedBlockSize) {
                    parsedOutputChannels
                } else {
                    parsedLegacy
                }
            }
        }
    }

    private fun liveDataPlausibilityScore(liveData: SpeeduinoLiveData): Int {
        var score = 0

        if (liveData.rpm in 0..15000) score++
        if (liveData.mapPressure in 10..300) score++
        if (liveData.tps in 0..100) score++
        if (liveData.batteryVoltage in 5.0..20.0) score++
        if (liveData.coolantTemp in -40..200) score++
        if (liveData.intakeTemp in -40..200) score++

        return score
    }
}

/**
 * Data class para dados em tempo real do Speeduino
 */
data class FirmwareInfo(
    val signature: String,
    val productString: String,
    val era: FirmwareEra
)
