package com.speeduino.manager.desktop

import com.speeduino.manager.ConfigManager
import com.speeduino.manager.SpeeduinoClient
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.definition.IniCatalogEntry
import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.compare.BeforeAfterLogComparator
import com.speeduino.manager.compare.LogCompareException
import com.speeduino.manager.compare.LogCompareReason
import com.speeduino.manager.compare.LogCompareResult
import com.speeduino.manager.connection.ISpeeduinoConnection
import com.speeduino.manager.connection.SpeeduinoSerialConnection
import com.speeduino.manager.connection.SpeeduinoTcpConnection
import com.speeduino.manager.ecu.FirmwareInfo
import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.ClosedLoopCorrectionConfig
import com.speeduino.manager.model.ClosedLoopCorrectionMapper
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.FirmwareEra
import com.speeduino.manager.model.IdleControlSettings
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.RusefiInputOutputSnapshot
import com.speeduino.manager.model.SecondarySerialConfig
import com.speeduino.manager.model.basemap.GeneratedBaseMap
import com.speeduino.manager.model.logging.LiveLogRecorder
import com.speeduino.manager.model.logging.LiveLogSnapshot
import com.speeduino.manager.sync.ConfigSyncService
import com.speeduino.manager.sync.SessionSyncPrompt
import com.speeduino.manager.tuning.AnalyzerResult
import com.speeduino.manager.tuning.TuningAssistantAnalyzer
import com.speeduino.manager.tuning.TuningStrategy
import com.speeduino.manager.telemetry.DiagnosticsFlags
import com.speeduino.manager.telemetry.Obd2InvestigationRecorder
import com.speeduino.manager.transport.AutoDetectEcuTransport
import com.speeduino.manager.transport.EcuTransport
import com.speeduino.manager.transport.Obd2OptimizationProfileStore
import com.speeduino.manager.transport.Obd2Transport
import com.speeduino.manager.transport.PsaConnectionSessionStore
import com.speeduino.manager.transport.PsaTransport
import com.speeduino.manager.transport.PromotingObd2Transport
import com.speeduino.manager.transport.RenaultTransport
import com.speeduino.manager.transport.VagTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal class DesktopSpeeduinoController(
    private val scope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    private val _liveData = MutableStateFlow<SpeeduinoLiveData?>(null)
    val liveData = _liveData.asStateFlow()

    private val _engineConstants = MutableStateFlow<EngineConstants?>(null)
    val engineConstants = _engineConstants.asStateFlow()

    private val _triggerSettings = MutableStateFlow<TriggerSettings?>(null)
    val triggerSettings = _triggerSettings.asStateFlow()

    private val _veTable = MutableStateFlow<VeTable?>(null)
    val veTable = _veTable.asStateFlow()
    private val _veTable2 = MutableStateFlow<VeTable?>(null)
    val veTable2 = _veTable2.asStateFlow()

    private val _ignitionTable = MutableStateFlow<IgnitionTable?>(null)
    val ignitionTable = _ignitionTable.asStateFlow()
    private val _ignitionTable2 = MutableStateFlow<IgnitionTable?>(null)
    val ignitionTable2 = _ignitionTable2.asStateFlow()

    private val _afrTable = MutableStateFlow<AfrTable?>(null)
    val afrTable = _afrTable.asStateFlow()

    private val _idleControlSettings = MutableStateFlow<IdleControlSettings?>(null)
    val idleControlSettings = _idleControlSettings.asStateFlow()

    private val _closedLoopCorrections = MutableStateFlow<ClosedLoopCorrectionConfig?>(null)
    val closedLoopCorrections = _closedLoopCorrections.asStateFlow()

    private val _tuningConfigState = MutableStateFlow(TuningConfigState())
    val tuningConfigState = _tuningConfigState.asStateFlow()

    private val _firmwareInfo = MutableStateFlow<FirmwareInfo?>(null)
    val firmwareInfo = _firmwareInfo.asStateFlow()

    private val _productString = MutableStateFlow<String?>(null)
    val productString = _productString.asStateFlow()

    private val _connectionInfo = MutableStateFlow<String?>(null)
    val connectionInfo = _connectionInfo.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val _desktopSettings = MutableStateFlow(DesktopSettingsStore.loadSettings())
    val desktopSettings = _desktopSettings.asStateFlow()

    private val _availableIniDefinitions = MutableStateFlow<List<IniCatalogEntry>>(emptyList())
    val availableIniDefinitions = _availableIniDefinitions.asStateFlow()

    private val _importedIniDefinitions = MutableStateFlow<List<ImportedIniDefinition>>(emptyList())
    val importedIniDefinitions = _importedIniDefinitions.asStateFlow()

    private val _activeIniDefinition = MutableStateFlow<IniDefinition?>(null)
    val activeIniDefinition = _activeIniDefinition.asStateFlow()

    private val _activeIniCatalogEntry = MutableStateFlow<IniCatalogEntry?>(null)
    val activeIniCatalogEntry = _activeIniCatalogEntry.asStateFlow()

    private val _readOnlySafeMode = MutableStateFlow(false)
    val readOnlySafeMode = _readOnlySafeMode.asStateFlow()

    private val _serialPorts = MutableStateFlow<List<SerialPortInfo>>(emptyList())
    val serialPorts = _serialPorts.asStateFlow()

    private val _streamIntervalMs = MutableStateFlow(120L)
    val streamIntervalMs = _streamIntervalMs.asStateFlow()

    private val logRecorder = LiveLogRecorder()
    val logState = logRecorder.state
    private val _logSnapshot = MutableStateFlow<LiveLogSnapshot?>(null)
    val logSnapshot = _logSnapshot.asStateFlow()
    private val _lastSavedLogPath = MutableStateFlow<String?>(null)
    val lastSavedLogPath = _lastSavedLogPath.asStateFlow()
    private val _logSaveStatus = MutableStateFlow<String?>(null)
    val logSaveStatus = _logSaveStatus.asStateFlow()
    private val _analyzerLogFile = MutableStateFlow<String?>(null)
    val analyzerLogFile = _analyzerLogFile.asStateFlow()
    private val _analyzerResult = MutableStateFlow<AnalyzerResult?>(null)
    val analyzerResult = _analyzerResult.asStateFlow()
    private val _analyzerBusy = MutableStateFlow(false)
    val analyzerBusy = _analyzerBusy.asStateFlow()
    private val _analyzerError = MutableStateFlow<String?>(null)
    val analyzerError = _analyzerError.asStateFlow()
    private val _analyzerUndoVeTable = MutableStateFlow<VeTable?>(null)
    val analyzerUndoAvailable = _analyzerUndoVeTable.asStateFlow()
    private val _beforeAfterBeforeLogPath = MutableStateFlow<String?>(null)
    val beforeAfterBeforeLogPath = _beforeAfterBeforeLogPath.asStateFlow()
    private val _beforeAfterAfterLogPath = MutableStateFlow<String?>(null)
    val beforeAfterAfterLogPath = _beforeAfterAfterLogPath.asStateFlow()
    private val _beforeAfterResult = MutableStateFlow<LogCompareResult?>(null)
    val beforeAfterResult = _beforeAfterResult.asStateFlow()
    private val _beforeAfterBusy = MutableStateFlow(false)
    val beforeAfterBusy = _beforeAfterBusy.asStateFlow()
    private val _beforeAfterError = MutableStateFlow<String?>(null)
    val beforeAfterError = _beforeAfterError.asStateFlow()

    private val configManager = ConfigManager()
    private val _configState = MutableStateFlow(ConfigSyncState())
    val configState = _configState.asStateFlow()
    private val _syncPrompt = MutableStateFlow<SyncPrompt?>(null)
    val syncPrompt = _syncPrompt.asStateFlow()

    private var pollingJob: Job? = null
    private var connection: ISpeeduinoConnection? = null
    private var client: EcuTransport? = null
    private var localSessionDir: File? = null
    private var ecuSessionDir: File? = null
    private val beforeAfterComparator = BeforeAfterLogComparator()
    private val syncService = ConfigSyncService(configManager)
    private val definitionRepository = DesktopDefinitionRepository()

    init {
        refreshIniDefinitions()
        maybeAutoConnect()
    }

    fun connectTcp(host: String, port: Int) {
        saveConnectionProfile(
            connectionType = ConnectionType.TCP,
            tcpHost = host,
            tcpPort = port
        )
        connectInternal(SpeeduinoTcpConnection(host, port))
    }

    fun connectSerial(portDescriptor: String, baudRate: Int, connectionType: ConnectionType = ConnectionType.USB) {
        saveConnectionProfile(
            connectionType = connectionType,
            serialPort = portDescriptor,
            serialBaudRate = baudRate
        )
        connectInternal(SpeeduinoSerialConnection(portDescriptor, baudRate))
    }

    fun saveConnectionProfile(
        connectionType: ConnectionType,
        tcpHost: String? = null,
        tcpPort: Int? = null,
        serialPort: String? = null,
        serialBaudRate: Int? = null,
    ) {
        val current = _desktopSettings.value
        saveDesktopSettings(
            current.copy(
                lastConnectionType = connectionType,
                lastTcpHost = tcpHost ?: current.lastTcpHost,
                lastTcpPort = tcpPort ?: current.lastTcpPort,
                lastSerialPort = serialPort ?: current.lastSerialPort,
                lastSerialBaudRate = serialBaudRate ?: current.lastSerialBaudRate,
            )
        )
    }

    private fun maybeAutoConnect() {
        val settings = _desktopSettings.value
        if (!settings.autoConnectOnStart || _connectionState.value.isConnected) {
            return
        }

        when (settings.lastConnectionType) {
            ConnectionType.TCP -> {
                val host = settings.lastTcpHost
                val port = settings.lastTcpPort
                if (!host.isNullOrBlank() && port != null) {
                    connectTcp(host, port)
                }
            }
            ConnectionType.USB,
            ConnectionType.BLUETOOTH -> {
                val portDescriptor = settings.lastSerialPort
                val baudRate = settings.lastSerialBaudRate
                if (!portDescriptor.isNullOrBlank() && baudRate != null) {
                    connectSerial(
                        portDescriptor = portDescriptor,
                        baudRate = baudRate,
                        connectionType = settings.lastConnectionType
                    )
                }
            }
            null -> Unit
        }
    }

    private fun connectInternal(newConnection: ISpeeduinoConnection) {
        disconnect()

        connection = newConnection
        client = createTransport(checkNotNull(connection))
        _connectionState.value = ConnectionState(ConnectionStatus.Connecting)

        pollingJob = scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client ?: return@launch
                val settings = _desktopSettings.value
                if (!settings.manualFirmwareProfile.isNullOrBlank()) {
                    activeClient.setManualFirmwareProfile(settings.manualFirmwareProfile, readOnly = true)
                } else {
                    activeClient.clearManualFirmwareProfile()
                }

                activeClient.connect()
                _firmwareInfo.value = activeClient.getFirmwareInfoCached()
                _productString.value = activeClient.getProductString()
                _connectionInfo.value = activeClient.getConnectionInfo()
                _readOnlySafeMode.value = activeClient.isReadOnlySafeMode()
                applyConfiguredIniDefinition(activeClient)
                activeClient.startLiveDataStream(_streamIntervalMs.value)
                downloadAllConfigs(autoRestartStream = true)
            } catch (e: Exception) {
                _lastError.value = e.message
                _connectionState.value = ConnectionState(
                    status = ConnectionStatus.Failed,
                    detail = e.message
                )
                disconnect()
                }
        }
    }

    private fun createTransport(connection: ISpeeduinoConnection): EcuTransport {
        val callbacks = transportCallbacks()
        val profileStore = Obd2OptimizationProfileStore()
        val sessionStore = PsaConnectionSessionStore()
        val investigationRecorder = Obd2InvestigationRecorder()
        return when (_desktopSettings.value.protocol) {
            AppProtocol.ELM327_OBD2 -> {
                val obd2Transport = Obd2Transport(
                    connection = connection,
                    onDataReceived = callbacks.onDataReceived,
                    onConnectionStateChanged = callbacks.onConnectionStateChanged,
                    onError = callbacks.onError,
                    profileStore = profileStore,
                    investigationRecorder = investigationRecorder,
                )
                val psaTransport = PsaTransport(
                    connection = connection,
                    onDataReceived = callbacks.onDataReceived,
                    onConnectionStateChanged = callbacks.onConnectionStateChanged,
                    onError = callbacks.onError,
                    obd2ProfileStore = profileStore,
                    sessionStore = sessionStore,
                    investigationRecorder = investigationRecorder,
                    enableInvestigationCampaign = DiagnosticsFlags.ENABLE_ECU_INVESTIGATION,
                )
                val renaultTransport = RenaultTransport(
                    connection = connection,
                    onDataReceived = callbacks.onDataReceived,
                    onConnectionStateChanged = callbacks.onConnectionStateChanged,
                    onError = callbacks.onError,
                    obd2ProfileStore = profileStore,
                    investigationRecorder = investigationRecorder,
                )
                val vagTransport = VagTransport(
                    connection = connection,
                    onDataReceived = callbacks.onDataReceived,
                    onConnectionStateChanged = callbacks.onConnectionStateChanged,
                    onError = callbacks.onError,
                    investigationRecorder = investigationRecorder,
                )
                val promotingObd2Transport = PromotingObd2Transport(
                    genericTransport = obd2Transport,
                    psaTransport = psaTransport,
                )
                val renaultPsaTransport = AutoDetectEcuTransport(
                    primaryTransport = renaultTransport,
                    obd2FallbackTransport = psaTransport,
                )
                val vagRenaultPsaTransport = AutoDetectEcuTransport(
                    primaryTransport = vagTransport,
                    obd2FallbackTransport = renaultPsaTransport,
                )
                AutoDetectEcuTransport(
                    primaryTransport = promotingObd2Transport,
                    obd2FallbackTransport = vagRenaultPsaTransport,
                )
            }
            AppProtocol.MS_PROTOCOL -> SpeeduinoClient(
                connection = connection,
                onDataReceived = callbacks.onDataReceived,
                onConnectionStateChanged = callbacks.onConnectionStateChanged,
                onError = callbacks.onError,
            )
        }
    }

    private fun transportCallbacks(): TransportCallbacks {
        return TransportCallbacks(
            onDataReceived = { data ->
                _liveData.value = data
                if (logRecorder.state.value.isRecording) {
                    logRecorder.record(data)
                }
            },
            onConnectionStateChanged = { isConnected ->
                _connectionState.value = if (isConnected) {
                    ConnectionState(ConnectionStatus.Connected)
                } else {
                    ConnectionState(ConnectionStatus.Disconnected)
                }
            },
            onError = { error ->
                _lastError.value = error
            }
        )
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        client?.stopLiveDataStream()
        client?.disconnect()
        client = null
        connection?.disconnect()
        connection = null
        _liveData.value = null
        if (localSessionDir == null) {
            _engineConstants.value = null
            _triggerSettings.value = null
            _veTable.value = null
            _veTable2.value = null
            _ignitionTable.value = null
            _ignitionTable2.value = null
            _afrTable.value = null
            _idleControlSettings.value = null
            _closedLoopCorrections.value = null
        }
        _syncPrompt.value = null
        _readOnlySafeMode.value = false
        _activeIniDefinition.value = null
        _activeIniCatalogEntry.value = null
        if (_connectionState.value.isConnected) {
            _connectionState.value = ConnectionState(ConnectionStatus.Disconnected)
        }
    }

    fun dismissSyncPrompt() {
        _syncPrompt.value = null
    }

    fun chooseSyncSource(useLocal: Boolean) {
        scope.launch(Dispatchers.IO) {
            val prompt = _syncPrompt.value ?: return@launch
            _syncPrompt.value = null
            if (useLocal) {
                _configState.value = _configState.value.copy(
                    isBusy = true,
                    message = "Restaurando sessão local..."
                )
                try {
                    syncService.restoreConfigToEcu(
                        client = client ?: throw IllegalStateException("ECU não conectada"),
                        sessionDir = prompt.localSessionDir,
                        stopOnRangeErr = true,
                        restartStreamIntervalMs = _streamIntervalMs.value,
                    )
                    ecuSessionDir = prompt.localSessionDir
                    localSessionDir = prompt.localSessionDir
                    loadTablesFromSession(prompt.localSessionDir)
                    _configState.value = _configState.value.copy(
                        isBusy = false,
                        message = "Sessão local aplicada na ECU."
                    )
                } catch (e: Exception) {
                    _configState.value = _configState.value.copy(
                        isBusy = false,
                        message = "Falha ao restaurar sessão local: ${e.message}"
                    )
                }
            } else {
                ecuSessionDir = prompt.ecuSessionDir
                localSessionDir = prompt.ecuSessionDir
                loadTablesFromSession(prompt.ecuSessionDir)
                _configState.value = _configState.value.copy(
                    isBusy = false,
                    message = "Sessão da ECU carregada."
                )
            }
        }
    }

    fun updateStreamInterval(intervalMs: Long) {
        _streamIntervalMs.value = intervalMs
        if (connectionState.value.isConnected) {
            scope.launch(Dispatchers.IO) {
                client?.stopLiveDataStream()
                client?.startLiveDataStream(intervalMs)
            }
        }
    }

    fun refreshSerialPorts() {
        val ports = SpeeduinoSerialConnection.listPorts()
        _serialPorts.value = ports.map { port ->
            SerialPortInfo(port, port)
        }
    }

    fun saveDesktopSettings(settings: DesktopSettingsState) {
        _desktopSettings.value = settings
        DesktopSettingsStore.saveSettings(settings)
    }

    fun refreshIniDefinitions(forceCatalogRefresh: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            val catalog = runCatching {
                definitionRepository.loadCatalog(forceRefresh = forceCatalogRefresh)
            }.getOrDefault(emptyList())
            val imported = runCatching {
                definitionRepository.listImportedDefinitions()
            }.getOrDefault(emptyList())
            _availableIniDefinitions.value = catalog
            _importedIniDefinitions.value = imported
        }
    }

    fun importIniDefinition(sourceFile: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val definition = definitionRepository.importDefinition(sourceFile)
                _importedIniDefinitions.value = definitionRepository.listImportedDefinitions()
                val updatedSettings = _desktopSettings.value.copy(
                    iniSelectionMode = IniSelectionMode.MANUAL,
                    iniSelectionSource = IniSelectionSource.IMPORTED,
                    iniDefinitionId = definition.sourceName,
                )
                saveDesktopSettings(updatedSettings)
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadEngineConstants() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _engineConstants.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readEngineConstants()
                } else {
                    localSessionDir?.let { configManager.loadEngineConstants(it) }
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveEngineConstants(constants: EngineConstants) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeEngineConstants(constants)
                }
                updateLocalEngineConstants(constants)
                _engineConstants.value = constants
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadTriggerSettings() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _triggerSettings.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readTriggerSettings()
                } else {
                    localSessionDir?.let { configManager.loadTriggerSettings(it) }
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveTriggerSettings(settings: TriggerSettings) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeTriggerSettings(settings)
                }
                updateLocalTriggerSettings(settings)
                _triggerSettings.value = settings
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun veTableState(mapIndex: Int): StateFlow<VeTable?> = if (mapIndex == 2) veTable2 else veTable

    fun ignitionTableState(mapIndex: Int): StateFlow<IgnitionTable?> = if (mapIndex == 2) ignitionTable2 else ignitionTable

    fun loadVeTable(mapIndex: Int = 1) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                val table = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readVeTable(mapIndex)
                } else {
                    localSessionDir?.let { configManager.loadVeTable(it, mapIndex) }
                }
                if (mapIndex == 2) _veTable2.value = table else _veTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveVeTable(table: VeTable, mapIndex: Int = 1) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeVeTable(table, mapIndex)
                }
                updateLocalVeTable(table, mapIndex)
                if (mapIndex == 2) _veTable2.value = table else _veTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadIgnitionTable(mapIndex: Int = 1) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                val table = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readIgnitionTable(mapIndex)
                } else {
                    localSessionDir?.let { configManager.loadIgnitionTable(it, mapIndex) }
                }
                if (mapIndex == 2) _ignitionTable2.value = table else _ignitionTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveIgnitionTable(table: IgnitionTable, mapIndex: Int = 1) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeIgnitionTable(table, mapIndex)
                }
                updateLocalIgnitionTable(table, mapIndex)
                if (mapIndex == 2) _ignitionTable2.value = table else _ignitionTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadAfrTable() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _afrTable.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readAfrTable()
                } else {
                    localSessionDir?.let { configManager.loadAfrTable(it) }
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveAfrTable(table: AfrTable) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeAfrTable(table)
                }
                updateLocalAfrTable(table)
                _afrTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadIdleControlSettings() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _idleControlSettings.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readIdleControlSettings()
                } else {
                    loadIdleControlSettingsFromSession(localSessionDir)
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveIdleControlSettings(settings: IdleControlSettings) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeIdleControlSettings(settings)
                }
                updateLocalIdleControlSettings(settings)
                _idleControlSettings.value = settings
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadClosedLoopCorrections() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _closedLoopCorrections.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readClosedLoopCorrectionConfig()
                } else {
                    loadClosedLoopCorrectionsFromSession(localSessionDir)
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveClosedLoopCorrections(config: ClosedLoopCorrectionConfig) {
        scope.launch(Dispatchers.IO) {
            try {
                val normalized = ClosedLoopCorrectionMapper.syncFromAfr(config)
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeClosedLoopCorrectionConfig(normalized)
                }
                updateLocalClosedLoopCorrections(normalized)
                _closedLoopCorrections.value = normalized
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadRusefiInputOutputSnapshot() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    _tuningConfigState.value = _tuningConfigState.value.copy(
                        rusefiSnapshot = activeClient.readRusefiInputOutputSnapshot()
                    )
                } else {
                    _lastError.value = "RuseFI input/output snapshot requires an active connection."
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadSecondarySerialConfig() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    _tuningConfigState.value = _tuningConfigState.value.copy(
                        secondarySerialConfig = activeClient.readSecondarySerialConfig()
                    )
                } else {
                    _lastError.value = "Secondary serial config requires an active connection."
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveSecondarySerialConfig(config: SecondarySerialConfig) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeSecondarySerialConfig(config, burn = true)
                }
                _tuningConfigState.value = _tuningConfigState.value.copy(secondarySerialConfig = config)
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun downloadAllConfigs() {
        scope.launch(Dispatchers.IO) {
            downloadAllConfigs(autoRestartStream = true)
        }
    }

    fun exportLatestConfig(targetFile: File) {
        scope.launch(Dispatchers.IO) {
            val sessionDir = localSessionDir ?: configManager.latestSavedConfig()
            if (sessionDir == null) {
                _configState.value = _configState.value.copy(
                    isBusy = false,
                    message = "Nenhuma sessão salva para exportar."
                )
                return@launch
            }
            _configState.value = _configState.value.copy(
                isBusy = true,
                message = "Exportando backup..."
            )
            try {
                FileOutputStream(targetFile).use { output ->
                    configManager.exportSessionToZip(sessionDir, output)
                }
                _configState.value = _configState.value.copy(
                    isBusy = false,
                    message = "Backup exportado: ${targetFile.name}",
                    lastSessionDir = sessionDir
                )
            } catch (e: Exception) {
                _configState.value = _configState.value.copy(
                    isBusy = false,
                    message = "Erro ao exportar: ${e.message}"
                )
            }
        }
    }

    fun importConfigAndRestore(sourceFile: File) {
        scope.launch(Dispatchers.IO) {
            _configState.value = _configState.value.copy(
                isBusy = true,
                message = "Importando backup..."
            )
            try {
                val sessionDir = FileInputStream(sourceFile).use { input ->
                    configManager.importSessionFromZip(input)
                }
                localSessionDir = sessionDir
                loadTablesFromSession(sessionDir)
                val warningText = "Sessão importada. Conecte para sincronizar."
                _configState.value = _configState.value.copy(
                    isBusy = false,
                    message = warningText,
                    lastSessionDir = sessionDir
                )
            } catch (e: Exception) {
                _configState.value = _configState.value.copy(
                    isBusy = false,
                    message = "Erro ao importar: ${e.message}"
                )
            }
        }
    }

    private suspend fun downloadAllConfigs(autoRestartStream: Boolean): Boolean = withContext(Dispatchers.IO) {
        val activeClient = client
        if (activeClient == null || !_connectionState.value.isConnected) {
            _configState.value = _configState.value.copy(
                isBusy = false,
                message = "ECU não conectada."
            )
            return@withContext false
        }

        _configState.value = _configState.value.copy(
            isBusy = true,
            progressPercent = 0,
            message = "Iniciando download..."
        )

        val (result, decision) = syncService.downloadAndResolveSync(activeClient, localSessionDir) { current, total, message ->
            val progress = if (total > 0) (current * 100) / total else 0
            _configState.value = _configState.value.copy(
                progressPercent = progress,
                message = message
            )
        }

        if (result.success) {
            val sessionDir = result.sessionDir
            ecuSessionDir = sessionDir
            _configState.value = _configState.value.copy(
                isBusy = false,
                progressPercent = 100,
                message = "Download concluído.",
                lastSessionDir = sessionDir
            )
            val localDir = localSessionDir
            val resolvedSession = decision.sessionDir
            if (resolvedSession != null) {
                localSessionDir = resolvedSession
                loadTablesFromSession(resolvedSession)
            } else if (localDir != null) {
                val prompt = decision.prompt
                if (prompt != null) {
                    _syncPrompt.value = prompt.toDesktop()
                }
            }
        } else {
            _configState.value = _configState.value.copy(
                isBusy = false,
                message = "Erro: ${result.error}"
            )
        }

        return@withContext result.success
    }

    private suspend fun loadTablesFromSession(sessionDir: File) {
        val snapshot = syncService.loadTablesFromSession(sessionDir)
        _veTable.value = snapshot.veTable
        _veTable2.value = snapshot.veTable2
        _ignitionTable.value = snapshot.ignitionTable
        _ignitionTable2.value = snapshot.ignitionTable2
        _afrTable.value = snapshot.afrTable
        _engineConstants.value = snapshot.engineConstants
        _triggerSettings.value = snapshot.triggerSettings
        _idleControlSettings.value = loadIdleControlSettingsFromSession(sessionDir)
        _closedLoopCorrections.value = loadClosedLoopCorrectionsFromSession(sessionDir)
    }

    private fun ensureLocalSessionDir(): File? {
        if (localSessionDir == null) {
            localSessionDir = ecuSessionDir
        }
        return localSessionDir
    }

    private fun updateLocalEngineConstants(constants: EngineConstants) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, "page_1.bin")
        val basePage = if (pageFile.exists() && pageFile.length() >= 128) {
            pageFile.readBytes()
        } else {
            ByteArray(128)
        }
        val data = constants.applyToPage1(basePage)
        pageFile.writeBytes(data)
    }

    private fun updateLocalTriggerSettings(settings: TriggerSettings) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, "page_${TriggerSettings.PAGE_NUMBER}.bin")
        val basePage = if (pageFile.exists() && pageFile.length() >= TriggerSettings.PAGE_LENGTH) {
            pageFile.readBytes()
        } else {
            ByteArray(TriggerSettings.PAGE_LENGTH)
        }
        val data = settings.toPageData(basePage)
        pageFile.writeBytes(data)
    }

    private fun updateLocalIdleControlSettings(settings: IdleControlSettings) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val page4File = File(sessionDir, "page_${IdleControlSettings.PAGE_NUMBER}.bin")
        val page7File = File(sessionDir, "page_${IdleControlSettings.TARGET_PAGE_NUMBER}.bin")
        val basePage4 = if (page4File.exists() && page4File.length() >= IdleControlSettings.PAGE_LENGTH) {
            page4File.readBytes()
        } else {
            ByteArray(IdleControlSettings.PAGE_LENGTH)
        }
        val basePage7 = if (page7File.exists() && page7File.length() >= IdleControlSettings.TARGET_PAGE_LENGTH) {
            page7File.readBytes()
        } else {
            ByteArray(IdleControlSettings.TARGET_PAGE_LENGTH)
        }
        page4File.writeBytes(settings.applyToPage4(basePage4))
        page7File.writeBytes(settings.applyTargetRpmToPage7(basePage7))
    }

    private fun updateLocalClosedLoopCorrections(config: ClosedLoopCorrectionConfig) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, "page_${ClosedLoopCorrectionMapper.PAGE_NUMBER}.bin")
        val basePage = if (pageFile.exists() && pageFile.length() >= ClosedLoopCorrectionMapper.PAGE_SIZE) {
            pageFile.readBytes()
        } else {
            ByteArray(ClosedLoopCorrectionMapper.PAGE_SIZE)
        }
        val era = _firmwareInfo.value?.era ?: FirmwareEra.MODERN_2025
        pageFile.writeBytes(ClosedLoopCorrectionMapper.applyToPage(basePage, config, era))
    }

    private fun updateLocalVeTable(table: VeTable, mapIndex: Int = 1) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, if (mapIndex == 2) "page_7.bin" else "page_2.bin")
        val format = resolveTableFormat(pageFile, VeTable.StorageFormat.MODERN_288, VeTable.StorageFormat.LEGACY_304)
        pageFile.writeBytes(table.toByteArray(format))
    }

    private fun updateLocalIgnitionTable(table: IgnitionTable, mapIndex: Int = 1) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, if (mapIndex == 2) "page_10.bin" else "page_3.bin")
        val format = resolveTableFormat(
            pageFile,
            IgnitionTable.StorageFormat.MODERN_288,
            IgnitionTable.StorageFormat.LEGACY_304
        )
        pageFile.writeBytes(table.toByteArray(format))
    }

    private fun updateLocalAfrTable(table: AfrTable) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, "page_5.bin")
        val format = resolveTableFormat(
            pageFile,
            AfrTable.StorageFormat.MODERN_288,
            AfrTable.StorageFormat.LEGACY_304
        )
        pageFile.writeBytes(table.toByteArray(format))
    }

    private fun <T> resolveTableFormat(
        pageFile: File,
        modern: T,
        legacy: T
    ): T {
        if (!pageFile.exists()) {
            return modern
        }
        val size = pageFile.length().toInt()
        val legacySize = when (legacy) {
            is VeTable.StorageFormat -> legacy.totalSize
            is IgnitionTable.StorageFormat -> legacy.totalSize
            is AfrTable.StorageFormat -> legacy.totalSize
            else -> 0
        }
        return if (size >= legacySize) legacy else modern
    }

    fun startLogCapture(intervalMs: Long) {
        logRecorder.start(intervalMs)
    }

    fun stopLogCapture() {
        logRecorder.stop()
        _logSnapshot.value = logRecorder.snapshot()
    }

    fun captureSnapshot() {
        _logSnapshot.value = logRecorder.snapshot()
    }

    fun saveLogSnapshot(fileName: String, selectedSignalKeys: Set<String>) {
        scope.launch(Dispatchers.IO) {
            val strings = LocalizationManager.currentStrings()
            try {
                val snapshot = logRecorder.snapshot()
                if (snapshot == null || snapshot.entries.isEmpty()) {
                    _logSaveStatus.value = strings["label.logSaveNoData"]
                    return@launch
                }

                val rawName = fileName.ifBlank { strings["label.logFilenamePrefix"] }
                val sanitizedName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val targetDir = java.nio.file.Paths.get(
                    System.getProperty("user.home"),
                    "SpeeduinoManagerDesktop",
                    "logs"
                )
                java.nio.file.Files.createDirectories(targetDir)
                val baseFile = targetDir.resolve(strings.format("label.logFilenameSuffix", sanitizedName)).toFile()
                val targetFile = uniqueFile(baseFile)

                val selected = selectedSignalKeys.ifEmpty { DefaultSelectedLogSignals }
                val activeSignals = LogExportSignals.filter { selected.contains(it.key) }
                val header = buildList {
                    add("timestamp_ms")
                    activeSignals.forEach { add(it.header) }
                }

                targetFile.bufferedWriter().use { writer ->
                    writer.appendLine(header.joinToString(","))
                    snapshot.entries.forEach { entry ->
                        val row = mutableListOf<String>()
                        row += entry.timestampMs.toString()
                        activeSignals.forEach { signal ->
                            row += when (signal.key) {
                                "rpm" -> entry.rpm.toString()
                                "map" -> entry.mapKpa.toString()
                                "tps" -> entry.tps.toString()
                                "coolant" -> entry.coolantTempC.toString()
                                "iat" -> entry.intakeTempC.toString()
                                "battery" -> (entry.batteryDeciVolt / 10.0).toString()
                                "advance" -> entry.advanceDeg.toString()
                                "afr" -> entry.o2.toString()
                                "afr_target" -> ""
                                "candidate_speed" -> entry.candidateSpeedKph?.toString() ?: ""
                                "candidate_pedal" -> entry.candidateAccelPedalPosPct?.toString() ?: ""
                                "candidate_gear" -> entry.candidateGear?.toString() ?: ""
                                "candidate_throttle_angle" -> entry.candidateThrottleAngleDeg?.toString() ?: ""
                                "candidate_ignition_advance" -> entry.candidateIgnitionAdvanceDeg?.toString() ?: ""
                                "candidate_injection_ms" -> entry.candidateInjectionDurationMs?.toString() ?: ""
                                "candidate_injection_mirror_ms" -> entry.candidateInjectionDurationMirrorMs?.toString() ?: ""
                                else -> ""
                            }
                        }
                        writer.appendLine(row.joinToString(","))
                    }
                }

                _lastSavedLogPath.value = targetFile.absolutePath
                _analyzerLogFile.value = targetFile.absolutePath
                _logSaveStatus.value = strings.format("label.logSaveSuccess", targetFile.absolutePath)
            } catch (e: Exception) {
                _logSaveStatus.value = strings.format("label.logSaveError", e.message ?: "unknown")
            }
        }
    }

    private fun uniqueFile(baseFile: File): File {
        if (!baseFile.exists()) return baseFile
        val name = baseFile.nameWithoutExtension
        val ext = baseFile.extension
        var index = 1
        while (true) {
            val candidateName = if (ext.isBlank()) "${name}_$index" else "${name}_$index.$ext"
            val candidate = File(baseFile.parentFile, candidateName)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun selectAnalyzerLogFile(path: String?) {
        _analyzerLogFile.value = path
        _analyzerResult.value = null
        _analyzerError.value = null
    }

    fun setBeforeAfterBeforeLogPath(path: String?) {
        _beforeAfterBeforeLogPath.value = path
        _beforeAfterResult.value = null
        _beforeAfterError.value = null
    }

    fun setBeforeAfterAfterLogPath(path: String?) {
        _beforeAfterAfterLogPath.value = path
        _beforeAfterResult.value = null
        _beforeAfterError.value = null
    }

    fun compareBeforeAfterLogs() {
        scope.launch(Dispatchers.IO) {
            val beforePath = _beforeAfterBeforeLogPath.value
            val afterPath = _beforeAfterAfterLogPath.value
            val ve = _veTable.value
            if (beforePath.isNullOrBlank() || afterPath.isNullOrBlank() || ve == null) {
                _beforeAfterError.value = "Load VE and select before/after logs first."
                return@launch
            }
            _beforeAfterBusy.value = true
            _beforeAfterError.value = null
            try {
                _beforeAfterResult.value = beforeAfterComparator.compareLogs(
                    beforePath = beforePath,
                    afterPath = afterPath,
                    rpmBins = ve.rpmBins,
                    loadBins = ve.loadBins,
                    preferredLoadType = ve.loadType
                )
            } catch (e: Exception) {
                _beforeAfterResult.value = null
                _beforeAfterError.value = when ((e as? LogCompareException)?.reason) {
                    LogCompareReason.EMPTY_LOG -> "Empty CSV."
                    LogCompareReason.MISSING_RPM -> "Missing RPM column."
                    LogCompareReason.MISSING_AFR_MEASURED -> "Missing AFR measured column."
                    LogCompareReason.MISSING_AFR_TARGET -> "Missing AFR target column."
                    LogCompareReason.MISSING_LOAD_CHANNEL -> "Missing MAP/TPS load column."
                    LogCompareReason.NO_VALID_SAMPLES -> "No valid samples in CSV."
                    null -> e.message ?: "Failed to compare logs."
                }
            } finally {
                _beforeAfterBusy.value = false
            }
        }
    }

    fun analyzeLogFile(strategy: TuningStrategy) {
        scope.launch(Dispatchers.IO) {
            val logPath = _analyzerLogFile.value
            val ve = _veTable.value
            val afr = _afrTable.value
            if (logPath.isNullOrBlank() || ve == null || afr == null) {
                _analyzerError.value = "Load VE, AFR and a log file first."
                return@launch
            }

            _analyzerBusy.value = true
            _analyzerError.value = null
            try {
                val result = File(logPath).useLines { lines ->
                    TuningAssistantAnalyzer.analyzeLines(
                        logName = File(logPath).name,
                        lines = lines,
                        veTable = ve,
                        afrTable = afr,
                        strategy = strategy
                    )
                }
                _analyzerResult.value = result
            } catch (e: Exception) {
                _analyzerError.value = e.message
                _analyzerResult.value = null
            } finally {
                _analyzerBusy.value = false
            }
        }
    }

    fun applyAnalyzerToVe(strategy: TuningStrategy, includedClusterIds: Set<String>) {
        scope.launch(Dispatchers.IO) {
            val ve = _veTable.value ?: return@launch
            val result = _analyzerResult.value ?: return@launch
            val logPath = _analyzerLogFile.value
            val afr = _afrTable.value
            _analyzerBusy.value = true
            _analyzerError.value = null
            try {
                _analyzerUndoVeTable.value = ve
                val allClusterIds = result.clusters.map { it.id }.toSet()
                val updated = TuningAssistantAnalyzer.applyClustersToVe(
                    veTable = ve,
                    suggestions = result.cellSuggestions,
                    includedClusterIds = if (includedClusterIds.isEmpty()) allClusterIds else includedClusterIds,
                    clusters = result.clusters
                )
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeVeTable(updated)
                }
                updateLocalVeTable(updated)
                _veTable.value = updated
                if (!logPath.isNullOrBlank() && afr != null) {
                    _analyzerResult.value = File(logPath).useLines { lines ->
                        TuningAssistantAnalyzer.analyzeLines(
                            logName = File(logPath).name,
                            lines = lines,
                            veTable = updated,
                            afrTable = afr,
                            strategy = strategy
                        )
                    }
                }
            } catch (e: Exception) {
                _analyzerError.value = e.message
            } finally {
                _analyzerBusy.value = false
            }
        }
    }

    fun undoLastAnalyzerApply() {
        scope.launch(Dispatchers.IO) {
            val previous = _analyzerUndoVeTable.value ?: return@launch
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeVeTable(previous)
                }
                updateLocalVeTable(previous)
                _veTable.value = previous
                _analyzerUndoVeTable.value = null
            } catch (e: Exception) {
                _analyzerError.value = e.message
            }
        }
    }

    fun reloadAnalyzerVeTable() {
        loadVeTable(1)
    }

    fun applyGeneratedBaseMap(map: GeneratedBaseMap, writeTables: Boolean = true, writeConstants: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            try {
                if (writeTables) {
                    client?.writeVeTable(map.veTable)
                    client?.writeIgnitionTable(map.ignitionTable)
                    client?.writeAfrTable(map.afrTable)
                    _veTable.value = map.veTable
                    _ignitionTable.value = map.ignitionTable
                    _afrTable.value = map.afrTable
                }
                if (writeConstants) {
                    client?.writeEngineConstants(map.engineConstants)
                    _engineConstants.value = map.engineConstants
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    private fun applyConfiguredIniDefinition(activeClient: EcuTransport) {
        val signature = activeClient.getFirmwareInfoCached()?.signature?.trim().orEmpty()
        if (signature.isBlank()) return

        val settings = _desktopSettings.value
        if (settings.iniSelectionMode == IniSelectionMode.AUTOMATIC) {
            val cachedDefinitionId = DesktopSettingsStore.loadCachedRemoteIniId(signature)
            if (!cachedDefinitionId.isNullOrBlank() && definitionRepository.hasCachedDefinitionById(cachedDefinitionId)) {
                val cachedDefinition = definitionRepository.loadCachedDefinitionById(cachedDefinitionId)
                _activeIniCatalogEntry.value = _availableIniDefinitions.value.firstOrNull { it.id == cachedDefinitionId }
                _activeIniDefinition.value = cachedDefinition
                if (!activeClient.applyIniDefinition(cachedDefinition)) {
                    _lastError.value = "Falha ao aplicar definicao .ini em cache $cachedDefinitionId"
                }
                return
            }
        }

        val definition = when {
            settings.iniSelectionMode == IniSelectionMode.MANUAL &&
                settings.iniSelectionSource == IniSelectionSource.IMPORTED &&
                !settings.iniDefinitionId.isNullOrBlank() -> {
                _activeIniCatalogEntry.value = null
                definitionRepository.loadImportedDefinition(settings.iniDefinitionId)
            }

            else -> {
                val entry = resolveCatalogEntryForSignature(signature, settings) ?: return
                _activeIniCatalogEntry.value = entry
                val wasCached = definitionRepository.isDefinitionCached(entry)
                val loaded = definitionRepository.loadDefinition(entry)
                DesktopSettingsStore.persistCachedRemoteIniId(signature, entry.id)
                if (!wasCached) {
                    _availableIniDefinitions.value = runCatching {
                        definitionRepository.loadCatalog(forceRefresh = false)
                    }.getOrDefault(_availableIniDefinitions.value)
                }
                loaded
            }
        }

        _activeIniDefinition.value = definition
        if (!activeClient.applyIniDefinition(definition)) {
            _lastError.value = "Falha ao aplicar definicao .ini ${definition.sourceName}"
        }
    }

    private fun resolveCatalogEntryForSignature(
        signature: String,
        settings: DesktopSettingsState,
    ): IniCatalogEntry? {
        if (settings.iniSelectionMode == IniSelectionMode.AUTOMATIC) {
            val cachedDefinitionId = DesktopSettingsStore.loadCachedRemoteIniId(signature)
            if (!cachedDefinitionId.isNullOrBlank() && definitionRepository.hasCachedDefinitionById(cachedDefinitionId)) {
                val cachedCatalogEntry = _availableIniDefinitions.value.firstOrNull { it.id == cachedDefinitionId }
                if (cachedCatalogEntry != null) {
                    return cachedCatalogEntry
                }
            }
        }

        val preferred = if (
            settings.iniSelectionMode == IniSelectionMode.MANUAL &&
            settings.iniSelectionSource == IniSelectionSource.CATALOG &&
            !settings.iniDefinitionId.isNullOrBlank()
        ) {
            definitionRepository.loadCatalog(forceRefresh = false).firstOrNull { it.id == settings.iniDefinitionId }
        } else {
            definitionRepository.findMatchingEntry(signature, forceCatalogRefresh = false)
        }
        if (preferred != null) return preferred

        return if (
            settings.iniSelectionMode == IniSelectionMode.MANUAL &&
            settings.iniSelectionSource == IniSelectionSource.CATALOG &&
            !settings.iniDefinitionId.isNullOrBlank()
        ) {
            definitionRepository.loadCatalog(forceRefresh = true).firstOrNull { it.id == settings.iniDefinitionId }
        } else {
            definitionRepository.findMatchingEntry(signature, forceCatalogRefresh = true)
        }
    }

    private fun loadIdleControlSettingsFromSession(sessionDir: File?): IdleControlSettings? {
        if (sessionDir == null) return null
        val page4File = File(sessionDir, "page_${IdleControlSettings.PAGE_NUMBER}.bin")
        val page7File = File(sessionDir, "page_${IdleControlSettings.TARGET_PAGE_NUMBER}.bin")
        if (!page4File.exists() || !page7File.exists()) return null

        val page4 = page4File.readBytes()
        val page7 = page7File.readBytes()
        if (page4.size < IdleControlSettings.PAGE_LENGTH || page7.size < IdleControlSettings.TARGET_PAGE_LENGTH) {
            return null
        }

        return IdleControlSettings.fromPage4(page4).copy(
            idleTargetRpm = IdleControlSettings.readTargetRpmFromPage7(page7)
        )
    }

    private fun loadClosedLoopCorrectionsFromSession(sessionDir: File?): ClosedLoopCorrectionConfig? {
        if (sessionDir == null) return null
        val pageFile = File(sessionDir, "page_${ClosedLoopCorrectionMapper.PAGE_NUMBER}.bin")
        if (!pageFile.exists()) return null

        val pageData = pageFile.readBytes()
        if (pageData.size < ClosedLoopCorrectionMapper.PAGE_SIZE) return null

        val era = _firmwareInfo.value?.era ?: FirmwareEra.MODERN_2025
        return runCatching {
            ClosedLoopCorrectionMapper.fromPage(pageData, era)
        }.getOrNull()
    }
}

private fun SessionSyncPrompt.toDesktop(): SyncPrompt {
    return SyncPrompt(
        localSessionDir = localSessionDir,
        ecuSessionDir = ecuSessionDir,
    )
}
