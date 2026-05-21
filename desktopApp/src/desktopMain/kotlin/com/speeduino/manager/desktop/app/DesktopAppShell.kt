package com.speeduino.manager.desktop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ConnectionType
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.navigation.DesktopRoute
import com.speeduino.manager.desktop.navigation.NavigationSidebar
import com.speeduino.manager.desktop.navigation.ScreenHost
import com.speeduino.manager.desktop.navigation.parentRoute
import com.speeduino.manager.desktop.ui.HeaderBar
import kotlinx.coroutines.launch

@Composable
internal fun DesktopAppShell() {
    val scope = rememberCoroutineScope()
    val controller = remember { DesktopSpeeduinoController(scope) }
    val desktopSettings by controller.desktopSettings.collectAsState()
    val appState = rememberDesktopAppState(desktopSettings)
    val strings = LocalStrings.current

    val connectionState by controller.connectionState.collectAsState()
    val liveData by controller.liveData.collectAsState()
    val configState by controller.configState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val portIsValid by remember {
        derivedStateOf { appState.port.toIntOrNull()?.let { it in 1..65535 } == true }
    }
    val onToggleConnection = {
        if (!connectionState.isConnected) {
            when (appState.connectionType) {
                ConnectionType.TCP -> if (portIsValid) {
                    controller.connectTcp(appState.host, appState.port.toInt())
                }
                ConnectionType.USB,
                ConnectionType.BLUETOOTH -> {
                    val baud = appState.baudRate.toIntOrNull() ?: 115200
                    if (appState.serialPort.isNotBlank()) {
                        controller.connectSerial(appState.serialPort, baud, appState.connectionType)
                    }
                }
            }
        } else {
            controller.disconnect()
        }
    }
    val onOpenRoute: (DesktopRoute) -> Unit = { route ->
        appState.currentRoute = route
    }

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF6F1E8),
            Color(0xFFE7EEF0),
            Color(0xFFF6F1E8)
        ),
        start = Offset.Zero,
        end = Offset(0f, 1400f)
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompactSidebar = maxWidth in 900.dp..1199.dp
            val useDrawer = maxWidth < 900.dp

            val appContent: @Composable () -> Unit = {
                Row(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (!useDrawer) {
                        NavigationSidebar(
                            currentRoute = appState.currentRoute,
                            onRouteSelected = onOpenRoute,
                            compact = isCompactSidebar
                        )
                    }
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 1.dp,
                        shadowElevation = 0.dp
                    ) {
                        val contentScroll = rememberScrollState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .verticalScroll(contentScroll),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                HeaderBar(
                                    title = strings[appState.currentRoute.titleKey],
                                    connectionState = connectionState,
                                    onBackClick = parentRoute(appState.currentRoute)?.let { parent ->
                                        { appState.currentRoute = parent }
                                    },
                                    onMenuClick = if (useDrawer) {
                                        { scope.launch { drawerState.open() } }
                                    } else {
                                        null
                                    }
                                )

                                ScreenHost(
                                    route = appState.currentRoute,
                                    controller = controller,
                                    connectionState = connectionState,
                                    liveData = liveData,
                                    host = appState.host,
                                    port = appState.port,
                                    portIsValid = portIsValid,
                                    connectionType = appState.connectionType,
                                    serialPort = appState.serialPort,
                                    baudRate = appState.baudRate,
                                    onHostChange = { appState.host = it },
                                    onPortChange = { appState.port = it.filter { ch -> ch.isDigit() } },
                                    onConnectionTypeChange = { appState.connectionType = it },
                                    onSerialPortChange = { appState.serialPort = it },
                                    onBaudRateChange = { appState.baudRate = it.filter { ch -> ch.isDigit() } },
                                    onToggleConnection = onToggleConnection,
                                    onOpenSettings = { appState.currentRoute = DesktopRoute.Settings },
                                    onOpenInstitutional = { appState.currentRoute = DesktopRoute.Institutional },
                                    onOpenRoute = onOpenRoute,
                                    onOpenConnectionSettings = { appState.currentRoute = DesktopRoute.ConnectionSettings },
                                    onOpenConnection = { appState.currentRoute = DesktopRoute.Connection },
                                    onOpenBluetoothConnection = { appState.currentRoute = DesktopRoute.BluetoothConnection },
                                    onOpenUsbSerialConnection = { appState.currentRoute = DesktopRoute.UsbSerialConnection },
                                    onOpenVeTable = { appState.currentRoute = DesktopRoute.VeTable },
                                    onOpenVeTable2 = { appState.currentRoute = DesktopRoute.VeTable2 },
                                    onOpenIgnitionTable = { appState.currentRoute = DesktopRoute.IgnitionTable },
                                    onOpenIgnitionTable2 = { appState.currentRoute = DesktopRoute.IgnitionTable2 },
                                    onOpenAfrTable = { appState.currentRoute = DesktopRoute.AfrTable },
                                    onOpenDwellTable = { appState.currentRoute = DesktopRoute.DwellTable },
                                    onOpenBaseMapWizard = { appState.currentRoute = DesktopRoute.BaseMapWizard },
                                    onOpenEngineConstants = { appState.currentRoute = DesktopRoute.EngineConstants },
                                    onOpenTriggerSettings = { appState.currentRoute = DesktopRoute.TriggerSettings },
                                    onOpenIdleControl = { appState.currentRoute = DesktopRoute.IdleControl },
                                    onOpenInputOutputConfig = { appState.currentRoute = DesktopRoute.InputOutputConfig },
                                    onOpenSensorCalibration = { appState.currentRoute = DesktopRoute.SensorsConfig },
                                    onOpenEngineProtection = { appState.currentRoute = DesktopRoute.EngineProtection },
                                    onOpenClosedLoopCorrections = { appState.currentRoute = DesktopRoute.ClosedLoopCorrections },
                                    onOpenInjectorConfig = { appState.currentRoute = DesktopRoute.InjectorConfig },
                                    onOpenRevLimiterConfig = { appState.currentRoute = DesktopRoute.RevLimiterConfig },
                                    onOpenSecondarySerial = { appState.currentRoute = DesktopRoute.SecondarySerial },
                                    onOpenTuningAssistant = { appState.currentRoute = DesktopRoute.TuningAssistant },
                                    onOpenLogsEcuTools = { appState.currentRoute = DesktopRoute.LogsEcuTools },
                                    onOpenLogViewer = { appState.currentRoute = DesktopRoute.LogViewer },
                                    onOpenRealTimeMonitor = { appState.currentRoute = DesktopRoute.RealTimeMonitor },
                                    onOpenLogAnalyzer = { appState.currentRoute = DesktopRoute.LogAnalyzer },
                                    onOpenBeforeAfter = { appState.currentRoute = DesktopRoute.BeforeAfter },
                                    onOpenHistoricalLogViewer = { path ->
                                        controller.loadLogSnapshotFromCsv(path)
                                        appState.currentRoute = DesktopRoute.LogViewer
                                    }
                                )
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(contentScroll),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                            if (configState.isBusy) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 3.dp,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = strings.format("label.configProgress", configState.progressPercent),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = configState.message ?: strings["label.noData"],
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (useDrawer) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            NavigationSidebar(
                                currentRoute = appState.currentRoute,
                                onRouteSelected = { route ->
                                    appState.currentRoute = route
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    appContent()
                }
            } else {
                appContent()
            }
        }
    }

    val syncPrompt by controller.syncPrompt.collectAsState()
    if (syncPrompt != null) {
        AlertDialog(
            onDismissRequest = { controller.dismissSyncPrompt() },
            title = { Text(strings["label.syncDialogTitle"]) },
            text = { Text(strings["label.syncDialogMessage"]) },
            confirmButton = {
                FilledTonalButton(onClick = { controller.chooseSyncSource(useLocal = true) }) {
                    Text(strings["action.useLocal"])
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = { controller.chooseSyncSource(useLocal = false) }) {
                    Text(strings["action.useEcu"])
                }
            }
        )
    }
}
