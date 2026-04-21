package com.speeduino.manager.desktop.navigation

internal data class NavSection(
    val titleKey: String,
    val route: DesktopRoute,
    val activeRoutes: Set<DesktopRoute> = setOf(route)
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

internal fun navSections(): List<NavSection> {
    return listOf(
        appNavSection(),
        dashboardNavSection(),
        connectionNavSection(),
        mapsNavSection(),
        configsNavSection(),
        logsNavSection()
    )
}

internal fun parentRoute(route: DesktopRoute): DesktopRoute? {
    return when (route) {
        DesktopRoute.ConnectionSettings,
        DesktopRoute.BluetoothConnection,
        DesktopRoute.UsbSerialConnection -> DesktopRoute.Connection
        DesktopRoute.VeTable,
        DesktopRoute.VeTable2,
        DesktopRoute.IgnitionTable,
        DesktopRoute.IgnitionTable2,
        DesktopRoute.AfrTable,
        DesktopRoute.BaseMapWizard -> DesktopRoute.MapsTables
        DesktopRoute.TuningAssistant,
        DesktopRoute.InjectorConfig,
        DesktopRoute.InputOutputConfig,
        DesktopRoute.RevLimiterConfig,
        DesktopRoute.SecondarySerial,
        DesktopRoute.EngineConstants,
        DesktopRoute.TriggerSettings,
        DesktopRoute.IdleControl,
        DesktopRoute.ClosedLoopCorrections,
        DesktopRoute.SensorsConfig,
        DesktopRoute.EngineProtection -> DesktopRoute.ConfigsTuning
        DesktopRoute.LogViewer,
        DesktopRoute.RealTimeMonitor,
        DesktopRoute.LogAnalyzer,
        DesktopRoute.BeforeAfter -> DesktopRoute.LogsEcuTools
        else -> null
    }
}
