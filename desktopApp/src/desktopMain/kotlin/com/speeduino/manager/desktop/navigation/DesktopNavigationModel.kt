package com.speeduino.manager.desktop.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector

internal data class NavSection(val titleKey: String, val entries: List<NavEntry>)

internal data class NavEntry(
    val route: DesktopRoute,
    val children: List<DesktopRoute> = emptyList(),
)

internal enum class DesktopRoute(
    val labelKey: String,
    val titleKey: String,
    val icon: ImageVector,
    val topLevel: Boolean = false
) {
    Home("route.home", "route.home", Icons.Default.Home, topLevel = true),
    Settings("app.settingsLabel", "app.settingsTitle", Icons.Default.Settings, topLevel = true),
    Institutional("route.institutional", "label.institutionalTitle", Icons.Default.Info, topLevel = true),
    More("nav.sectionMore", "nav.sectionMore", Icons.Default.Info, topLevel = true),
    Dashboard("route.dashboard", "route.dashboard", Icons.Default.Dashboard, topLevel = true),
    Connection("route.connection", "route.connection", Icons.Default.Cable, topLevel = true),
    Fuel("route.fuel", "route.fuel", Icons.Default.TableChart, topLevel = true),
    Ignition("route.ignition", "route.ignition", Icons.Default.TableChart, topLevel = true),
    EngineSetup("route.engineSetup", "route.engineSetup", Icons.Default.Settings, topLevel = true),
    EngineOperation("route.engineOperation", "route.engineOperation", Icons.Default.Settings, topLevel = true),
    Tools("route.tools", "route.tools", Icons.Default.Build, topLevel = true),
    ConnectionSettings("label.wifiTcp", "label.wifiTcp", Icons.Default.Cable),
    BluetoothConnection("label.bluetooth", "label.bluetooth", Icons.Default.Cable),
    UsbSerialConnection("label.usbSerial", "label.usbSerial", Icons.Default.Cable),
    MapsTables("route.mapsTables", "route.mapsTables", Icons.Default.TableChart),
    ConfigsTuning("route.configsTuning", "route.configsTuning", Icons.Default.Settings),
    TuningAssistant("route.tuningAssistant", "label.tuningAssistantTitle", Icons.Default.Settings),
    InjectorConfig("label.injectors", "label.injectors", Icons.Default.Settings),
    InputOutputConfig("label.inputOutputTitle", "label.inputOutputTitle", Icons.Default.Settings),
    RevLimiterConfig("label.revLimiter", "label.revLimiter", Icons.Default.Settings),
    SecondarySerial("label.secondarySerialTitle", "label.secondarySerialTitle", Icons.Default.Settings),
    VeTable("route.veTable", "route.veTable", Icons.Default.TableChart),
    VeTable2("route.veTable2", "route.veTable2", Icons.Default.TableChart),
    IgnitionTable("route.ignitionTable", "route.ignitionTable", Icons.Default.TableChart),
    IgnitionTable2("route.ignitionTable2", "route.ignitionTable2", Icons.Default.TableChart),
    AfrTable("route.afrTable", "route.afrTable", Icons.Default.TableChart),
    DwellTable("route.dwellTable", "route.dwellTable", Icons.Default.TableChart),
    BaseMapWizard("route.baseMapWizard", "route.baseMapWizard", Icons.Default.TableChart),
    EngineConstants("route.engineConstants", "route.engineConstants", Icons.Default.Settings),
    TriggerSettings("route.triggerSettings", "route.triggerSettings", Icons.Default.Settings),
    IgnitionConfig("route.ignition", "route.ignition", Icons.Default.Settings),
    IdleControl("route.idleControl", "route.idleControl", Icons.Default.Settings),
    ClosedLoopCorrections("route.closedLoopCorrections", "route.closedLoopCorrections", Icons.Default.Settings),
    SensorsConfig("route.sensorsConfig", "route.sensorsConfig", Icons.Default.Settings),
    EngineProtection("route.engineProtection", "route.engineProtection", Icons.Default.Settings),
    RealTimeMonitor("route.realTimeMonitor", "route.realTimeMonitor", Icons.Default.Build),
    LogViewer("route.logViewer", "route.logViewer", Icons.Default.Build),
    LogsEcuTools("route.logsEcuTools", "label.logsEcuToolsTitle", Icons.Default.Build),
    LogAnalyzer("route.logAnalyzer", "route.logAnalyzer", Icons.Default.Build),
    BeforeAfter("route.beforeAfter", "route.beforeAfter", Icons.Default.Build),
    VirtualDyno("route.virtualDyno", "route.virtualDyno", Icons.Default.Build)
}

internal fun navSections(): List<NavSection> {
    return listOf(
        NavSection(
            titleKey = "nav.sectionApp",
            entries = listOf(
                NavEntry(DesktopRoute.Home),
                NavEntry(DesktopRoute.Dashboard),
                NavEntry(DesktopRoute.Settings),
                NavEntry(DesktopRoute.More),
            )
        ),
        connectionNavSection(),
        fuelNavSection(),
        ignitionNavSection(),
        engineSetupNavSection(),
        engineOperationNavSection(),
        toolsNavSection(),
        NavSection(
            titleKey = "nav.sectionMore",
            entries = listOf(NavEntry(DesktopRoute.Institutional))
        )
    )
}

internal fun selectedNavRoute(route: DesktopRoute): DesktopRoute {
    return generateSequence(route) { current -> parentRoute(current) }
        .firstOrNull { it.topLevel }
        ?: route
}

internal fun parentRoute(route: DesktopRoute): DesktopRoute? {
    return when (route) {
        DesktopRoute.ConnectionSettings,
        DesktopRoute.BluetoothConnection,
        DesktopRoute.UsbSerialConnection -> DesktopRoute.Connection
        DesktopRoute.VeTable,
        DesktopRoute.VeTable2,
        DesktopRoute.AfrTable,
        DesktopRoute.BaseMapWizard,
        DesktopRoute.TuningAssistant -> DesktopRoute.Fuel
        DesktopRoute.IgnitionTable,
        DesktopRoute.IgnitionTable2,
        DesktopRoute.DwellTable,
        DesktopRoute.TriggerSettings,
        DesktopRoute.IgnitionConfig -> DesktopRoute.Ignition
        DesktopRoute.InjectorConfig,
        DesktopRoute.InputOutputConfig,
        DesktopRoute.EngineConstants,
        DesktopRoute.SensorsConfig,
        DesktopRoute.SecondarySerial -> DesktopRoute.EngineSetup
        DesktopRoute.IdleControl,
        DesktopRoute.ClosedLoopCorrections,
        DesktopRoute.EngineProtection,
        DesktopRoute.RevLimiterConfig -> DesktopRoute.EngineOperation
        DesktopRoute.LogViewer,
        DesktopRoute.RealTimeMonitor,
        DesktopRoute.LogAnalyzer,
        DesktopRoute.BeforeAfter,
        DesktopRoute.VirtualDyno -> DesktopRoute.Tools
        DesktopRoute.MapsTables -> DesktopRoute.Fuel
        DesktopRoute.ConfigsTuning -> DesktopRoute.EngineSetup
        DesktopRoute.LogsEcuTools -> DesktopRoute.Tools
        DesktopRoute.More -> null
        else -> null
    }
}
