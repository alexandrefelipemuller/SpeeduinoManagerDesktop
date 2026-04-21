package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speeduino.manager.SpeeduinoLiveData

@Composable
internal fun NavigationSidebar(
    currentRoute: DesktopRoute,
    onRouteSelected: (DesktopRoute) -> Unit
) {
    val strings = LocalStrings.current

    Surface(
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings["app.sidebarTitlePrimary"],
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = strings["app.sidebarTitleSecondary"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    navSections().forEach { section ->
                        Text(
                            text = strings[section.titleKey].uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            section.routes.forEach { route ->
                                val selected = route == currentRoute
                                val contentColor = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                val background = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                }
                                val borderColor = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = background,
                                    border = BorderStroke(1.dp, borderColor)
                                ) {
                                    val clickableModifier = if (selected) {
                                        Modifier
                                    } else {
                                        Modifier.clickable { onRouteSelected(route) }
                                    }
                                    Row(
                                        modifier = clickableModifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings[route.labelKey],
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
internal fun ScreenHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    connectionState: ConnectionState,
    liveData: SpeeduinoLiveData?,
    host: String,
    port: String,
    portIsValid: Boolean,
    connectionType: ConnectionType,
    serialPort: String,
    baudRate: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectionTypeChange: (ConnectionType) -> Unit,
    onSerialPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit,
    onToggleConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInstitutional: () -> Unit,
    onOpenConnectionSettings: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenBluetoothConnection: () -> Unit,
    onOpenUsbSerialConnection: () -> Unit,
    onOpenVeTable: () -> Unit,
    onOpenVeTable2: () -> Unit,
    onOpenIgnitionTable: () -> Unit,
    onOpenIgnitionTable2: () -> Unit,
    onOpenAfrTable: () -> Unit,
    onOpenBaseMapWizard: () -> Unit,
    onOpenEngineConstants: () -> Unit,
    onOpenTriggerSettings: () -> Unit,
    onOpenIdleControl: () -> Unit,
    onOpenInputOutputConfig: () -> Unit,
    onOpenSensorCalibration: () -> Unit,
    onOpenEngineProtection: () -> Unit,
    onOpenClosedLoopCorrections: () -> Unit,
    onOpenInjectorConfig: () -> Unit,
    onOpenRevLimiterConfig: () -> Unit,
    onOpenSecondarySerial: () -> Unit,
    onOpenTuningAssistant: () -> Unit,
    onOpenLogsEcuTools: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenBeforeAfter: () -> Unit
) {
    when (route) {
        DesktopRoute.Settings -> SettingsScreen(controller)
        DesktopRoute.Institutional -> InstitutionalScreenDesktop()
        DesktopRoute.Dashboard -> DashboardScreen(liveData, onOpenSettings = onOpenSettings)
        DesktopRoute.Connection -> DiagnosticScreen(
            controller = controller,
            connectionState = connectionState,
            host = host,
            port = port,
            portIsValid = portIsValid,
            connectionType = connectionType,
            serialPort = serialPort,
            baudRate = baudRate,
            onHostChange = onHostChange,
            onPortChange = onPortChange,
            onConnectionTypeChange = onConnectionTypeChange,
            onSerialPortChange = onSerialPortChange,
            onBaudRateChange = onBaudRateChange,
            onToggleConnection = onToggleConnection,
            onOpenConnectionSettings = onOpenConnectionSettings,
            onOpenBluetoothConnection = onOpenBluetoothConnection,
            onOpenUsbSerialConnection = onOpenUsbSerialConnection,
            onOpenLogsEcuTools = onOpenLogsEcuTools,
            onOpenInstitutional = onOpenInstitutional
        )
        DesktopRoute.LogsEcuTools -> LogsEcuToolsScreenDesktop(
            onOpenDiagnostic = onOpenConnection,
            onOpenLogViewer = onOpenLogViewer,
            onOpenRealTimeMonitor = onOpenRealTimeMonitor,
            onOpenBeforeAfter = onOpenBeforeAfter
        )
        DesktopRoute.ConnectionSettings -> ConnectionSettingsScreenDesktop(
            controller = controller,
            onOpenBluetoothConnection = onOpenBluetoothConnection,
            onOpenUsbSerialConnection = onOpenUsbSerialConnection
        )
        DesktopRoute.BluetoothConnection -> BluetoothConnectionScreenDesktop(controller)
        DesktopRoute.UsbSerialConnection -> UsbSerialConnectionScreenDesktop(controller)
        DesktopRoute.MapsTables -> MapsTablesScreenDesktop(
            onOpenVeTable = onOpenVeTable,
            onOpenVeTable2 = onOpenVeTable2,
            onOpenIgnitionTable = onOpenIgnitionTable,
            onOpenIgnitionTable2 = onOpenIgnitionTable2,
            onOpenAfrTable = onOpenAfrTable,
            onOpenBaseMapWizard = onOpenBaseMapWizard
        )
        DesktopRoute.ConfigsTuning -> ConfigsTuningScreenDesktop(
            onOpenEngineConstants = onOpenEngineConstants,
            onOpenTriggerSettings = onOpenTriggerSettings,
            onOpenIdleControl = onOpenIdleControl,
            onOpenInputOutput = onOpenInputOutputConfig,
            onOpenSensorCalibration = onOpenSensorCalibration,
            onOpenEngineProtection = onOpenEngineProtection,
            onOpenClosedLoopCorrections = onOpenClosedLoopCorrections,
            onOpenInjectorConfig = onOpenInjectorConfig,
            onOpenRevLimiter = onOpenRevLimiterConfig,
            onOpenSecondarySerial = onOpenSecondarySerial,
            onOpenTuningAssistant = onOpenTuningAssistant
        )
        DesktopRoute.TuningAssistant -> TuningAssistantScreenDesktop(
            controller = controller,
            onOpenBeforeAfter = onOpenBeforeAfter
        )
        DesktopRoute.InjectorConfig -> InjectorConfigScreenDesktop(controller)
        DesktopRoute.InputOutputConfig -> InputOutputConfigScreenDesktop(
            controller = controller,
            onOpenSecondarySerial = onOpenSecondarySerial
        )
        DesktopRoute.RevLimiterConfig -> RevLimiterConfigScreenDesktop()
        DesktopRoute.SecondarySerial -> SecondarySerialScreenDesktop(controller)
        DesktopRoute.VeTable -> VeTableScreenDesktop(controller, mapIndex = 1)
        DesktopRoute.VeTable2 -> VeTableScreenDesktop(controller, mapIndex = 2)
        DesktopRoute.IgnitionTable -> IgnitionTableScreenDesktop(controller, mapIndex = 1)
        DesktopRoute.IgnitionTable2 -> IgnitionTableScreenDesktop(controller, mapIndex = 2)
        DesktopRoute.AfrTable -> AfrTableScreenDesktop(controller)
        DesktopRoute.BaseMapWizard -> BaseMapWizardScreenDesktop(controller)
        DesktopRoute.EngineConstants -> EngineConstantsScreenDesktop(controller)
        DesktopRoute.TriggerSettings -> TriggerSettingsScreenDesktop(controller)
        DesktopRoute.IdleControl -> IdleControlScreenDesktop(controller)
        DesktopRoute.ClosedLoopCorrections -> ClosedLoopCorrectionsScreenDesktop(controller)
        DesktopRoute.SensorsConfig -> SensorsConfigScreenDesktop()
        DesktopRoute.EngineProtection -> EngineProtectionScreenDesktop()
        DesktopRoute.RealTimeMonitor -> RealTimeMonitorScreenDesktop(controller, liveData)
        DesktopRoute.LogViewer -> LogViewerScreenDesktop(controller)
        DesktopRoute.LogAnalyzer -> LogAnalyzerScreenDesktop(controller)
        DesktopRoute.BeforeAfter -> BeforeAfterScreenDesktop(controller)
    }
}

internal fun navSections(): List<NavSection> {
    return listOf(
        NavSection(
            titleKey = "nav.sectionApp",
            routes = listOf(DesktopRoute.Settings, DesktopRoute.Institutional)
        ),
        NavSection(
            titleKey = "nav.sectionDashboard",
            routes = listOf(DesktopRoute.Dashboard)
        ),
        NavSection(
            titleKey = "nav.sectionMaps",
            routes = listOf(
                DesktopRoute.MapsTables,
                DesktopRoute.VeTable,
                DesktopRoute.VeTable2,
                DesktopRoute.IgnitionTable,
                DesktopRoute.IgnitionTable2,
                DesktopRoute.AfrTable,
                DesktopRoute.BaseMapWizard
            )
        ),
        NavSection(
            titleKey = "nav.sectionConfigs",
            routes = listOf(
                DesktopRoute.ConfigsTuning,
                DesktopRoute.InjectorConfig,
                DesktopRoute.InputOutputConfig,
                DesktopRoute.RevLimiterConfig,
                DesktopRoute.SecondarySerial,
                DesktopRoute.TuningAssistant,
                DesktopRoute.EngineConstants,
                DesktopRoute.TriggerSettings,
                DesktopRoute.IdleControl,
                DesktopRoute.ClosedLoopCorrections,
                DesktopRoute.SensorsConfig,
                DesktopRoute.EngineProtection
            )
        ),
        NavSection(
            titleKey = "nav.sectionLogs",
            routes = listOf(
                DesktopRoute.ConnectionSettings,
                DesktopRoute.BluetoothConnection,
                DesktopRoute.UsbSerialConnection,
                DesktopRoute.Connection,
                DesktopRoute.LogsEcuTools,
                DesktopRoute.LogViewer,
                DesktopRoute.RealTimeMonitor,
                DesktopRoute.LogAnalyzer,
                DesktopRoute.BeforeAfter
            )
        )
    )
}
