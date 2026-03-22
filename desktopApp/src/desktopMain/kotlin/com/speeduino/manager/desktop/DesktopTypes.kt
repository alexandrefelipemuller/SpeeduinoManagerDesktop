package com.speeduino.manager.desktop

import java.io.File

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

internal data class NavSection(
    val titleKey: String,
    val routes: List<DesktopRoute>
)

internal enum class DesktopRoute(val labelKey: String, val titleKey: String) {
    Settings("app.settingsLabel", "app.settingsTitle"),
    Dashboard("route.dashboard", "route.dashboard"),
    Connection("route.connection", "route.connection"),
    VeTable("route.veTable", "route.veTable"),
    VeTable2("route.veTable2", "route.veTable2"),
    IgnitionTable("route.ignitionTable", "route.ignitionTable"),
    IgnitionTable2("route.ignitionTable2", "route.ignitionTable2"),
    AfrTable("route.afrTable", "route.afrTable"),
    BaseMapWizard("route.baseMapWizard", "route.baseMapWizard"),
    EngineConstants("route.engineConstants", "route.engineConstants"),
    TriggerSettings("route.triggerSettings", "route.triggerSettings"),
    SensorsConfig("route.sensorsConfig", "route.sensorsConfig"),
    EngineProtection("route.engineProtection", "route.engineProtection"),
    RealTimeMonitor("route.realTimeMonitor", "route.realTimeMonitor"),
    LogViewer("route.logViewer", "route.logViewer"),
    LogAnalyzer("route.logAnalyzer", "route.logAnalyzer"),
    BeforeAfter("route.beforeAfter", "route.beforeAfter")
}
