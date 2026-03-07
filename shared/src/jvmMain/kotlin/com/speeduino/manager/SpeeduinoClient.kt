package com.speeduino.manager

import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.shared.Logger
import com.speeduino.manager.connection.ConnectionTrace
import com.speeduino.manager.connection.ISpeeduinoConnection
import com.speeduino.manager.model.FirmwareEra
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.Algorithm
import com.speeduino.manager.model.EcuCapabilities
import com.speeduino.manager.model.EcuDefinition
import com.speeduino.manager.model.EcuDefinitionRegistry
import com.speeduino.manager.model.EcuFamily
import com.speeduino.manager.model.OutputField
import com.speeduino.manager.model.SpeeduinoOutputChannels
import com.speeduino.manager.model.SpeeduinoTableDefinitions
import com.speeduino.manager.model.EngineProtectionConfig
import com.speeduino.manager.model.EngineProtectionMapper
import com.speeduino.manager.model.MegaSpeedIniTableDefinitions
import com.speeduino.manager.model.Ms2TableDefinitions
import com.speeduino.manager.model.PinLayoutDetector
import com.speeduino.manager.model.PinLayoutInfo
import com.speeduino.manager.model.TableDefinitions
import com.speeduino.manager.model.TableValidator
import com.speeduino.manager.model.UnsupportedFirmwareException
import com.speeduino.manager.model.ValidationException
import com.speeduino.manager.model.SecondarySerialConfig
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.Ms3TableDefinitions
import com.speeduino.manager.model.PressureCalibration
import com.speeduino.manager.model.RusefiF407DiscoveryDefinitions
import com.speeduino.manager.model.RusefiIniTableDefinitions
import com.speeduino.manager.model.RusefiTableDefinitions
import com.speeduino.manager.model.RusefiInputOutputConfig
import com.speeduino.manager.model.RusefiInputOutputSnapshot
import com.speeduino.manager.model.RusefiIniUiParsers
import com.speeduino.manager.model.SpeeduinoIniDefinitions
import com.speeduino.manager.model.TpsCalibration
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.protocol.SerialCapability
import com.speeduino.manager.protocol.SpeeduinoProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

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
        private const val LIVE_DATA_FAULT_REPORT_INTERVAL_MS = 30_000L
        private const val LIVE_STREAM_RECOVERABLE_TIMEOUT_LIMIT = 3
        private val PARTIAL_TIMEOUT_REGEX = Regex("""Timeout: expected (\d+) bytes, received (\d+)""")
    }

    private val protocol = SpeeduinoProtocol(connection)

    private var _isStreaming = false
    private var streamJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Firmware info and table definitions (set after connect)
    private var firmwareInfo: FirmwareInfo? = null
    private var ecuDefinition: EcuDefinition? = null
    private var tableDefinitions: TableDefinitions? = null
    private var outputChannelFields: List<OutputField>? = null
    private var activeIniDefinition: IniDefinition? = null
    private var speeduinoIniCatalog: SpeeduinoIniDefinitions.Catalog? = null
    private var megaSpeedIniCatalog: MegaSpeedIniTableDefinitions.Catalog? = null
    private var rusefiIniCatalog: RusefiIniTableDefinitions.Catalog? = null
    private var cachedEngineConstants: EngineConstants? = null
    private var pinLayoutInfo: PinLayoutInfo? = null
    private val connectMutex = Mutex()
    private var liveDataSampleCounter = 0
    private var manualFirmwareProfile: String? = null
    private var readOnlySafeModeEnabled = false

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
            // Legacy serial (USB/BT/desktop serial) fica mais estável com handshake simples
            // como no fluxo antigo (uma assinatura válida já é suficiente).
            val useLegacyHandshakeCore = shouldUseLegacyHandshakeCore()
            val firmwareSamples: List<String>
            val firmwareConsensus: FirmwareConsensus?
            val detectedFirmwareSignature = if (useLegacyHandshakeCore) {
                val raw = protocol.getFirmwareInfoLegacyStrict()
                val sanitized = sanitizeFirmwareSignature(raw).ifBlank { "Unknown" }
                firmwareSamples = listOf(sanitized)
                firmwareConsensus = null
                normalizeFirmwareSignature(sanitized) ?: sanitized
            } else {
                firmwareSamples = readFirmwareSignatureSamples()
                firmwareConsensus = resolveFirmwareConsensus(firmwareSamples)
                firmwareConsensus.signature
                    ?: firmwareSamples.lastOrNull()
                    ?: "Unknown"
            }
            val effectiveFirmwareSignature = manualFirmwareProfile ?: detectedFirmwareSignature

            val productString = try {
                protocol.getProductString(effectiveFirmwareSignature)
            } catch (e: Exception) {
                Logger.w(TAG, "Não foi possível obter product string: ${e.message}")
                "Unknown"
            }

            Logger.d(TAG, "Firmware detectado: $detectedFirmwareSignature")
            Logger.d(TAG, "Product string: $productString")
            if (useLegacyHandshakeCore) {
                Logger.d(TAG, "Legacy handshake core ativo para transporte sem modern protocol")
            }
            if (manualFirmwareProfile != null) {
                Logger.w(
                    TAG,
                    "⚠️ Usando perfil manual de firmware: $manualFirmwareProfile (safe mode read-only)"
                )
            }

            // 3. Validar compatibilidade e carregar definitions
            try {
                if (manualFirmwareProfile == null && firmwareConsensus != null) {
                    validateFirmwareConsensus(firmwareConsensus, firmwareSamples)
                }
                val resolvedDefinition = EcuDefinitionRegistry.resolve(
                    signature = effectiveFirmwareSignature,
                    productString = productString
                )
                val definitions = resolvedDefinition.tableDefinitions
                val era = when (resolvedDefinition.family) {
                    EcuFamily.SPEEDUINO -> SpeeduinoTableDefinitions.detectFirmwareEra(effectiveFirmwareSignature)
                    EcuFamily.MS2 -> FirmwareEra.MS2
                    EcuFamily.MEGASPEED -> FirmwareEra.MS2
                    EcuFamily.MS3 -> FirmwareEra.MS3
                    EcuFamily.RUSEFI -> FirmwareEra.RUSEFI
                    else -> throw UnsupportedFirmwareException("Unsupported ECU family: ${resolvedDefinition.family}")
                }

                readOnlySafeModeEnabled = manualFirmwareProfile != null

                // Armazenar informações
                ecuDefinition = resolvedDefinition
                firmwareInfo = FirmwareInfo(
                    signature = effectiveFirmwareSignature,
                    productString = productString,
                    era = era,
                    family = resolvedDefinition.family,
                    capabilities = resolvedDefinition.capabilities
                )
                tableDefinitions = definitions

                // 4. Detect output channels block size and load field definitions
                outputChannelFields = definitions?.let { loadedDefinitions ->
                    SpeeduinoOutputChannels.getDefinition(loadedDefinitions.ochBlockSize)
                }

                Logger.i(TAG, "✅ Firmware compatível: $effectiveFirmwareSignature (era: $era)")
                if (definitions != null) {
                    Logger.i(TAG, "✅ Table definitions carregadas: VE=Page${definitions.veTable.page}, Ignition=Page${definitions.ignitionTable.page}")
                    Logger.i(TAG, "✅ Output channels: ${definitions.ochBlockSize} bytes, ${outputChannelFields?.size} fields")
                } else {
                    Logger.i(TAG, "ℹ️ ECU ${resolvedDefinition.family} conectada sem definitions de tuning nesta PR")
                }
                if (readOnlySafeModeEnabled) {
                    Logger.w(TAG, "⚠️ Safe mode read-only habilitado (perfil manual)")
                }

            } catch (e: UnsupportedFirmwareException) {
                // Desconectar se firmware incompatível
                connection.disconnect()
                readOnlySafeModeEnabled = false

                val originalMessage = e.message.orEmpty()
                val normalizedMessage = originalMessage.lowercase(Locale.US)
                val isChannelQualityError =
                    "assinatura de firmware" in normalizedMessage ||
                    "firmware signature" in normalizedMessage
                if (isChannelQualityError) {
                    Logger.e(TAG, originalMessage)
                    throw e
                }

                val supportedVersions = EcuDefinitionRegistry.getSupportedFamilies()
                val errorMessage = """
                ❌ Firmware não suportado: $effectiveFirmwareSignature

                Versões suportadas:
                ${supportedVersions.joinToString("\n") { "  • $it" }}

                Por favor, atualize o firmware da ECU ou entre em contato.
            """.trimIndent()

                Logger.e(TAG, errorMessage)
                throw UnsupportedFirmwareException(errorMessage)
            }
        }
    }

    private fun shouldUseLegacyHandshakeCore(): Boolean {
        return !connection.supportsModernProtocol()
    }

    private data class FirmwareConsensus(
        val signature: String?,
        val consensusHits: Int
    )

    private suspend fun readFirmwareSignatureSamples(maxAttempts: Int = 3): List<String> {
        val samples = mutableListOf<String>()
        var lastError: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                val raw = protocol.getFirmwareInfo()
                val sanitized = sanitizeFirmwareSignature(raw)
                if (sanitized.isNotBlank()) {
                    samples.add(sanitized)
                }
            } catch (e: Exception) {
                lastError = e
                Logger.w(TAG, "Falha ao ler assinatura de firmware (tentativa ${attempt + 1}/$maxAttempts): ${e.message}")
            }

            if (attempt < maxAttempts - 1) {
                delay(80)
            }
        }

        if (samples.isEmpty()) {
            throw (lastError ?: UnsupportedFirmwareException("Unable to read firmware signature"))
        }

        return samples
    }

    private fun resolveFirmwareConsensus(samples: List<String>): FirmwareConsensus {
        val normalized = samples.mapNotNull { normalizeFirmwareSignature(it) }
        if (normalized.isEmpty()) {
            return FirmwareConsensus(signature = null, consensusHits = 0)
        }

        val grouped = normalized.groupingBy { it }.eachCount()
        val best = grouped.maxByOrNull { it.value }
        return FirmwareConsensus(signature = best?.key, consensusHits = best?.value ?: 0)
    }

    private fun sanitizeFirmwareSignature(raw: String): String {
        val withoutControls = raw.map { ch ->
            if (ch.isISOControl()) ' ' else ch
        }.joinToString("")

        return withoutControls
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeFirmwareSignature(signature: String): String? {
        val sanitized = sanitizeFirmwareSignature(signature)
        if (sanitized.isBlank()) return null

        // Canonical Speeduino signature.
        val canonical = Regex(
            """(?i)^speeduino\s+(\d{6})(?:\.\d+)?$"""
        ).find(sanitized)
        if (canonical != null) {
            return "speeduino ${canonical.groupValues[1]}"
        }

        val canonicalAlt = Regex(
            """(?i)^speeduino\s+(\d{4})\.(\d{2})$"""
        ).find(sanitized)
        if (canonicalAlt != null) {
            return "speeduino ${canonicalAlt.groupValues[1]}${canonicalAlt.groupValues[2]}"
        }

        val ms3Canonical = Regex(
            """(?i)^ms3\s+format\s+([0-9]{4}\.[0-9]{2}[a-z]?)$"""
        ).find(sanitized)
        if (ms3Canonical != null) {
            return "MS3 Format ${ms3Canonical.groupValues[1].uppercase(Locale.US)}"
        }

        val ms2Canonical = Regex(
            """(?i)^ms2extra\s+comms([0-9a-z]+)$"""
        ).find(sanitized)
        if (ms2Canonical != null) {
            return "MS2Extra comms${ms2Canonical.groupValues[1].lowercase(Locale.US)}"
        }

        val megaSpeedCanonical = Regex(
            """(?i)^ms2extra\s+megaspeed(?:\s+.*)?$"""
        ).find(sanitized)
        if (megaSpeedCanonical != null) {
            return "MS2Extra MegaSpeed"
        }

        if (sanitized.startsWith("rusEFI", ignoreCase = true)) {
            return sanitized
        }

        // Resilient path: tolerate corrupted prefix if we still have a "...duino" token + valid version.
        val hasDuinoToken = Regex("""(?i)\b[a-z]*duino\b""").containsMatchIn(sanitized)
        val v6 = Regex("""\b(20\d{4})\b""").find(sanitized)?.groupValues?.getOrNull(1)
        if (hasDuinoToken && !v6.isNullOrBlank()) {
            return "speeduino $v6"
        }

        val dotted = Regex("""\b(20\d{2})\.(\d{2})\b""").find(sanitized)
        if (hasDuinoToken && dotted != null) {
            return "speeduino ${dotted.groupValues[1]}${dotted.groupValues[2]}"
        }

        return null
    }

    private fun validateFirmwareConsensus(
        consensus: FirmwareConsensus,
        samples: List<String>
    ) {
        val signature = consensus.signature
        if (signature != null && (samples.size == 1 || consensus.consensusHits >= 2)) {
            return
        }

        connection.disconnect()
        val errorMessage = buildString {
            appendLine("❌ Assinatura de firmware inválida/ilegível")
            appendLine()
            appendLine("Amostras recebidas:")
            samples.forEach { sample ->
                appendLine("- $sample")
            }
            appendLine()
            appendLine("Isso costuma indicar problema no canal (ruído, baud incorreto, cabo/adaptador, timeout).")
            append("Tente reconectar e verificar a conexão.")
        }
        Logger.e(TAG, errorMessage)
        throw UnsupportedFirmwareException(errorMessage)
    }

    /**
     * Obtém informações do firmware conectado
     * @return FirmwareInfo ou null se não conectado
     */
    fun getFirmwareInfoCached(): FirmwareInfo? = firmwareInfo

    /**
     * Obtém definição genérica da ECU conectada.
     */
    fun getEcuDefinition(): EcuDefinition? = ecuDefinition

    /**
     * Obtém a família da ECU conectada.
     */
    fun getEcuFamily(): EcuFamily = firmwareInfo?.family ?: EcuFamily.UNKNOWN

    /**
     * Obtém capacidades declaradas da ECU conectada.
     */
    fun getEcuCapabilities(): EcuCapabilities? = firmwareInfo?.capabilities

    /**
     * Obtém informações do pin layout detectado (Page 1).
     */
    fun getPinLayoutInfoCached(): PinLayoutInfo? = pinLayoutInfo

    /**
     * Permite cachear pin layout detectado fora do fluxo padrão.
     */
    fun cachePinLayoutInfo(info: PinLayoutInfo?) {
        pinLayoutInfo = info
    }

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

    fun applyIniDefinition(definition: IniDefinition): Boolean {
        activeIniDefinition = definition

        if (firmwareInfo?.family == EcuFamily.SPEEDUINO) {
            val catalog = SpeeduinoIniDefinitions.fromIni(definition) ?: return false
            speeduinoIniCatalog = catalog
            tableDefinitions = catalog.tableDefinitions
            ecuDefinition = ecuDefinition?.copy(
                runtime = ecuDefinition?.runtime?.copy(
                    blockSize = definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize
                ) ?: return false,
                tableDefinitions = catalog.tableDefinitions,
                pageCatalog = catalog.pageCatalog.ifEmpty { ecuDefinition?.pageCatalog ?: emptyList() },
            )
            outputChannelFields = catalog.outputFields.ifEmpty {
                SpeeduinoOutputChannels.getDefinition(
                    definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize
                )
            }
            SpeeduinoOutputChannels.registerRuntimeDefinition(
                definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize,
                outputChannelFields ?: emptyList(),
            )
            Logger.i(
                TAG,
                "✅ Layout Speeduino aplicado via .ini: VE=${catalog.tableDefinitions.veTable.page}, " +
                    "IGN=${catalog.tableDefinitions.ignitionTable.page}, AFR=${catalog.tableDefinitions.afrTable.page}"
            )
            return true
        }

        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            val catalog = MegaSpeedIniTableDefinitions.fromIni(definition) ?: return false
            megaSpeedIniCatalog = catalog
            tableDefinitions = catalog.tableDefinitions
            ecuDefinition = ecuDefinition?.copy(
                runtime = ecuDefinition?.runtime?.copy(
                    blockSize = definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize
                ) ?: return false,
                tableDefinitions = catalog.tableDefinitions,
                pageCatalog = catalog.pageCatalog.ifEmpty { ecuDefinition?.pageCatalog ?: emptyList() },
            )
            outputChannelFields = SpeeduinoOutputChannels.getDefinition(
                definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize
            )
            Logger.i(
                TAG,
                "✅ Layout MegaSpeed aplicado via .ini: VE=${catalog.veTable.metadata.page}, " +
                    "IGN=${catalog.ignitionTable.metadata.page}, AFR=${catalog.afrTable.metadata.page}"
            )
            return true
        }

        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            val catalog = RusefiIniTableDefinitions.fromIni(definition) ?: return false
            rusefiIniCatalog = catalog
            tableDefinitions = catalog.tableDefinitions
            ecuDefinition = ecuDefinition?.copy(
                runtime = ecuDefinition?.runtime?.copy(
                    blockSize = definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize
                ) ?: return false,
                tableDefinitions = catalog.tableDefinitions,
                pageCatalog = catalog.pageCatalog.ifEmpty { ecuDefinition?.pageCatalog ?: emptyList() },
            )
            outputChannelFields = catalog.outputFields.ifEmpty {
                SpeeduinoOutputChannels.getDefinition(
                    definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize
                )
            }
            SpeeduinoOutputChannels.registerRuntimeDefinition(
                definition.ochBlockSize ?: catalog.tableDefinitions.ochBlockSize,
                outputChannelFields ?: emptyList(),
            )
            Logger.i(
                TAG,
                "✅ Layout rusEFI aplicado via .ini: VE=${formatPageId(catalog.veTable.metadata.page)}, " +
                    "IGN=${formatPageId(catalog.ignitionTable.metadata.page)}, AFR=${formatPageId(catalog.afrTable.metadata.page)}"
            )
            return true
        }

        return false
    }

    fun setManualFirmwareProfile(signature: String, readOnly: Boolean = true) {
        val normalized = normalizeFirmwareSignature(signature)
            ?: throw UnsupportedFirmwareException("Invalid manual firmware profile: $signature")
        manualFirmwareProfile = normalized
        readOnlySafeModeEnabled = readOnly
        Logger.w(TAG, "Perfil manual configurado: $normalized (readOnly=$readOnly)")
    }

    fun clearManualFirmwareProfile() {
        manualFirmwareProfile = null
        readOnlySafeModeEnabled = false
    }

    fun getManualFirmwareProfile(): String? = manualFirmwareProfile

    fun isReadOnlySafeMode(): Boolean = readOnlySafeModeEnabled

    /**
     * Desconecta do Speeduino
     */
    fun disconnect() {
        stopLiveDataStream()
        connection.disconnect()
        // NÃO cancelar o scope - apenas o job do stream
        // Isso permite reconectar e reiniciar o stream
        firmwareInfo = null
        ecuDefinition = null
        tableDefinitions = null
        outputChannelFields = null
        activeIniDefinition = null
        speeduinoIniCatalog = null
        megaSpeedIniCatalog = null
        rusefiIniCatalog = null
        cachedEngineConstants = null
        pinLayoutInfo = null
        SpeeduinoOutputChannels.clearAllRuntimeDefinitions()
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
        return when (getEcuFamily()) {
            EcuFamily.SPEEDUINO -> {
                protocol.getSerialCapability()
            }
            EcuFamily.RUSEFI -> {
                // rusEFI simulator/firmware compatibility profile used by this app.
                SerialCapability(protocolVersion = 1, blockingFactor = 1024, tableBlockingFactor = 1024)
            }
            EcuFamily.MS2,
            EcuFamily.MEGASPEED,
            EcuFamily.MS3 -> {
                // These families may close socket on unsupported 'f' capability command.
                // Use conservative defaults and avoid probing.
                SerialCapability(protocolVersion = 1, blockingFactor = 256, tableBlockingFactor = 256)
            }
            else -> {
                SerialCapability(protocolVersion = 1, blockingFactor = 256, tableBlockingFactor = 256)
            }
        }
    }

    /**
     * Lê CRC32 de uma página (comando 'd')
     */
    suspend fun getPageCRC(pageNum: Int): Long {
        require(pageNum in 0..0xFF) { "CRC page id fora do range legacy: ${formatPageId(pageNum)}" }
        return protocol.getPageCRC(pageNum.toByte())
    }

    suspend fun getPageCRC(pageNum: Byte): Long {
        return protocol.getPageCRC(pageNum)
    }

    /**
     * Lê uma página completa (comando 'p')
     * @param pageNum Número da página (0-15)
     * @param offset Offset inicial
     * @param length Tamanho a ler
     */
    suspend fun readPage(pageNum: Int, offset: Int, length: Int): ByteArray {
        require(pageNum in 0..0xFF) { "Page read legacy requer id de 8 bits: ${formatPageId(pageNum)}" }
        return protocol.readPage(pageNum.toByte(), offset, length)
    }

    suspend fun readPage(pageNum: Byte, offset: Int, length: Int): ByteArray {
        return protocol.readPage(pageNum, offset, length)
    }

    suspend fun readConfigChunk(pageId: Int, offset: Int, length: Int): ByteArray {
        return when (firmwareInfo?.family ?: EcuFamily.UNKNOWN) {
            EcuFamily.MS2, EcuFamily.MEGASPEED -> protocol.readTable(pageId, offset, length, EcuFamily.MS2)
            EcuFamily.MS3 -> protocol.readTable(pageId, offset, length, EcuFamily.MS3)
            EcuFamily.RUSEFI -> protocol.readTable(pageId, offset, length, EcuFamily.RUSEFI)
            else -> readPage(pageId, offset, length)
        }
    }

    suspend fun readConfigChunk(pageId: Byte, offset: Int, length: Int): ByteArray {
        return readConfigChunk(pageId.toInt() and 0xFF, offset, length)
    }

    /**
     * Lê página completa em blocos (para páginas grandes)
     */
    suspend fun readFullPage(pageNum: Int, pageSize: Int, blockSize: Int): ByteArray = withContext(Dispatchers.IO) {
        val pageLabel = formatPageId(pageNum)
        val pageData = ByteArray(pageSize)
        var offset = 0

        while (offset < pageSize) {
            val chunkSize = minOf(blockSize, pageSize - offset)
            val chunk = readConfigChunk(pageNum, offset, chunkSize)
            if (chunk.isEmpty()) {
                break
            }

            val bytesToCopy = minOf(chunk.size, pageSize - offset)
            chunk.copyInto(pageData, offset, endIndex = bytesToCopy)
            offset += bytesToCopy

            if (chunk.size < chunkSize) {
                Logger.w(
                    TAG,
                    "Leitura parcial da página $pageLabel: solicitado $chunkSize bytes, recebido ${chunk.size} no offset $offset"
                )
                break
            }

            // Small delay to avoid overwhelming the ECU
            delay(10)
        }

        if (offset == pageSize) {
            pageData
        } else {
            pageData.copyOf(offset)
        }
    }

    suspend fun readFullPage(pageNum: Byte, pageSize: Int, blockSize: Int): ByteArray {
        return readFullPage(pageNum.toInt() and 0xFF, pageSize, blockSize)
    }

    fun getEcuPageCatalog() = ecuDefinition?.pageCatalog ?: emptyList()

    /**
     * Lê Engine Constants (Page 1 - 128 bytes)
     */
    suspend fun readEngineConstants(): EngineConstants {
        val rusefiSchemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val constants = when (firmwareInfo?.family) {
            EcuFamily.MS2, EcuFamily.MEGASPEED -> {
                Logger.d(TAG, "Lendo Engine Constants MS2 (Page 0x04)...")
                val pageData = readFullPage(pageNum = 0x04, pageSize = 1024, blockSize = 256)
                Logger.d(TAG, "Page 0x04 recebida: ${pageData.size} bytes")
                EngineConstants.fromMs2Page1(pageData)
            }

            EcuFamily.RUSEFI -> {
                activeIniDefinition?.let { definition ->
                    val length = RusefiIniUiParsers.requiredBytesForEngine(definition)
                    Logger.d(TAG, "Lendo Engine Constants rusEFI via .ini (Page 0x0000 chunk 0..${length - 1})...")
                    val pageData = readConfigChunk(pageId = 0x0000, offset = 0, length = length)
                    RusefiIniUiParsers.parseEngineConstants(definition, pageData)
                } ?: run {
                    Logger.d(TAG, "Lendo Engine Constants rusEFI (Page 0x0000 chunk 0..555)...")
                    val pageData = readConfigChunk(pageId = 0x0000, offset = 0, length = 556)
                    EngineConstants.fromRusefiMainPage(pageData, rusefiSchemaId)
                }
            }

            else -> {
                Logger.d(TAG, "Lendo Engine Constants (Page 1)...")
                val pageData = readPage(pageNum = 1, offset = 0, length = 128)
                Logger.d(TAG, "Page 1 recebida: ${pageData.size} bytes")
                pinLayoutInfo = PinLayoutDetector.fromPage1(pageData)
                EngineConstants.fromPage1(pageData)
            }
        }

        cachedEngineConstants = constants
        return constants
    }

    /**
     * Lê calibração de pressão (MAP/Baro/EMAP) em Page 1.
     */
    suspend fun readPressureCalibration(): PressureCalibration = withContext(Dispatchers.IO) {
        val pageData = readPage(pageNum = 1, offset = 0, length = 128)
        PressureCalibration(
            mapMin = readS8(pageData, 46),
            mapMax = readU16(pageData, 47),
            baroMin = readS8(pageData, 64),
            baroMax = readU16(pageData, 65),
            emapMin = readS8(pageData, 67),
            emapMax = readU16(pageData, 68)
        )
    }

    /**
     * Grava calibração de pressão (MAP/Baro/EMAP) em Page 1 + burn.
     */
    suspend fun writePressureCalibration(calibration: PressureCalibration, burn: Boolean = true) = withContext(Dispatchers.IO) {
        ensureWritable("writePressureCalibration")
        val basePage = readPage(pageNum = 1, offset = 0, length = 128)
        writeS8(basePage, 46, calibration.mapMin)
        writeU16(basePage, 47, calibration.mapMax)
        writeS8(basePage, 64, calibration.baroMin)
        writeU16(basePage, 65, calibration.baroMax)
        writeS8(basePage, 67, calibration.emapMin)
        writeU16(basePage, 68, calibration.emapMax)
        protocol.writePage(pageNum = 1, offset = 0, data = basePage)
        if (burn) {
            delay(300)
            protocol.burnConfig()
        }
    }

    /**
     * Lê calibração de TPS em Page 1.
     */
    suspend fun readTpsCalibration(): TpsCalibration = withContext(Dispatchers.IO) {
        val pageData = readPage(pageNum = 1, offset = 0, length = 128)
        TpsCalibration(
            tpsMin = readU8(pageData, 44),
            tpsMax = readU8(pageData, 45)
        )
    }

    /**
     * Grava calibração de TPS em Page 1 + burn.
     */
    suspend fun writeTpsCalibration(calibration: TpsCalibration, burn: Boolean = true) = withContext(Dispatchers.IO) {
        ensureWritable("writeTpsCalibration")
        val basePage = readPage(pageNum = 1, offset = 0, length = 128)
        writeU8(basePage, 44, calibration.tpsMin)
        writeU8(basePage, 45, calibration.tpsMax)
        protocol.writePage(pageNum = 1, offset = 0, data = basePage)
        if (burn) {
            delay(300)
            protocol.burnConfig()
        }
    }

    /**
     * Lê Trigger Settings (Page 4 - 128 bytes)
     */
    suspend fun readTriggerSettings(): TriggerSettings {
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            activeIniDefinition?.let { definition ->
                val length = RusefiIniUiParsers.requiredBytesForTrigger(definition)
                Logger.d(TAG, "Lendo Trigger Settings rusEFI via .ini (Page 0x0000 chunk 0..${length - 1})...")
                val pageData = readConfigChunk(pageId = 0x0000, offset = 0, length = length)
                return RusefiIniUiParsers.parseTriggerSettings(definition, pageData)
            }
            val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
            val isF407Discovery = schemaId == "rusefi-f407-discovery"
            val chunkOffset = if (isF407Discovery) 484 else 488
            val paddedSize = if (isF407Discovery) 1658 else 1686
            val length = paddedSize - chunkOffset
            Logger.d(TAG, "Lendo Trigger Settings rusEFI (Page 0x0000 chunk $chunkOffset..${paddedSize - 1})...")
            val pageData = readConfigChunk(pageId = 0x0000, offset = chunkOffset, length = length)
            val padded = ByteArray(paddedSize)
            pageData.copyInto(padded, destinationOffset = chunkOffset)
            return TriggerSettings.fromRusefiMainPage(padded, schemaId)
        }
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            Logger.d(TAG, "Lendo Trigger Settings MS2 (Page 0x04)...")
            val pageData = readFullPage(
                pageNum = TriggerSettings.MS2_PAGE_NUMBER,
                pageSize = 1024,
                blockSize = 256
            )
            Logger.d(TAG, "Trigger Settings MS2 recebidos: ${pageData.size} bytes")
            return TriggerSettings.fromMs2PageData(pageData)
        }

        Logger.d(TAG, "Lendo Trigger Settings (Page 4)...")
        val pageData = readPage(
            pageNum = TriggerSettings.PAGE_NUMBER.toByte(),
            offset = 0,
            length = TriggerSettings.PAGE_LENGTH
        )
        Logger.d(TAG, "Trigger Settings recebidos: ${pageData.size} bytes")
        return TriggerSettings.fromPageData(pageData)
    }

    suspend fun readRusefiInputOutputSnapshot(): RusefiInputOutputSnapshot {
        if (firmwareInfo?.family != EcuFamily.RUSEFI) {
            throw UnsupportedOperationException("I/O snapshot disponível apenas para rusEFI")
        }
        activeIniDefinition?.let { definition ->
            val length = RusefiIniUiParsers.requiredBytesForInputOutput(definition)
            Logger.d(TAG, "Lendo rusEFI Input/Output snapshot via .ini (Page 0x0000 chunk 0..${length - 1})...")
            val pageData = readConfigChunk(pageId = 0x0000, offset = 0, length = length)
            return RusefiIniUiParsers.parseInputOutputSnapshot(definition, pageData)
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val chunkOffset = if (isF407Discovery) 52 else 56
        val paddedSize = if (isF407Discovery) 1640 else 1668
        val length = paddedSize - chunkOffset
        Logger.d(TAG, "Lendo rusEFI Input/Output snapshot (Page 0x0000 chunk $chunkOffset..${paddedSize - 1})...")
        val pageData = readConfigChunk(pageId = 0x0000, offset = chunkOffset, length = length)
        val padded = ByteArray(paddedSize)
        pageData.copyInto(padded, destinationOffset = chunkOffset)
        return RusefiInputOutputConfig.fromMainPage(padded, schemaId)
    }

    /**
     * Lê configuração do Secondary Serial (Page 9, offset 0)
     */
    suspend fun readSecondarySerialConfig(): SecondarySerialConfig = withContext(Dispatchers.IO) {
        Logger.d(TAG, "Lendo Secondary Serial Config (Page 9)...")
        val data = readPage(
            pageNum = SecondarySerialConfig.PAGE_NUMBER.toByte(),
            offset = SecondarySerialConfig.OFFSET,
            length = 1
        )
        val value = data.firstOrNull()?.toInt()?.and(0xFF) ?: 0
        SecondarySerialConfig.fromByte(value)
    }

    /**
     * Lê Engine Protection/Limiters (Page 6)
     */
    suspend fun readEngineProtectionConfig(): EngineProtectionConfig {
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            throw UnsupportedOperationException("Engine Protection rusEFI ainda não mapeado nesta versão")
        }
        Logger.d(TAG, "Lendo Engine Protection (Page 6)...")
        val pageData = readPage(
            pageNum = EngineProtectionMapper.PAGE_NUMBER.toByte(),
            offset = 0,
            length = EngineProtectionMapper.PAGE_SIZE
        )
        val era = firmwareInfo?.era ?: FirmwareEra.MODERN_2025
        return EngineProtectionMapper.fromPage(pageData, era)
    }

    /**
     * Grava Engine Protection/Limiters (Page 6) + Burn
     */
    suspend fun writeEngineProtectionConfig(config: EngineProtectionConfig, burn: Boolean = true) {
        ensureWritable("writeEngineProtectionConfig")
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            throw UnsupportedOperationException("Engine Protection rusEFI ainda não mapeado nesta versão")
        }
        Logger.d(TAG, "Gravando Engine Protection (Page 6)...")
        val basePage = readPage(
            pageNum = EngineProtectionMapper.PAGE_NUMBER.toByte(),
            offset = 0,
            length = EngineProtectionMapper.PAGE_SIZE
        )
        val era = firmwareInfo?.era ?: FirmwareEra.MODERN_2025
        val updatedData = EngineProtectionMapper.applyToPage(basePage, config, era)
        protocol.writePage(
            pageNum = EngineProtectionMapper.PAGE_NUMBER.toByte(),
            offset = 0,
            data = updatedData
        )
        if (burn) {
            delay(300)
            protocol.burnConfig()
            Logger.d(TAG, "Engine Protection gravado e burn executado")
        } else {
            Logger.d(TAG, "Engine Protection gravado (sem burn)")
        }
    }

    /**
     * Grava Trigger Settings (Page 4) + Burn
     */
    suspend fun writeTriggerSettings(settings: TriggerSettings, burn: Boolean = true) {
        ensureWritable("writeTriggerSettings")
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            throw UnsupportedOperationException("Trigger Settings rusEFI ainda não mapeados nesta versão")
        }
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            Logger.d(TAG, "Gravando Trigger Settings MS2 (Page 0x04)...")
            val basePage = readFullPage(
                pageNum = TriggerSettings.MS2_PAGE_NUMBER,
                pageSize = 1024,
                blockSize = 256
            )
            val updatedData = settings.toMs2PageData(basePage)
            protocol.writeTable(
                tableId = TriggerSettings.MS2_PAGE_NUMBER,
                offset = 0,
                data = updatedData
            )
            if (burn) {
                delay(300)
                protocol.burnTable(TriggerSettings.MS2_PAGE_NUMBER)
                Logger.d(TAG, "Trigger Settings MS2 gravados e burn executado")
            } else {
                Logger.d(TAG, "Trigger Settings MS2 gravados (sem burn)")
            }
            return
        }

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
     * Grava configuração do Secondary Serial (Page 9, offset 0) + Burn
     */
    suspend fun writeSecondarySerialConfig(config: SecondarySerialConfig, burn: Boolean = true) = withContext(Dispatchers.IO) {
        ensureWritable("writeSecondarySerialConfig")
        Logger.d(TAG, "Gravando Secondary Serial Config (Page 9)...")
        val baseData = readPage(
            pageNum = SecondarySerialConfig.PAGE_NUMBER.toByte(),
            offset = SecondarySerialConfig.OFFSET,
            length = 1
        )
        val original = baseData.firstOrNull()?.toInt()?.and(0xFF) ?: 0
        val updated = config.applyToByte(original)

        protocol.writePage(
            pageNum = SecondarySerialConfig.PAGE_NUMBER.toByte(),
            offset = SecondarySerialConfig.OFFSET,
            data = byteArrayOf(updated.toByte())
        )

        if (burn) {
            delay(300)
            protocol.burnConfig()
            Logger.d(TAG, "Secondary Serial Config gravado e burn executado")
        } else {
            Logger.d(TAG, "Secondary Serial Config gravado (sem burn)")
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
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            return readMs2VeTable()
        }
        if (firmwareInfo?.family == EcuFamily.MS3) {
            return readMs3VeTable()
        }
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            return readRusefiVeTable()
        }

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
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            return readMs2IgnitionTable()
        }
        if (firmwareInfo?.family == EcuFamily.MS3) {
            return readMs3IgnitionTable()
        }
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            return readRusefiIgnitionTable()
        }

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
        ensureWritable("writeEngineConstants")
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            throw UnsupportedOperationException("Engine Constants rusEFI ainda não mapeados nesta versão")
        }
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            Logger.d(TAG, "Gravando Engine Constants MS2 (Page 0x04)...")
            val basePage = readFullPage(pageNum = 0x04, pageSize = 1024, blockSize = 256)
            val pageData = engineConstants.applyToMs2Page1(basePage)
            protocol.writeTable(tableId = 0x04, offset = 0, data = pageData)
            protocol.burnTable(tableId = 0x04)
            Logger.d(TAG, "Page 0x04 gravada com sucesso")
        } else {
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
        }
        cachedEngineConstants = engineConstants
    }

    /**
     * Grava uma página completa de configuração (backup/restore)
     */
    suspend fun writeRawPage(pageNum: Int, data: ByteArray) {
        ensureWritable("writeRawPage")
        if (
            firmwareInfo?.family == EcuFamily.MS2 ||
            firmwareInfo?.family == EcuFamily.MEGASPEED ||
            firmwareInfo?.family == EcuFamily.MS3 ||
            firmwareInfo?.family == EcuFamily.RUSEFI
        ) {
            protocol.writeTable(tableId = pageNum, offset = 0, data = data, family = getEcuFamily())
            protocol.burnTable(tableId = pageNum, family = getEcuFamily())
        } else {
            protocol.writePage(pageNum = pageNum.toByte(), offset = 0, data = data)
            protocol.burnConfig()
        }
        Logger.d(TAG, "Página ${formatPageId(pageNum)} gravada via backup e burn executado")
    }

    suspend fun writeRawPage(pageNum: Byte, data: ByteArray) {
        writeRawPage(pageNum.toInt() and 0xFF, data)
    }

    /**
     * Grava uma página completa sem executar burn (para restauração em lote).
     */
    suspend fun writeRawPageWithoutBurn(pageNum: Int, data: ByteArray) {
        ensureWritable("writeRawPageWithoutBurn")
        if (
            firmwareInfo?.family == EcuFamily.MS2 ||
            firmwareInfo?.family == EcuFamily.MEGASPEED ||
            firmwareInfo?.family == EcuFamily.MS3 ||
            firmwareInfo?.family == EcuFamily.RUSEFI
        ) {
            protocol.writeTable(tableId = pageNum, offset = 0, data = data, family = getEcuFamily())
        } else {
            protocol.writePage(pageNum = pageNum.toByte(), offset = 0, data = data)
        }
        Logger.d(TAG, "Página ${formatPageId(pageNum)} gravada via backup (sem burn)")
    }

    suspend fun writeRawPageWithoutBurn(pageNum: Byte, data: ByteArray) {
        writeRawPageWithoutBurn(pageNum.toInt() and 0xFF, data)
    }

    /**
     * Executa burn após gravações em lote.
     */
    suspend fun burnConfigs() {
        ensureWritable("burnConfigs")
        if (
            firmwareInfo?.family == EcuFamily.MS2 ||
            firmwareInfo?.family == EcuFamily.MEGASPEED ||
            firmwareInfo?.family == EcuFamily.MS3 ||
            firmwareInfo?.family == EcuFamily.RUSEFI
        ) {
            throw IllegalStateException("${firmwareInfo?.family} requires page-specific burn; use table-specific write helpers.")
        }
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
        ensureWritable("writeVeTable")
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            writeMs2VeTable(veTable)
            return
        }
        if (firmwareInfo?.family == EcuFamily.MS3) {
            writeMs3VeTable(veTable)
            return
        }
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            writeRusefiVeTable(veTable)
            return
        }
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
        ensureWritable("writeIgnitionTable")
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            writeMs2IgnitionTable(ignitionTable)
            return
        }
        if (firmwareInfo?.family == EcuFamily.MS3) {
            writeMs3IgnitionTable(ignitionTable)
            return
        }
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            writeRusefiIgnitionTable(ignitionTable)
            return
        }
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
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            return readMs2AfrTable()
        }
        if (firmwareInfo?.family == EcuFamily.MS3) {
            return readMs3AfrTable()
        }
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            return readRusefiAfrTable()
        }

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

    private suspend fun readMs3VeTable(): VeTable {
        val layout = Ms3TableDefinitions.VE_TABLE_1
        Logger.d(
            TAG,
            "Lendo MS3 VE Table 1 (table 0x${layout.metadata.page.toString(16).uppercase(Locale.US)}, ${layout.metadata.totalSize} bytes)..."
        )
        val valuesData = readConfigChunk(
            pageId = layout.metadata.page.toByte(),
            offset = layout.metadata.offset,
            length = layout.metadata.totalSize
        )
        val rpmAxisData = readConfigChunk(
            pageId = layout.rpmAxis.tableId.toByte(),
            offset = layout.rpmAxis.offset,
            length = layout.rpmAxis.count * 2
        )
        val loadAxisData = readConfigChunk(
            pageId = layout.loadAxis.tableId.toByte(),
            offset = layout.loadAxis.offset,
            length = layout.loadAxis.count * 2
        )
        return Ms3TableDefinitions.parseVeTable(
            valuesData = valuesData,
            rpmAxisData = rpmAxisData,
            loadAxisData = loadAxisData,
            loadType = VeTable.LoadType.MAP
        )
    }

    private suspend fun readMs2VeTable(): VeTable {
        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            megaSpeedIniCatalog?.let { catalog ->
                Logger.d(TAG, "Lendo MegaSpeed VE Table 1 via .ini (${formatPageId(catalog.veTable.metadata.page)})...")
                val valuesData = readConfigChunk(catalog.veTable.metadata.page, catalog.veTable.metadata.offset, catalog.veTable.metadata.totalSize)
                val rpmAxisData = readConfigChunk(catalog.veTable.rpmAxis.pageId, catalog.veTable.rpmAxis.offset, catalog.veTable.rpmAxis.count * 2)
                val loadAxisData = readConfigChunk(catalog.veTable.loadAxis.pageId, catalog.veTable.loadAxis.offset, catalog.veTable.loadAxis.count * 2)
                return MegaSpeedIniTableDefinitions.parseVeTable(catalog.veTable, valuesData, rpmAxisData, loadAxisData)
            }
        }
        val layout = Ms2TableDefinitions.VE_TABLE_1
        Logger.d(
            TAG,
            "Lendo MS2 VE Table 1 (table 0x${layout.metadata.page.toString(16).uppercase(Locale.US)}, ${layout.metadata.totalSize} bytes)..."
        )
        val valuesData = readConfigChunk(
            pageId = layout.metadata.page.toByte(),
            offset = layout.metadata.offset,
            length = layout.metadata.totalSize
        )
        val rpmAxisData = readConfigChunk(
            pageId = layout.rpmAxis.tableId.toByte(),
            offset = layout.rpmAxis.offset,
            length = layout.rpmAxis.count * 2
        )
        val loadAxisData = readConfigChunk(
            pageId = layout.loadAxis.tableId.toByte(),
            offset = layout.loadAxis.offset,
            length = layout.loadAxis.count * 2
        )
        return Ms2TableDefinitions.parseVeTable(
            valuesData = valuesData,
            rpmAxisData = rpmAxisData,
            loadAxisData = loadAxisData,
            loadType = VeTable.LoadType.MAP
        )
    }

    private suspend fun readMs3IgnitionTable(): IgnitionTable {
        val layout = Ms3TableDefinitions.IGNITION_TABLE_1
        Logger.d(
            TAG,
            "Lendo MS3 Ignition Table 1 (table 0x${layout.metadata.page.toString(16).uppercase(Locale.US)}, ${layout.metadata.totalSize} bytes)..."
        )
        val valuesData = readConfigChunk(
            pageId = layout.metadata.page.toByte(),
            offset = layout.metadata.offset,
            length = layout.metadata.totalSize
        )
        val rpmAxisData = readConfigChunk(
            pageId = layout.rpmAxis.tableId.toByte(),
            offset = layout.rpmAxis.offset,
            length = layout.rpmAxis.count * 2
        )
        val loadAxisData = readConfigChunk(
            pageId = layout.loadAxis.tableId.toByte(),
            offset = layout.loadAxis.offset,
            length = layout.loadAxis.count * 2
        )
        return Ms3TableDefinitions.parseIgnitionTable(
            valuesData = valuesData,
            rpmAxisData = rpmAxisData,
            loadAxisData = loadAxisData,
            loadType = IgnitionTable.LoadType.MAP
        )
    }

    private suspend fun readMs2IgnitionTable(): IgnitionTable {
        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            megaSpeedIniCatalog?.let { catalog ->
                Logger.d(TAG, "Lendo MegaSpeed Ignition Table 1 via .ini (${formatPageId(catalog.ignitionTable.metadata.page)})...")
                val valuesData = readConfigChunk(catalog.ignitionTable.metadata.page, catalog.ignitionTable.metadata.offset, catalog.ignitionTable.metadata.totalSize)
                val rpmAxisData = readConfigChunk(catalog.ignitionTable.rpmAxis.pageId, catalog.ignitionTable.rpmAxis.offset, catalog.ignitionTable.rpmAxis.count * 2)
                val loadAxisData = readConfigChunk(catalog.ignitionTable.loadAxis.pageId, catalog.ignitionTable.loadAxis.offset, catalog.ignitionTable.loadAxis.count * 2)
                return MegaSpeedIniTableDefinitions.parseIgnitionTable(catalog.ignitionTable, valuesData, rpmAxisData, loadAxisData)
            }
        }
        val layout = Ms2TableDefinitions.IGNITION_TABLE_1
        Logger.d(
            TAG,
            "Lendo MS2 Ignition Table 1 (table 0x${layout.metadata.page.toString(16).uppercase(Locale.US)}, ${layout.metadata.totalSize} bytes)..."
        )
        val valuesData = readConfigChunk(
            pageId = layout.metadata.page.toByte(),
            offset = layout.metadata.offset,
            length = layout.metadata.totalSize
        )
        val rpmAxisData = readConfigChunk(
            pageId = layout.rpmAxis.tableId.toByte(),
            offset = layout.rpmAxis.offset,
            length = layout.rpmAxis.count * 2
        )
        val loadAxisData = readConfigChunk(
            pageId = layout.loadAxis.tableId.toByte(),
            offset = layout.loadAxis.offset,
            length = layout.loadAxis.count * 2
        )
        return Ms2TableDefinitions.parseIgnitionTable(
            valuesData = valuesData,
            rpmAxisData = rpmAxisData,
            loadAxisData = loadAxisData,
            loadType = IgnitionTable.LoadType.MAP
        )
    }

    private suspend fun readMs3AfrTable(): AfrTable {
        val layout = Ms3TableDefinitions.AFR_TABLE_1
        Logger.d(
            TAG,
            "Lendo MS3 AFR Table 1 (table 0x${layout.metadata.page.toString(16).uppercase(Locale.US)}, ${layout.metadata.totalSize} bytes)..."
        )
        val valuesData = readConfigChunk(
            pageId = layout.metadata.page.toByte(),
            offset = layout.metadata.offset,
            length = layout.metadata.totalSize
        )
        val rpmAxisData = readConfigChunk(
            pageId = layout.rpmAxis.tableId.toByte(),
            offset = layout.rpmAxis.offset,
            length = layout.rpmAxis.count * 2
        )
        val loadAxisData = readConfigChunk(
            pageId = layout.loadAxis.tableId.toByte(),
            offset = layout.loadAxis.offset,
            length = layout.loadAxis.count * 2
        )
        return Ms3TableDefinitions.parseAfrTable(
            valuesData = valuesData,
            rpmAxisData = rpmAxisData,
            loadAxisData = loadAxisData,
            loadType = AfrTable.LoadType.MAP
        )
    }

    private suspend fun readMs2AfrTable(): AfrTable {
        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            megaSpeedIniCatalog?.let { catalog ->
                Logger.d(TAG, "Lendo MegaSpeed AFR Table 1 via .ini (${formatPageId(catalog.afrTable.metadata.page)})...")
                val valuesData = readConfigChunk(catalog.afrTable.metadata.page, catalog.afrTable.metadata.offset, catalog.afrTable.metadata.totalSize)
                val rpmAxisData = readConfigChunk(catalog.afrTable.rpmAxis.pageId, catalog.afrTable.rpmAxis.offset, catalog.afrTable.rpmAxis.count * 2)
                val loadAxisData = readConfigChunk(catalog.afrTable.loadAxis.pageId, catalog.afrTable.loadAxis.offset, catalog.afrTable.loadAxis.count * 2)
                return MegaSpeedIniTableDefinitions.parseAfrTable(catalog.afrTable, valuesData, rpmAxisData, loadAxisData)
            }
        }
        val layout = Ms2TableDefinitions.AFR_TABLE_1
        Logger.d(
            TAG,
            "Lendo MS2 AFR Table 1 (table 0x${layout.metadata.page.toString(16).uppercase(Locale.US)}, ${layout.metadata.totalSize} bytes)..."
        )
        val valuesData = readConfigChunk(
            pageId = layout.metadata.page.toByte(),
            offset = layout.metadata.offset,
            length = layout.metadata.totalSize
        )
        val rpmAxisData = readConfigChunk(
            pageId = layout.rpmAxis.tableId.toByte(),
            offset = layout.rpmAxis.offset,
            length = layout.rpmAxis.count * 2
        )
        val loadAxisData = readConfigChunk(
            pageId = layout.loadAxis.tableId.toByte(),
            offset = layout.loadAxis.offset,
            length = layout.loadAxis.count * 2
        )
        return Ms2TableDefinitions.parseAfrTable(
            valuesData = valuesData,
            rpmAxisData = rpmAxisData,
            loadAxisData = loadAxisData,
            loadType = AfrTable.LoadType.MAP
        )
    }

    private suspend fun readRusefiVeTable(): VeTable {
        rusefiIniCatalog?.let { catalog ->
            Logger.d(TAG, "Lendo rusEFI VE Table 1 via .ini (${formatPageId(catalog.veTable.metadata.page)})...")
            val valuesData = readConfigChunk(catalog.veTable.metadata.page, catalog.veTable.metadata.offset, catalog.veTable.metadata.totalSize)
            val rpmAxisData = readConfigChunk(catalog.veTable.rpmAxis.tableId, catalog.veTable.rpmAxis.offset, catalog.veTable.rpmAxis.count * 2)
            val loadAxisData = readConfigChunk(catalog.veTable.loadAxis.tableId, catalog.veTable.loadAxis.offset, catalog.veTable.loadAxis.count * 2)
            return RusefiTableDefinitions.parseVeTableWithLayout(catalog.veTable, valuesData, rpmAxisData, loadAxisData, loadType = VeTable.LoadType.MAP)
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val layout = if (isF407Discovery) RusefiF407DiscoveryDefinitions.VE_TABLE_1 else RusefiTableDefinitions.VE_TABLE_1
        Logger.d(TAG, "Lendo rusEFI VE Table 1 (${formatPageId(layout.metadata.page)})...")
        val valuesData = readConfigChunk(layout.metadata.page, layout.metadata.offset, layout.metadata.totalSize)
        val rpmAxisData = readConfigChunk(layout.rpmAxis.tableId, layout.rpmAxis.offset, layout.rpmAxis.count * 2)
        val loadAxisData = readConfigChunk(layout.loadAxis.tableId, layout.loadAxis.offset, layout.loadAxis.count * 2)
        return if (isF407Discovery) {
            RusefiF407DiscoveryDefinitions.parseVeTable(valuesData, rpmAxisData, loadAxisData)
        } else {
            RusefiTableDefinitions.parseVeTable(valuesData, rpmAxisData, loadAxisData, loadType = VeTable.LoadType.MAP)
        }
    }

    private suspend fun readRusefiIgnitionTable(): IgnitionTable {
        rusefiIniCatalog?.let { catalog ->
            Logger.d(TAG, "Lendo rusEFI Ignition Table 1 via .ini (${formatPageId(catalog.ignitionTable.metadata.page)})...")
            val valuesData = readConfigChunk(catalog.ignitionTable.metadata.page, catalog.ignitionTable.metadata.offset, catalog.ignitionTable.metadata.totalSize)
            val rpmAxisData = readConfigChunk(catalog.ignitionTable.rpmAxis.tableId, catalog.ignitionTable.rpmAxis.offset, catalog.ignitionTable.rpmAxis.count * 2)
            val loadAxisData = readConfigChunk(catalog.ignitionTable.loadAxis.tableId, catalog.ignitionTable.loadAxis.offset, catalog.ignitionTable.loadAxis.count * 2)
            return RusefiTableDefinitions.parseIgnitionTableWithLayout(catalog.ignitionTable, valuesData, rpmAxisData, loadAxisData, loadType = IgnitionTable.LoadType.MAP)
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val layout = if (isF407Discovery) RusefiF407DiscoveryDefinitions.IGNITION_TABLE_1 else RusefiTableDefinitions.IGNITION_TABLE_1
        Logger.d(TAG, "Lendo rusEFI Ignition Table 1 (${formatPageId(layout.metadata.page)})...")
        val valuesData = readConfigChunk(layout.metadata.page, layout.metadata.offset, layout.metadata.totalSize)
        val rpmAxisData = readConfigChunk(layout.rpmAxis.tableId, layout.rpmAxis.offset, layout.rpmAxis.count * 2)
        val loadAxisData = readConfigChunk(layout.loadAxis.tableId, layout.loadAxis.offset, layout.loadAxis.count * 2)
        return if (isF407Discovery) {
            RusefiF407DiscoveryDefinitions.parseIgnitionTable(valuesData, rpmAxisData, loadAxisData)
        } else {
            RusefiTableDefinitions.parseIgnitionTable(valuesData, rpmAxisData, loadAxisData, loadType = IgnitionTable.LoadType.MAP)
        }
    }

    private suspend fun readRusefiAfrTable(): AfrTable {
        rusefiIniCatalog?.let { catalog ->
            Logger.d(TAG, "Lendo rusEFI AFR Table 1 via .ini (${formatPageId(catalog.afrTable.metadata.page)})...")
            val valuesData = readConfigChunk(catalog.afrTable.metadata.page, catalog.afrTable.metadata.offset, catalog.afrTable.metadata.totalSize)
            val rpmAxisData = readConfigChunk(catalog.afrTable.rpmAxis.tableId, catalog.afrTable.rpmAxis.offset, catalog.afrTable.rpmAxis.count * 2)
            val loadAxisData = readConfigChunk(catalog.afrTable.loadAxis.tableId, catalog.afrTable.loadAxis.offset, catalog.afrTable.loadAxis.count * 2)
            return RusefiTableDefinitions.parseAfrTableWithLayout(catalog.afrTable, valuesData, rpmAxisData, loadAxisData, loadType = AfrTable.LoadType.MAP)
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val layout = if (isF407Discovery) RusefiF407DiscoveryDefinitions.AFR_TABLE_1 else RusefiTableDefinitions.AFR_TABLE_1
        Logger.d(TAG, "Lendo rusEFI AFR Table 1 (${formatPageId(layout.metadata.page)})...")
        val valuesData = readConfigChunk(layout.metadata.page, layout.metadata.offset, layout.metadata.totalSize)
        val rpmAxisData = readConfigChunk(layout.rpmAxis.tableId, layout.rpmAxis.offset, layout.rpmAxis.count * 2)
        val loadAxisData = readConfigChunk(layout.loadAxis.tableId, layout.loadAxis.offset, layout.loadAxis.count * 2)
        return if (isF407Discovery) {
            RusefiF407DiscoveryDefinitions.parseAfrTable(valuesData, rpmAxisData, loadAxisData)
        } else {
            RusefiTableDefinitions.parseAfrTable(valuesData, rpmAxisData, loadAxisData, loadType = AfrTable.LoadType.MAP)
        }
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
        ensureWritable("writeAfrTable")
        if (firmwareInfo?.family == EcuFamily.MS2 || firmwareInfo?.family == EcuFamily.MEGASPEED) {
            writeMs2AfrTable(afrTable)
            return
        }
        if (firmwareInfo?.family == EcuFamily.MS3) {
            writeMs3AfrTable(afrTable)
            return
        }
        if (firmwareInfo?.family == EcuFamily.RUSEFI) {
            writeRusefiAfrTable(afrTable)
            return
        }
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

    private suspend fun writeMs3VeTable(veTable: VeTable) {
        val validationResult = TableValidator(Ms3TableDefinitions.VE_TABLE_1.metadata)
            .validateBeforeWrite(veTable)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }

        val serialized = Ms3TableDefinitions.serializeVeTable(veTable)
        protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
        protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
        protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
        Logger.d(TAG, "✅ MS3 VE Table 1 gravada e burn executado")
    }

    private suspend fun writeMs2VeTable(veTable: VeTable) {
        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            megaSpeedIniCatalog?.let { catalog ->
                val validationResult = TableValidator(catalog.veTable.metadata).validateBeforeWrite(veTable)
                if (!validationResult.isValid) {
                    throw ValidationException(validationResult)
                }
                val serialized = MegaSpeedIniTableDefinitions.serializeVeTable(catalog.veTable, veTable)
                protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
                protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
                protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
                delay(300)
                serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
                Logger.d(TAG, "✅ MegaSpeed VE Table 1 gravada via .ini e burn executado")
                return
            }
        }
        val validationResult = TableValidator(Ms2TableDefinitions.VE_TABLE_1.metadata)
            .validateBeforeWrite(veTable)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }

        val serialized = Ms2TableDefinitions.serializeVeTable(veTable)
        protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
        protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
        protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
        Logger.d(TAG, "✅ MS2 VE Table 1 gravada e burn executado")
    }

    private suspend fun writeMs3IgnitionTable(ignitionTable: IgnitionTable) {
        val validationResult = TableValidator(Ms3TableDefinitions.IGNITION_TABLE_1.metadata)
            .validateBeforeWrite(ignitionTable)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }

        val serialized = Ms3TableDefinitions.serializeIgnitionTable(ignitionTable)
        protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
        protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
        protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
        Logger.d(TAG, "✅ MS3 Ignition Table 1 gravada e burn executado")
    }

    private suspend fun writeMs2IgnitionTable(ignitionTable: IgnitionTable) {
        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            megaSpeedIniCatalog?.let { catalog ->
                val validationResult = TableValidator(catalog.ignitionTable.metadata).validateBeforeWrite(ignitionTable)
                if (!validationResult.isValid) {
                    throw ValidationException(validationResult)
                }
                val serialized = MegaSpeedIniTableDefinitions.serializeIgnitionTable(catalog.ignitionTable, ignitionTable)
                protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
                protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
                protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
                delay(300)
                serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
                Logger.d(TAG, "✅ MegaSpeed Ignition Table 1 gravada via .ini e burn executado")
                return
            }
        }
        val validationResult = TableValidator(Ms2TableDefinitions.IGNITION_TABLE_1.metadata)
            .validateBeforeWrite(ignitionTable)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }

        val serialized = Ms2TableDefinitions.serializeIgnitionTable(ignitionTable)
        protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
        protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
        protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
        Logger.d(TAG, "✅ MS2 Ignition Table 1 gravada e burn executado")
    }

    private suspend fun writeMs3AfrTable(afrTable: AfrTable) {
        val serialized = Ms3TableDefinitions.serializeAfrTable(afrTable)
        protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
        protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
        protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
        Logger.d(TAG, "✅ MS3 AFR Table 1 gravada e burn executado")
    }

    private suspend fun writeMs2AfrTable(afrTable: AfrTable) {
        if (firmwareInfo?.family == EcuFamily.MEGASPEED) {
            megaSpeedIniCatalog?.let { catalog ->
                val serialized = MegaSpeedIniTableDefinitions.serializeAfrTable(catalog.afrTable, afrTable)
                protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
                protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
                protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
                delay(300)
                serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
                Logger.d(TAG, "✅ MegaSpeed AFR Table 1 gravada via .ini e burn executado")
                return
            }
        }
        val serialized = Ms2TableDefinitions.serializeAfrTable(afrTable)
        protocol.writeTable(serialized.valuesTableId.toByte(), serialized.valuesOffset, serialized.valuesData)
        protocol.writeTable(serialized.rpmAxisTableId.toByte(), serialized.rpmAxisOffset, serialized.rpmAxisData)
        protocol.writeTable(serialized.loadAxisTableId.toByte(), serialized.loadAxisOffset, serialized.loadAxisData)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it.toByte()) }
        Logger.d(TAG, "✅ MS2 AFR Table 1 gravada e burn executado")
    }

    private suspend fun writeRusefiVeTable(veTable: VeTable) {
        rusefiIniCatalog?.let { catalog ->
            val validationResult = TableValidator(catalog.veTable.metadata).validateBeforeWrite(veTable)
            if (!validationResult.isValid) {
                throw ValidationException(validationResult)
            }
            val serialized = RusefiTableDefinitions.serializeVeTableWithLayout(
                layout = catalog.veTable,
                table = veTable,
                signedValues = false,
                valueScale = 10,
            )
            protocol.writeTable(serialized.valuesTableId, serialized.valuesOffset, serialized.valuesData, EcuFamily.RUSEFI)
            protocol.writeTable(serialized.rpmAxisTableId, serialized.rpmAxisOffset, serialized.rpmAxisData, EcuFamily.RUSEFI)
            protocol.writeTable(serialized.loadAxisTableId, serialized.loadAxisOffset, serialized.loadAxisData, EcuFamily.RUSEFI)
            delay(300)
            serialized.burnTableIds.sorted().forEach { protocol.burnTable(it, EcuFamily.RUSEFI) }
            Logger.d(TAG, "✅ rusEFI VE Table 1 gravada via .ini e burn executado")
            return
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val metadata = if (isF407Discovery) RusefiF407DiscoveryDefinitions.VE_TABLE_1.metadata else RusefiTableDefinitions.VE_TABLE_1.metadata
        val validationResult = TableValidator(metadata)
            .validateBeforeWrite(veTable)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }

        val serialized = if (isF407Discovery) {
            RusefiF407DiscoveryDefinitions.serializeVeTable(veTable)
        } else {
            RusefiTableDefinitions.serializeVeTable(veTable)
        }
        protocol.writeTable(serialized.valuesTableId, serialized.valuesOffset, serialized.valuesData, EcuFamily.RUSEFI)
        protocol.writeTable(serialized.rpmAxisTableId, serialized.rpmAxisOffset, serialized.rpmAxisData, EcuFamily.RUSEFI)
        protocol.writeTable(serialized.loadAxisTableId, serialized.loadAxisOffset, serialized.loadAxisData, EcuFamily.RUSEFI)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it, EcuFamily.RUSEFI) }
        Logger.d(TAG, "✅ rusEFI VE Table 1 gravada e burn executado")
    }

    private suspend fun writeRusefiIgnitionTable(ignitionTable: IgnitionTable) {
        rusefiIniCatalog?.let { catalog ->
            val validationResult = TableValidator(catalog.ignitionTable.metadata).validateBeforeWrite(ignitionTable)
            if (!validationResult.isValid) {
                throw ValidationException(validationResult)
            }
            val serialized = RusefiTableDefinitions.serializeIgnitionTableWithLayout(catalog.ignitionTable, ignitionTable)
            protocol.writeTable(serialized.valuesTableId, serialized.valuesOffset, serialized.valuesData, EcuFamily.RUSEFI)
            protocol.writeTable(serialized.rpmAxisTableId, serialized.rpmAxisOffset, serialized.rpmAxisData, EcuFamily.RUSEFI)
            protocol.writeTable(serialized.loadAxisTableId, serialized.loadAxisOffset, serialized.loadAxisData, EcuFamily.RUSEFI)
            delay(300)
            serialized.burnTableIds.sorted().forEach { protocol.burnTable(it, EcuFamily.RUSEFI) }
            Logger.d(TAG, "✅ rusEFI Ignition Table 1 gravada via .ini e burn executado")
            return
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val metadata = if (isF407Discovery) RusefiF407DiscoveryDefinitions.IGNITION_TABLE_1.metadata else RusefiTableDefinitions.IGNITION_TABLE_1.metadata
        val validationResult = TableValidator(metadata)
            .validateBeforeWrite(ignitionTable)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult)
        }

        val serialized = if (isF407Discovery) {
            RusefiF407DiscoveryDefinitions.serializeIgnitionTable(ignitionTable)
        } else {
            RusefiTableDefinitions.serializeIgnitionTable(ignitionTable)
        }
        protocol.writeTable(serialized.valuesTableId, serialized.valuesOffset, serialized.valuesData, EcuFamily.RUSEFI)
        protocol.writeTable(serialized.rpmAxisTableId, serialized.rpmAxisOffset, serialized.rpmAxisData, EcuFamily.RUSEFI)
        protocol.writeTable(serialized.loadAxisTableId, serialized.loadAxisOffset, serialized.loadAxisData, EcuFamily.RUSEFI)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it, EcuFamily.RUSEFI) }
        Logger.d(TAG, "✅ rusEFI Ignition Table 1 gravada e burn executado")
    }

    private suspend fun writeRusefiAfrTable(afrTable: AfrTable) {
        rusefiIniCatalog?.let { catalog ->
            val serialized = RusefiTableDefinitions.serializeAfrTableWithLayout(catalog.afrTable, afrTable)
            protocol.writeTable(serialized.valuesTableId, serialized.valuesOffset, serialized.valuesData, EcuFamily.RUSEFI)
            protocol.writeTable(serialized.rpmAxisTableId, serialized.rpmAxisOffset, serialized.rpmAxisData, EcuFamily.RUSEFI)
            protocol.writeTable(serialized.loadAxisTableId, serialized.loadAxisOffset, serialized.loadAxisData, EcuFamily.RUSEFI)
            delay(300)
            serialized.burnTableIds.sorted().forEach { protocol.burnTable(it, EcuFamily.RUSEFI) }
            Logger.d(TAG, "✅ rusEFI AFR Table 1 gravada via .ini e burn executado")
            return
        }
        val schemaId = ecuDefinition?.runtime?.schemaId ?: "rusefi-main"
        val isF407Discovery = schemaId == "rusefi-f407-discovery"
        val serialized = if (isF407Discovery) {
            RusefiF407DiscoveryDefinitions.serializeAfrTable(afrTable)
        } else {
            RusefiTableDefinitions.serializeAfrTable(afrTable)
        }
        protocol.writeTable(serialized.valuesTableId, serialized.valuesOffset, serialized.valuesData, EcuFamily.RUSEFI)
        protocol.writeTable(serialized.rpmAxisTableId, serialized.rpmAxisOffset, serialized.rpmAxisData, EcuFamily.RUSEFI)
        protocol.writeTable(serialized.loadAxisTableId, serialized.loadAxisOffset, serialized.loadAxisData, EcuFamily.RUSEFI)
        delay(300)
        serialized.burnTableIds.sorted().forEach { protocol.burnTable(it, EcuFamily.RUSEFI) }
        Logger.d(TAG, "✅ rusEFI AFR Table 1 gravada e burn executado")
    }

    /**
     * Lê dados em tempo real
     * Tenta Modern Protocol ('n') primeiro, fallback para Legacy ('A') se falhar
     */
    suspend fun readLiveData(): SpeeduinoLiveData = withContext(Dispatchers.IO) {
        val ecuFamily = firmwareInfo?.family ?: EcuFamily.UNKNOWN
        val outputSize = ecuDefinition?.runtime?.blockSize ?: tableDefinitions?.ochBlockSize ?: 0
        val isModernEra = firmwareInfo?.era?.isModern() == true
        val canUseModern = ecuFamily == EcuFamily.SPEEDUINO &&
            connection.supportsModernProtocol() &&
            outputSize > 0 &&
            isModernEra

        val data = if (ecuFamily == EcuFamily.RUSEFI) {
            try {
                protocol.readRusefiOutputChannels(outputSize)
            } catch (e: Exception) {
                if (connection.isConnected()) {
                    connection.clearInputBuffer()
                }
                throw e
            }
        } else if (canUseModern) {
            var lastError: Exception? = null
            val maxAttempts = 2
            var attempt = 0
            var modernData: ByteArray? = null

            while (attempt < maxAttempts && modernData == null) {
                attempt++
                try {
                    // ✅ Tenta Modern Protocol primeiro (Speeduino 2020+)
                    modernData = protocol.readLiveDataModern(outputSize)
                } catch (e: Exception) {
                    if (!connection.isConnected()) {
                        Logger.e(TAG, "Conexão perdida durante leitura de live data")
                        throw Exception("Não conectado")
                    }

                    lastError = e
                    Logger.w(TAG, "Modern Protocol falhou (tentativa $attempt/$maxAttempts): ${e.message}")
                    connection.clearInputBuffer()
                    if (attempt < maxAttempts) {
                        delay(25)
                    }
                }
            }

            if (modernData == null) {
                Logger.w(TAG, "Modern Protocol falhou após $maxAttempts tentativas, tentando Legacy: ${lastError?.message}")
                // ⚠️ Fallback para Legacy Protocol (versões antigas)
                protocol.readLiveData(outputSize.takeIf { it > 0 } ?: 128)
            } else {
                modernData
            }
        } else {
            protocol.readLiveData(outputSize.takeIf { it > 0 } ?: 128)
        }

        if (outputSize > 0 && data.size != outputSize) {
            val mode = if (canUseModern) "modern_or_fallback" else "legacy"
            val message = "oc_mismatch family=${ecuFamily.name} mode=$mode expected=$outputSize actual=${data.size}"
            Logger.w(TAG, "⚠ $message")
            ConnectionTrace.info("live_data", message)
        }

        val isModernData = canUseModern && data.size == outputSize
        val liveData = when {
            ecuFamily == EcuFamily.RUSEFI && data.size >= outputSize && outputSize > 0 -> {
                RusefiLiveDataParser.fromOutputChannels(data)
            }
            (ecuFamily == EcuFamily.MS2 || ecuFamily == EcuFamily.MEGASPEED) && data.size >= outputSize && outputSize > 0 -> {
                Ms2LiveDataParser.fromOutputChannels(data)
            }
            ecuFamily == EcuFamily.MS3 && data.size >= outputSize && outputSize > 0 -> {
                Ms3LiveDataParser.fromOutputChannels(data)
            }
            isModernData -> {
            parseOutputChannelsWithFallback(data)
            }
            else -> {
                SpeeduinoLiveDataParser.fromLegacyFrame(data)
            }
        }
        logLiveDataSample(data, liveData, isModernData)
        liveData
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

        if (firmwareInfo?.capabilities?.supportsLiveData == false) {
            Logger.w(TAG, "Live data não suportado para ${firmwareInfo?.family}")
            return
        }

        Logger.d(TAG, "Iniciando live data stream (intervalo: ${intervalMs}ms)")

        // Cancelar job anterior se existir
        streamJob?.cancel()
        _isStreaming = true

        streamJob = scope.launch {
            var packetCount = 0
            var recoverableReadTimeouts = 0
            while (_isStreaming && connection.isConnected()) {
                try {
                    val liveData = readLiveData()
                    recoverableReadTimeouts = 0
                    onDataReceived(liveData)
                    packetCount++

                    // Log a cada 50 pacotes (~5 segundos com intervalo de 100ms)
                    if (packetCount % 50 == 0) {
                        Logger.d(TAG, "Stream ativo: $packetCount pacotes recebidos (RPM: ${liveData.rpm})")
                    }

                    delay(intervalMs)
                } catch (_: CancellationException) {
                    // Stream cancelado por troca de fluxo (pause/restart/disconnect).
                    // Isso não é erro de protocolo/transporte.
                    break
                } catch (e: Exception) {
                    val isRecoverableLiveTimeout =
                        _isStreaming &&
                            connection.isConnected() &&
                            isRecoverableLiveDataTimeout(e)
                    if (isRecoverableLiveTimeout) {
                        recoverableReadTimeouts++
                        ConnectionTrace.info(
                            "live_data",
                            "recoverable_timeout attempt=$recoverableReadTimeouts message=${e.message ?: "unknown"}"
                        )
                        Logger.w(
                            TAG,
                            "Timeout parcial no stream (${recoverableReadTimeouts}/${LIVE_STREAM_RECOVERABLE_TIMEOUT_LIMIT}): ${e.message}"
                        )
                        connection.clearInputBuffer()

                        if (recoverableReadTimeouts < LIVE_STREAM_RECOVERABLE_TIMEOUT_LIMIT) {
                            delay(intervalMs.coerceAtLeast(25L))
                            continue
                        }

                        Logger.e(
                            TAG,
                            "Falha no stream após $recoverableReadTimeouts timeouts parciais consecutivos"
                        )
                    }

                    if (_isStreaming) { // Only report error if still streaming
                        Logger.e(TAG, "Erro no stream: ${e.message}", e)
                        if (packetCount == 0) {
                            ConnectionTrace.error(
                                "live_data",
                                "stream_failed_before_first_packet message=${e.message ?: "unknown"}",
                                e
                            )
                        }
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

    private fun isRecoverableLiveDataTimeout(error: Exception): Boolean {
        val message = error.message ?: return false
        if (message.startsWith("Timeout: no data received")) {
            return true
        }
        val match = PARTIAL_TIMEOUT_REGEX.find(message) ?: return false
        val received = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return false
        return received >= 0
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

    private fun readU8(data: ByteArray, offset: Int): Int = data[offset].toInt() and 0xFF

    private fun readS8(data: ByteArray, offset: Int): Int = data[offset].toInt()

    private fun readU16(data: ByteArray, offset: Int): Int {
        val lsb = readU8(data, offset)
        val msb = readU8(data, offset + 1)
        return lsb or (msb shl 8)
    }

    private fun writeU8(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.coerceIn(0, 255).toByte()
    }

    private fun writeS8(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.coerceIn(-128, 127).toByte()
    }

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        val clamped = value.coerceIn(0, 65535)
        data[offset] = (clamped and 0xFF).toByte()
        data[offset + 1] = ((clamped shr 8) and 0xFF).toByte()
    }

    private fun ensureWritable(operation: String) {
        if (readOnlySafeModeEnabled) {
            throw IllegalStateException(
                "Read-only safe mode enabled ($operation blocked). Disable manual firmware profile to write."
            )
        }
    }

    /**
     * Pausa stream aguardando o ciclo atual finalizar para evitar respostas pendentes.
     */
    suspend fun pauseLiveDataStream(timeoutMs: Long = 6000) {
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
                withTimeoutOrNull(500) {
                    job.join()
                }
            }
        }

        streamJob = null
        connection.clearInputBuffer()
    }

    // ==================== Data Parsing ====================

    /**
     * Parse live data packet (127 bytes)
     * Based on Speeduino logger.cpp getTSLogEntry function
     */
    private fun parseLiveData(data: ByteArray): SpeeduinoLiveData {
        return SpeeduinoLiveDataParser.fromLegacyFrame(data)
    }

    private fun parseOutputChannelsWithFallback(data: ByteArray): SpeeduinoLiveData {
        val parsed = SpeeduinoLiveDataParser.fromOutputChannels(data)
        val score = liveDataOutOfRangeScore(parsed)
        if (score < 2 || data[0].toInt() != 0) {
            return parsed
        }

        val shifted = shiftOutputChannels(data)
        val shiftedParsed = SpeeduinoLiveDataParser.fromOutputChannels(shifted)
        val shiftedScore = liveDataOutOfRangeScore(shiftedParsed)

        return if (shiftedScore < score && shiftedScore <= 1) {
            Logger.w(TAG, "Live data parece desalinhado; aplicando shift de 1 byte (score $score -> $shiftedScore)")
            com.speeduino.manager.connection.ConnectionTrace.info(
                "live_data",
                "shifted output channels by 1 byte (score=$score->$shiftedScore)"
            )
            shiftedParsed
        } else {
            parsed
        }
    }

    private fun shiftOutputChannels(data: ByteArray): ByteArray {
        if (data.isEmpty()) {
            return data
        }
        val shifted = ByteArray(data.size)
        data.copyInto(shifted, 0, 1, data.size)
        shifted[shifted.lastIndex] = 0
        return shifted
    }

    private fun liveDataOutOfRangeScore(data: SpeeduinoLiveData): Int {
        var score = 0
        if (data.rpm < 0 || data.rpm > 20000) score++
        if (data.mapPressure >= 256 && data.mapPressure % 256 == 0) score += 2
        if (data.mapPressure > 500) score++
        if (data.tps < 0 || data.tps > 100) score++
        if (data.batteryVoltage < 6.0 || data.batteryVoltage > 20.0) score++
        if (data.coolantTemp < -40 || data.coolantTemp > 170) score++
        if (data.intakeTemp < -40 || data.intakeTemp > 170) score++
        return score
    }

    private var lastFaultSampleAtMs = 0L

    private fun logLiveDataSample(data: ByteArray, liveData: SpeeduinoLiveData, isModern: Boolean) {
        if (!com.speeduino.manager.connection.ConnectionTrace.enabled) {
            return
        }
        liveDataSampleCounter += 1
        if (liveDataSampleCounter % 25 != 0) {
            return
        }

        val (shouldReport, score) = shouldReportLiveDataIssue(liveData)
        if (!shouldReport) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastFaultSampleAtMs < LIVE_DATA_FAULT_REPORT_INTERVAL_MS) {
            return
        }
        lastFaultSampleAtMs = now

        val hex = data.joinToString(" ") { "%02X".format(it) }
        val message = buildLiveDataFaultMessage(data, liveData, isModern, score, hex)
        com.speeduino.manager.connection.ConnectionTrace.info("live_data", message)
    }

    private fun shouldReportLiveDataIssue(liveData: SpeeduinoLiveData): Pair<Boolean, Int> {
        val score = liveDataOutOfRangeScore(liveData)
        return Pair(score >= 3, score)
    }

    private fun buildLiveDataFaultMessage(
        data: ByteArray,
        liveData: SpeeduinoLiveData,
        isModern: Boolean,
        score: Int,
        hexPayload: String
    ): String {
        val batteryStr = String.format(Locale.US, "%.1f", liveData.batteryVoltage)
        return buildString {
            append("faulty sample score=$score")
            append(" ${if (isModern) "modern" else "legacy"}")
            append(" len=${data.size}")
            append(" rpm=${liveData.rpm}")
            append(" map=${liveData.mapPressure}")
            append(" tps=${liveData.tps}")
            append(" batt=$batteryStr")
            append(" temp=${liveData.coolantTemp}/${liveData.intakeTemp}")
            append(" bytes=$hexPayload")
        }
    }
}

/**
 * Data class para dados em tempo real do Speeduino
 */
data class FirmwareInfo(
    val signature: String,
    val productString: String,
    val era: FirmwareEra,
    val family: EcuFamily = EcuFamily.SPEEDUINO,
    val capabilities: EcuCapabilities = EcuCapabilities(
        supportsModernProtocol = true,
        supportsLegacyProtocol = true,
        supportsPageRead = true,
        supportsPageWrite = true,
        supportsBurn = true,
        supportsLiveData = true
    )
)
