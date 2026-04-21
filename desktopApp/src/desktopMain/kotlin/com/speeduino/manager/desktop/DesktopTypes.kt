package com.speeduino.manager.desktop

import java.io.File
import com.speeduino.manager.model.RusefiInputOutputSnapshot
import com.speeduino.manager.model.SecondarySerialConfig

internal enum class ConnectionStatus {
    Connected,
    Disconnected,
    Connecting,
    Failed
}

internal data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val detail: String? = null
) {
    val isConnected: Boolean
        get() = status == ConnectionStatus.Connected
}

internal data class ConfigSyncState(
    val isBusy: Boolean = false,
    val progressPercent: Int = 0,
    val message: String? = null,
    val lastSessionDir: File? = null
)

internal data class RestoreOutcome(
    val warnings: List<String>,
    val inconsistentPages: List<Byte>,
    val completed: Boolean
)

internal data class SyncPrompt(
    val localSessionDir: File,
    val ecuSessionDir: File
)

internal data class TuningConfigState(
    val rusefiSnapshot: RusefiInputOutputSnapshot? = null,
    val secondarySerialConfig: SecondarySerialConfig? = null,
)

internal data class TransportCallbacks(
    val onDataReceived: (com.speeduino.manager.SpeeduinoLiveData) -> Unit,
    val onConnectionStateChanged: (Boolean) -> Unit,
    val onError: (String) -> Unit,
)

internal enum class ConnectionType(val labelKey: String) {
    TCP("label.connectionTypeTcp"),
    USB("label.connectionTypeUsb"),
    BLUETOOTH("label.connectionTypeBluetooth");

    fun label(strings: Strings): String = strings[labelKey]
}

internal data class SerialPortInfo(
    val systemPortName: String,
    val displayName: String
)

internal data class BluetoothDeviceInfo(
    val address: String,
    val displayName: String
)

internal data class NavSection(
    val titleKey: String,
    val routes: List<DesktopRoute>
)

internal enum class DesktopRoute(val labelKey: String, val titleKey: String) {
    Settings("app.settingsLabel", "app.settingsTitle"),
    Institutional("route.institutional", "label.institutionalTitle"),
    Dashboard("route.dashboard", "route.dashboard"),
    Connection("route.connection", "route.connection"),
    ConnectionSettings("label.connectionScreenTitle", "label.connectionScreenTitle"),
    BluetoothConnection("label.bluetooth", "label.bluetooth"),
    UsbSerialConnection("label.usbSerial", "label.usbSerial"),
    MapsTables("route.mapsTables", "route.mapsTables"),
    ConfigsTuning("route.configsTuning", "route.configsTuning"),
    TuningAssistant("route.tuningAssistant", "label.tuningAssistantTitle"),
    InjectorConfig("label.injectors", "label.injectors"),
    InputOutputConfig("label.inputOutputTitle", "label.inputOutputTitle"),
    RevLimiterConfig("label.revLimiter", "label.revLimiter"),
    SecondarySerial("label.secondarySerialTitle", "label.secondarySerialTitle"),
    VeTable("route.veTable", "route.veTable"),
    VeTable2("route.veTable2", "route.veTable2"),
    IgnitionTable("route.ignitionTable", "route.ignitionTable"),
    IgnitionTable2("route.ignitionTable2", "route.ignitionTable2"),
    AfrTable("route.afrTable", "route.afrTable"),
    BaseMapWizard("route.baseMapWizard", "route.baseMapWizard"),
    EngineConstants("route.engineConstants", "route.engineConstants"),
    TriggerSettings("route.triggerSettings", "route.triggerSettings"),
    IdleControl("route.idleControl", "route.idleControl"),
    ClosedLoopCorrections("route.closedLoopCorrections", "route.closedLoopCorrections"),
    SensorsConfig("route.sensorsConfig", "route.sensorsConfig"),
    EngineProtection("route.engineProtection", "route.engineProtection"),
    RealTimeMonitor("route.realTimeMonitor", "route.realTimeMonitor"),
    LogViewer("route.logViewer", "route.logViewer"),
    LogsEcuTools("route.logsEcuTools", "label.logsEcuToolsTitle"),
    LogAnalyzer("route.logAnalyzer", "route.logAnalyzer"),
    BeforeAfter("route.beforeAfter", "route.beforeAfter")
}
