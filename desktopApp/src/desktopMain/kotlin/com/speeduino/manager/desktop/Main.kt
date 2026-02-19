package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.speeduino.manager.ConfigManager
import com.speeduino.manager.FirmwareInfo
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.SpeeduinoClient
import com.speeduino.manager.connection.SpeeduinoTcpConnection
import com.speeduino.manager.connection.SpeeduinoSerialConnection
import com.speeduino.manager.connection.ISpeeduinoConnection
import com.speeduino.manager.compare.BeforeAfterLogComparator
import com.speeduino.manager.compare.LogCompareException
import com.speeduino.manager.compare.LogCompareReason
import com.speeduino.manager.compare.LogCompareResult
import com.speeduino.manager.compare.LogHeatCellState
import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.Algorithm
import com.speeduino.manager.model.Color as SharedColor
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.EngineStroke
import com.speeduino.manager.model.EngineType
import com.speeduino.manager.model.InjectorLayout
import com.speeduino.manager.model.InjectorPortType
import com.speeduino.manager.model.InjectorStaging
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.MapSampleMethod
import com.speeduino.manager.model.Page6Validator
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.basemap.BaseMapAdjustments
import com.speeduino.manager.model.basemap.BaseMapGenerator
import com.speeduino.manager.model.basemap.EngineProfile
import com.speeduino.manager.model.basemap.FuelType
import com.speeduino.manager.model.basemap.GeneratedBaseMap
import com.speeduino.manager.model.logging.LiveLogRecorder
import com.speeduino.manager.model.logging.LiveLogSnapshot
import com.speeduino.manager.tuning.AnalyzerResult
import com.speeduino.manager.tuning.CellRef
import com.speeduino.manager.tuning.TuningAssistantAnalyzer
import com.speeduino.manager.tuning.TuningStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import kotlin.math.abs

private val SpeeduinoColorScheme = lightColorScheme(
    primary = Color(0xFF305C4F),
    onPrimary = Color(0xFFF8F6F2),
    secondary = Color(0xFFC37B2C),
    onSecondary = Color(0xFF2A1A05),
    background = Color(0xFFF5F1E8),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFF0E7D8),
    onSurfaceVariant = Color(0xFF3B342C),
    outline = Color(0xFFB8AFA2)
)

private val SpeeduinoTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
private fun SpeeduinoDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpeeduinoColorScheme,
        typography = SpeeduinoTypography,
        content = content
    )
}

fun main() = application {
    val language by LocalizationManager.language.collectAsState()
    val strings = remember(language) { Strings(Translations.forLanguage(language)) }

    Window(
        onCloseRequest = ::exitApplication,
        title = strings["app.windowTitle"],
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        CompositionLocalProvider(LocalStrings provides strings) {
            SpeeduinoDesktopTheme {
                DesktopApp()
            }
        }
    }
}

@Composable
private fun DesktopApp() {
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("5555") }
    var currentRoute by remember { mutableStateOf(DesktopRoute.Connection) }
    var connectionType by remember { mutableStateOf(ConnectionType.TCP) }
    var serialPort by remember { mutableStateOf("") }
    var baudRate by remember { mutableStateOf("115200") }
    val strings = LocalStrings.current

    val scope = rememberCoroutineScope()
    val controller = remember { DesktopSpeeduinoController(scope) }
    val connectionState by controller.connectionState.collectAsState()
    val liveData by controller.liveData.collectAsState()

    val portIsValid by remember {
        derivedStateOf { port.toIntOrNull()?.let { it in 1..65535 } == true }
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
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NavigationSidebar(
                currentRoute = currentRoute,
                onRouteSelected = { currentRoute = it }
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
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
                            title = strings[currentRoute.titleKey],
                            connectionState = connectionState
                        )

                        ScreenHost(
                            route = currentRoute,
                            controller = controller,
                            connectionState = connectionState,
                            liveData = liveData,
                            host = host,
                            port = port,
                            portIsValid = portIsValid,
                            connectionType = connectionType,
                            serialPort = serialPort,
                            baudRate = baudRate,
                            onHostChange = { host = it },
                            onPortChange = { port = it.filter { ch -> ch.isDigit() } },
                            onConnectionTypeChange = { connectionType = it },
                            onSerialPortChange = { serialPort = it },
                            onBaudRateChange = { baudRate = it.filter { ch -> ch.isDigit() } },
                            onToggleConnection = {
                                if (!connectionState.isConnected) {
                                    when (connectionType) {
                                        ConnectionType.TCP -> if (portIsValid) {
                                            controller.connectTcp(host, port.toInt())
                                        }
                                        ConnectionType.USB,
                                        ConnectionType.BLUETOOTH -> {
                                            val baud = baudRate.toIntOrNull() ?: 115200
                                            if (serialPort.isNotBlank()) {
                                                controller.connectSerial(serialPort, baud)
                                            }
                                        }
                                    }
                                } else {
                                    controller.disconnect()
                                }
                            },
                            onOpenSettings = { currentRoute = DesktopRoute.Settings }
                        )
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(contentScroll),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
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

@Composable
private fun HeaderBar(
    title: String,
    connectionState: ConnectionState
) {
    val strings = LocalStrings.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = strings["app.headerTitle"],
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusPill(
            connectionState = connectionState
        )
    }
}

@Composable
private fun NavigationSidebar(
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
private fun ScreenHost(
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
    onOpenSettings: () -> Unit
) {
    when (route) {
        DesktopRoute.Settings -> SettingsScreen(controller)
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
            onToggleConnection = onToggleConnection
        )
        DesktopRoute.VeTable -> VeTableScreenDesktop(controller)
        DesktopRoute.IgnitionTable -> IgnitionTableScreenDesktop(controller)
        DesktopRoute.AfrTable -> AfrTableScreenDesktop(controller)
        DesktopRoute.BaseMapWizard -> BaseMapWizardScreenDesktop(controller)
        DesktopRoute.EngineConstants -> EngineConstantsScreenDesktop(controller)
        DesktopRoute.TriggerSettings -> TriggerSettingsScreenDesktop(controller)
        DesktopRoute.SensorsConfig -> SensorsConfigScreenDesktop()
        DesktopRoute.EngineProtection -> EngineProtectionScreenDesktop()
        DesktopRoute.RealTimeMonitor -> RealTimeMonitorScreenDesktop(controller, liveData)
        DesktopRoute.LogViewer -> LogViewerScreenDesktop(controller)
        DesktopRoute.LogAnalyzer -> LogAnalyzerScreenDesktop(controller)
        DesktopRoute.BeforeAfter -> BeforeAfterScreenDesktop(controller)
    }
}

@Composable
private fun DiagnosticScreen(
    controller: DesktopSpeeduinoController,
    connectionState: ConnectionState,
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
    onToggleConnection: () -> Unit
) {
    val strings = LocalStrings.current
    val firmwareInfo by controller.firmwareInfo.collectAsState()
    val productString by controller.productString.collectAsState()
    val connectionInfo by controller.connectionInfo.collectAsState()
    val lastError by controller.lastError.collectAsState()
    val appVersion = APP_VERSION
    val serialPorts by controller.serialPorts.collectAsState()

    LaunchedEffect(connectionType) {
        if (connectionType != ConnectionType.TCP) {
            controller.refreshSerialPorts()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConnectionCard(
            host = host,
            port = port,
            portIsValid = portIsValid,
            connectionType = connectionType,
            serialPort = serialPort,
            baudRate = baudRate,
            serialPorts = serialPorts,
            connectionState = connectionState,
            onHostChange = onHostChange,
            onPortChange = onPortChange,
            onConnectionTypeChange = onConnectionTypeChange,
            onSerialPortChange = onSerialPortChange,
            onBaudRateChange = onBaudRateChange,
            onToggleConnection = onToggleConnection
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings["label.diagnostics"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                InfoRow(strings["label.firmware"], firmwareInfo?.signature ?: strings["label.noData"])
                InfoRow(strings["label.product"], productString ?: strings["label.noData"])
                InfoRow(strings["label.connection"], connectionInfo ?: strings["label.noData"])
                InfoRow(strings["label.appVersion"], appVersion)
                if (!lastError.isNullOrBlank()) {
                    val errorText = lastError ?: ""
                    Text(
                        text = strings.format("label.errorWithValue", errorText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9A3B2E)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    liveData: SpeeduinoLiveData?,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    val availableSignals = remember(liveData, strings) { dashboardGaugeSignals(liveData, strings) }
    var selectedGaugeKeys by remember { mutableStateOf(listOf("rpm", "map", "tps", "coolant")) }

    LaunchedEffect(availableSignals.map { it.key }) {
        selectedGaugeKeys = selectedGaugeKeys.map { key ->
            if (availableSignals.any { it.key == key }) key else availableSignals.first().key
        }
    }

    val gauges = remember(availableSignals, selectedGaugeKeys) {
        selectedGaugeKeys.mapIndexed { index, key ->
            availableSignals.firstOrNull { it.key == key } ?: availableSignals[index % availableSignals.size]
        }
    }

    val stats = remember(liveData, strings) {
        listOf(
            StatItem(
                strings["label.battery"],
                liveData?.batteryVoltage?.let { strings.format("label.voltageFormat", it) }
                    ?: strings["label.noData"]
            ),
            StatItem(
                strings["label.ignition"],
                liveData?.advance?.let { strings.format("label.degFormat", it) }
                    ?: strings["label.noData"]
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppSectionCard(onOpenSettings = onOpenSettings)
        GaugeGrid(
            gauges = gauges,
            availableSignals = availableSignals,
            onChangeGauge = { index, signalKey ->
                selectedGaugeKeys = selectedGaugeKeys.toMutableList().also { list ->
                    list[index] = signalKey
                }
            }
        )
        StatRow(stats)
    }
}

@Composable
private fun AppSectionCard(onOpenSettings: () -> Unit) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings["app.sectionTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings["app.settingsTitle"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onOpenSettings) {
                Text(strings["app.settingsLabel"])
            }
        }
    }
}

@Composable
private fun SettingsScreen(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val language by LocalizationManager.language.collectAsState()
    val configState by controller.configState.collectAsState()
    val languageOptions = listOf(
        AppLanguage.EN to strings["app.languageEnglish"],
        AppLanguage.PT to strings["app.languagePortuguese"],
        AppLanguage.ES to strings["app.languageSpanish"],
        AppLanguage.FR to strings["app.languageFrench"],
        AppLanguage.ID to strings["app.languageIndonesian"],
        AppLanguage.ZH to strings["app.languageChinese"]
    )
    val selectedLabel = languageOptions.firstOrNull { it.first == language }?.second
        ?: strings["app.languageEnglish"]

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings["app.settingsTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings["app.sectionTitle"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DropdownField(
                    label = strings["app.languageLabel"],
                    value = selectedLabel,
                    options = languageOptions.map { it.second }
                ) { label ->
                    val selected = languageOptions.firstOrNull { it.second == label }?.first
                        ?: AppLanguage.EN
                    LocalizationManager.setLanguage(selected)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings["label.backupTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings["label.backupSubtitle"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = { controller.downloadAllConfigs() },
                        enabled = !configState.isBusy
                    ) {
                        Text(strings["action.downloadConfig"])
                    }
                    FilledTonalButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val target = chooseSaveFile(
                                title = strings["label.backupSaveTitle"],
                                defaultName = "speeduino_backup_$timestamp.zip"
                            )
                            if (target != null) {
                                controller.exportLatestConfig(target)
                            }
                        },
                        enabled = !configState.isBusy
                    ) {
                        Text(strings["action.exportConfig"])
                    }
                    FilledTonalButton(
                        onClick = {
                            val source = chooseOpenFile(strings["label.backupOpenTitle"])
                            if (source != null) {
                                controller.importConfigAndRestore(source)
                            }
                        },
                        enabled = !configState.isBusy
                    ) {
                        Text(strings["action.importConfig"])
                    }
                }
                if (configState.isBusy) {
                    Text(
                        text = strings.format("label.configProgress", configState.progressPercent),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val message = configState.message
                if (!message.isNullOrBlank()) {
                    Text(
                        text = strings.format("label.configStatus", message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun VeTableScreenDesktop(controller: DesktopSpeeduinoController) {
    val table by controller.veTable.collectAsState()
    val strings = LocalStrings.current
    MapTableScreen(
        title = strings["route.veTable"],
        description = strings["label.mapVe"],
        table = table,
        onLoad = controller::loadVeTable,
        onSave = controller::saveVeTable,
        formatValue = { it.toString() },
        parseValue = { it.toIntOrNull() },
        valueRange = 0..255,
        cellColor = { VeTable.getColorForValue(it).toComposeColor() },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

@Composable
private fun IgnitionTableScreenDesktop(controller: DesktopSpeeduinoController) {
    val table by controller.ignitionTable.collectAsState()
    val strings = LocalStrings.current
    MapTableScreen(
        title = strings["route.ignitionTable"],
        description = strings["label.mapIgnition"],
        table = table,
        onLoad = controller::loadIgnitionTable,
        onSave = controller::saveIgnitionTable,
        formatValue = { it.toString() },
        parseValue = { it.toIntOrNull() },
        valueRange = -40..70,
        cellColor = { IgnitionTable.getColorForValue(it).toComposeColor() },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

@Composable
private fun AfrTableScreenDesktop(controller: DesktopSpeeduinoController) {
    val table by controller.afrTable.collectAsState()
    val strings = LocalStrings.current
    MapTableScreen(
        title = strings["route.afrTable"],
        description = strings["label.mapAfr"],
        table = table,
        onLoad = controller::loadAfrTable,
        onSave = controller::saveAfrTable,
        formatValue = { AfrTable.formatValue(it) },
        parseValue = { parseAfrValue(it) },
        valueRange = 100..200,
        cellColor = { AfrTable.getColorForValue(it).toComposeColor() },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

@Composable
private fun <T> MapTableScreen(
    title: String,
    description: String,
    table: T?,
    onLoad: () -> Unit,
    onSave: (T) -> Unit,
    formatValue: (Int) -> String,
    parseValue: (String) -> Int?,
    valueRange: IntRange,
    cellColor: (Int) -> Color,
    rpmBins: (T) -> List<Int>,
    loadBins: (T) -> List<Int>,
    values: (T) -> List<List<Int>>,
    updateCell: (T, Int, Int, Int) -> T,
    updateRpm: (T, Int, Int) -> T,
    updateLoad: (T, Int, Int) -> T
) {
    val strings = LocalStrings.current
    var workingTable by remember(table) { mutableStateOf(table) }
    var hasChanges by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TableEditTarget?>(null) }
    var editValue by remember { mutableStateOf("") }
    var invertYAxis by remember { mutableStateOf(true) }

    LaunchedEffect(table) {
        workingTable = table
        hasChanges = false
    }

    val rpm = workingTable?.let(rpmBins).orEmpty()
    val load = workingTable?.let(loadBins).orEmpty()
    val grid = workingTable?.let(values).orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onLoad) { Text(strings["action.loadEcu"]) }
                    FilledTonalButton(
                        onClick = { workingTable?.let(onSave); hasChanges = false },
                        enabled = workingTable != null && hasChanges
                    ) { Text(strings["action.saveEcu"]) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings["label.tableInvert"],
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = { invertYAxis = !invertYAxis }) {
                Text(
                    text = "⇅",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (invertYAxis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (workingTable == null) {
            PlaceholderScreen(title, strings["label.noDataLoaded"])
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 520.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HeaderCell(strings["label.loadRpm"])
                            rpm.forEachIndexed { index, value ->
                                HeaderCell(value.toString()) {
                                    editTarget = TableEditTarget.Rpm(index)
                                    editValue = value.toString()
                                }
                            }
                        }
                        val loadOrder = if (invertYAxis) load.indices.reversed() else load.indices
                        loadOrder.forEach { dataRowIndex ->
                            val loadValue = load.getOrNull(dataRowIndex) ?: return@forEach
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HeaderCell(loadValue.toString()) {
                                    editTarget = TableEditTarget.Load(dataRowIndex)
                                    editValue = loadValue.toString()
                                }
                                grid.getOrNull(dataRowIndex)?.forEachIndexed { colIndex, cell ->
                                    ValueCell(
                                        value = formatValue(cell),
                                        background = cellColor(cell),
                                        onClick = {
                                            editTarget = TableEditTarget.Cell(dataRowIndex, colIndex)
                                            editValue = formatValue(cell)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(verticalScroll),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                    HorizontalScrollbar(
                        adapter = rememberScrollbarAdapter(horizontalScroll),
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    )
                }
            }
        }
    }

    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(strings["action.editValue"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.format("label.recommendedRange", valueRange.first, valueRange.last))
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val parsed = parseValue(editValue)
                        val target = editTarget
                        if (parsed != null && target != null) {
                            workingTable = when (target) {
                                is TableEditTarget.Cell -> workingTable?.let {
                                    updateCell(it, target.row, target.col, parsed.coerceIn(valueRange))
                                }
                                is TableEditTarget.Rpm -> workingTable?.let {
                                    updateRpm(it, target.index, parsed)
                                }
                                is TableEditTarget.Load -> workingTable?.let {
                                    updateLoad(it, target.index, parsed)
                                }
                            }
                            hasChanges = true
                        }
                        editTarget = null
                    }
                ) { Text(strings["action.apply"]) }
            },
            dismissButton = {
                FilledTonalButton(onClick = { editTarget = null }) { Text(strings["action.cancel"]) }
            }
        )
    }
}

private sealed class TableEditTarget {
    data class Cell(val row: Int, val col: Int) : TableEditTarget()
    data class Rpm(val index: Int) : TableEditTarget()
    data class Load(val index: Int) : TableEditTarget()
}

@Composable
private fun HeaderCell(text: String, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        val modifier = Modifier
            .width(68.dp)
            .padding(vertical = 6.dp, horizontal = 8.dp)
        val clickable = if (onClick != null) modifier.clickable { onClick() } else modifier
        Box(
            modifier = clickable,
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ValueCell(value: String, onClick: () -> Unit) {
    ValueCell(value = value, background = MaterialTheme.colorScheme.surface, onClick = onClick)
}

@Composable
private fun ValueCell(value: String, background: Color, onClick: () -> Unit) {
    val bg = background.copy(alpha = 0.78f)
    val contentColor = if (bg.luminance() < 0.45f) Color(0xFFF8F6F2) else Color(0xFF1C1B1A)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .width(68.dp)
                .padding(vertical = 6.dp, horizontal = 8.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

private fun SharedColor.toComposeColor(): Color {
    return Color(argb)
}

private fun parseAfrValue(text: String): Int? {
    val normalized = text.replace(',', '.')
    return when {
        normalized.contains('.') -> {
            normalized.toFloatOrNull()?.let { (it * 10).toInt() }
        }
        else -> normalized.toIntOrNull()
    }
}

@Composable
private fun EngineConstantsScreenDesktop(controller: DesktopSpeeduinoController) {
    val constants by controller.engineConstants.collectAsState()
    val strings = LocalStrings.current
    var reqFuel by remember(constants) { mutableStateOf(constants?.reqFuel?.toString() ?: "6.0") }
    var batteryVoltage by remember(constants) { mutableStateOf(constants?.batteryVoltage?.toString() ?: "12.0") }
    var algorithm by remember(constants) { mutableStateOf(constants?.algorithm ?: Algorithm.SPEED_DENSITY) }
    var squirtsPerCycle by remember(constants) { mutableStateOf(constants?.squirtsPerCycle?.toString() ?: "1") }
    var injectorStaging by remember(constants) { mutableStateOf(constants?.injectorStaging ?: InjectorStaging.ALTERNATING) }
    var engineStroke by remember(constants) { mutableStateOf(constants?.engineStroke ?: EngineStroke.FOUR_STROKE) }
    var numberOfCylinders by remember(constants) { mutableStateOf(constants?.numberOfCylinders?.toString() ?: "4") }
    var injectorPortType by remember(constants) { mutableStateOf(constants?.injectorPortType ?: InjectorPortType.PORT) }
    var numberOfInjectors by remember(constants) { mutableStateOf(constants?.numberOfInjectors?.toString() ?: "4") }
    var engineType by remember(constants) { mutableStateOf(constants?.engineType ?: EngineType.EVEN_FIRE) }
    var stoich by remember(constants) { mutableStateOf(constants?.stoichiometricRatio?.toString() ?: "14.7") }
    var injectorLayout by remember(constants) { mutableStateOf(constants?.injectorLayout ?: InjectorLayout.SEQUENTIAL) }
    var mapSampleMethod by remember(constants) { mutableStateOf(constants?.mapSampleMethod ?: MapSampleMethod.CYCLE_AVERAGE) }
    var mapSwitchPoint by remember(constants) { mutableStateOf(constants?.mapSwitchPoint?.toString() ?: "4000") }
    var channel2Angle by remember(constants) { mutableStateOf(constants?.channel2Angle?.toString() ?: "180") }
    var channel3Angle by remember(constants) { mutableStateOf(constants?.channel3Angle?.toString() ?: "270") }
    var channel4Angle by remember(constants) { mutableStateOf(constants?.channel4Angle?.toString() ?: "360") }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(constants) {
        hasChanges = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    strings["label.engineConstantsTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(strings["label.engineConstantsSubtitle"], style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = controller::loadEngineConstants) { Text(strings["action.loadEcu"]) }
                    FilledTonalButton(
                        onClick = {
                            val updated = EngineConstants(
                                reqFuel = reqFuel.toFloatOrNull() ?: 6.0f,
                                batteryVoltage = batteryVoltage.toFloatOrNull() ?: 12.0f,
                                algorithm = algorithm,
                                squirtsPerCycle = squirtsPerCycle.toIntOrNull() ?: 1,
                                injectorStaging = injectorStaging,
                                engineStroke = engineStroke,
                                numberOfCylinders = numberOfCylinders.toIntOrNull() ?: 4,
                                injectorPortType = injectorPortType,
                                numberOfInjectors = numberOfInjectors.toIntOrNull() ?: 4,
                                engineType = engineType,
                                stoichiometricRatio = stoich.toFloatOrNull() ?: 14.7f,
                                injectorLayout = injectorLayout,
                                mapSampleMethod = mapSampleMethod,
                                mapSwitchPoint = mapSwitchPoint.toIntOrNull() ?: 4000,
                                channel2Angle = channel2Angle.toIntOrNull() ?: 180,
                                channel3Angle = channel3Angle.toIntOrNull() ?: 270,
                                channel4Angle = channel4Angle.toIntOrNull() ?: 360
                            )
                            controller.saveEngineConstants(updated)
                        },
                        enabled = hasChanges
                    ) { Text(strings["action.saveEcu"]) }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(strings["label.fuelAndReference"], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.reqFuelMs"], reqFuel, {
                        reqFuel = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.voltage"], batteryVoltage, {
                        batteryVoltage = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }

                HorizontalDivider()

                Text(strings["label.algorithmAndInjection"], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(strings["label.algorithm"], algorithm.displayName, Algorithm.values().map { it.displayName }) { label ->
                        algorithm = Algorithm.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    DropdownField(strings["label.injectorStaging"], injectorStaging.displayName, InjectorStaging.values().map { it.displayName }) { label ->
                        injectorStaging = InjectorStaging.values().first { it.displayName == label }
                        hasChanges = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(strings["label.stroke"], engineStroke.displayName, EngineStroke.values().map { it.displayName }) { label ->
                        engineStroke = EngineStroke.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    DropdownField(strings["label.portLabel"], injectorPortType.displayName, InjectorPortType.values().map { it.displayName }) { label ->
                        injectorPortType = InjectorPortType.values().first { it.displayName == label }
                        hasChanges = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(strings["label.engineType"], engineType.displayName, EngineType.values().map { it.displayName }) { label ->
                        engineType = EngineType.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    DropdownField(strings["label.layout"], injectorLayout.displayName, InjectorLayout.values().map { it.displayName }) { label ->
                        injectorLayout = InjectorLayout.values().first { it.displayName == label }
                        hasChanges = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.squirtsPerCycle"], squirtsPerCycle, {
                        squirtsPerCycle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.cylinders"], numberOfCylinders, {
                        numberOfCylinders = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.injectors"], numberOfInjectors, {
                        numberOfInjectors = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }

                HorizontalDivider()

                Text(strings["label.mapAndStoich"], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(strings["label.mapSample"], mapSampleMethod.displayName, MapSampleMethod.values().map { it.displayName }) { label ->
                        mapSampleMethod = MapSampleMethod.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    NumberField(strings["label.stoichAfr"], stoich, {
                        stoich = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.mapSwitch"], mapSwitchPoint, {
                        mapSwitchPoint = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }

                HorizontalDivider()

                Text(strings["label.oddfireAngles"], style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.channel2"], channel2Angle, {
                        channel2Angle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.channel3"], channel3Angle, {
                        channel3Angle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.channel4"], channel4Angle, {
                        channel4Angle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TriggerSettingsScreenDesktop(controller: DesktopSpeeduinoController) {
    val settings by controller.triggerSettings.collectAsState()
    val strings = LocalStrings.current
    var triggerAngle by remember(settings) { mutableStateOf(settings?.triggerAngleDeg?.toString() ?: "0") }
    var triggerMultiplier by remember(settings) { mutableStateOf(settings?.triggerAngleMultiplier?.toString() ?: "1") }
    var triggerPattern by remember(settings) { mutableStateOf(settings?.triggerPattern ?: 0) }
    var baseTeeth by remember(settings) { mutableStateOf(settings?.primaryBaseTeeth?.toString() ?: "36") }
    var missingTeeth by remember(settings) { mutableStateOf(settings?.missingTeeth?.toString() ?: "1") }
    var primarySpeed by remember(settings) { mutableStateOf(settings?.primaryTriggerSpeed ?: TriggerSettings.TriggerSpeed.CRANK) }
    var triggerEdge by remember(settings) { mutableStateOf(settings?.triggerEdge ?: TriggerSettings.SignalEdge.RISING) }
    var secondaryEdge by remember(settings) { mutableStateOf(settings?.secondaryTriggerEdge ?: TriggerSettings.SignalEdge.RISING) }
    var secondaryType by remember(settings) { mutableStateOf(settings?.secondaryTriggerType ?: 0) }
    var phaseHigh by remember(settings) { mutableStateOf(settings?.levelForFirstPhaseHigh ?: false) }
    var skipRevs by remember(settings) { mutableStateOf(settings?.skipRevolutions?.toString() ?: "0") }
    var filter by remember(settings) { mutableStateOf(settings?.triggerFilter ?: TriggerSettings.TriggerFilter.OFF) }
    var reSyncEveryCycle by remember(settings) { mutableStateOf(settings?.reSyncEveryCycle ?: false) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        hasChanges = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(strings["label.triggerTitle"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(strings["label.triggerSubtitle"], style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = controller::loadTriggerSettings) { Text(strings["action.loadEcu"]) }
                    FilledTonalButton(
                        onClick = {
                            val updated = TriggerSettings(
                                triggerAngleDeg = triggerAngle.toIntOrNull() ?: 0,
                                triggerAngleMultiplier = triggerMultiplier.toIntOrNull() ?: 1,
                                triggerPattern = triggerPattern,
                                primaryBaseTeeth = baseTeeth.toIntOrNull() ?: 36,
                                missingTeeth = missingTeeth.toIntOrNull() ?: 1,
                                primaryTriggerSpeed = primarySpeed,
                                triggerEdge = triggerEdge,
                                secondaryTriggerEdge = secondaryEdge,
                                secondaryTriggerType = secondaryType,
                                levelForFirstPhaseHigh = phaseHigh,
                                skipRevolutions = skipRevs.toIntOrNull() ?: 0,
                                triggerFilter = filter,
                                reSyncEveryCycle = reSyncEveryCycle
                            )
                            controller.saveTriggerSettings(updated)
                        },
                        enabled = hasChanges
                    ) { Text(strings["action.saveEcu"]) }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.triggerAngle"], triggerAngle, {
                        triggerAngle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.angleMultiplier"], triggerMultiplier, {
                        triggerMultiplier = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }
                DropdownField(
                    strings["label.triggerPattern"],
                    triggerPatternLabel(strings, triggerPattern),
                    triggerPatternOptions(strings)
                ) { label ->
                    triggerPattern = triggerPatternFromLabel(strings, label)
                    hasChanges = true
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.baseTeeth"], baseTeeth, {
                        baseTeeth = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField(strings["label.missingTeeth"], missingTeeth, {
                        missingTeeth = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }
                DropdownField(
                    strings["label.primarySpeed"],
                    primarySpeed.name,
                    TriggerSettings.TriggerSpeed.values().map { it.name }
                ) { label ->
                    primarySpeed = TriggerSettings.TriggerSpeed.valueOf(label)
                    hasChanges = true
                }
                DropdownField(
                    strings["label.triggerEdge"],
                    triggerEdge.name,
                    TriggerSettings.SignalEdge.values().map { it.name }
                ) { label ->
                    triggerEdge = TriggerSettings.SignalEdge.valueOf(label)
                    hasChanges = true
                }
                TriggerVisualizationCardDesktop(
                    totalTeeth = baseTeeth.toIntOrNull() ?: 0,
                    missingTeeth = missingTeeth.toIntOrNull() ?: 0,
                    triggerAngleDeg = triggerAngle.toIntOrNull() ?: 0,
                    triggerAngleMultiplier = triggerMultiplier.toIntOrNull() ?: 0,
                    triggerEdge = triggerEdge,
                    primaryTriggerSpeed = primarySpeed,
                    isMissingToothPattern = triggerPattern == 0
                )
                DropdownField(
                    strings["label.secondaryEdge"],
                    secondaryEdge.name,
                    TriggerSettings.SignalEdge.values().map { it.name }
                ) { label ->
                    secondaryEdge = TriggerSettings.SignalEdge.valueOf(label)
                    hasChanges = true
                }
                DropdownField(
                    strings["label.secondaryPattern"],
                    secondaryPatternLabel(strings, secondaryType),
                    secondaryPatternOptions(strings)
                ) { label ->
                    secondaryType = secondaryPatternFromLabel(strings, label)
                    hasChanges = true
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.skipRevolutions"], skipRevs, {
                        skipRevs = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    DropdownField(
                        strings["label.triggerFilter"],
                        filter.name,
                        TriggerSettings.TriggerFilter.values().map { it.name }
                    ) { label ->
                        filter = TriggerSettings.TriggerFilter.valueOf(label)
                        hasChanges = true
                    }
                }
                ToggleField(strings["label.primaryPhaseHigh"], phaseHigh) {
                    phaseHigh = it
                    hasChanges = true
                }
                ToggleField(strings["label.resyncEveryCycle"], reSyncEveryCycle) {
                    reSyncEveryCycle = it
                    hasChanges = true
                }
            }
        }
    }
}

@Composable
private fun TriggerVisualizationCardDesktop(
    totalTeeth: Int,
    missingTeeth: Int,
    triggerAngleDeg: Int,
    triggerAngleMultiplier: Int,
    triggerEdge: TriggerSettings.SignalEdge,
    primaryTriggerSpeed: TriggerSettings.TriggerSpeed,
    isMissingToothPattern: Boolean
) {
    val strings = LocalStrings.current
    val preview = remember(
        totalTeeth,
        missingTeeth,
        triggerAngleDeg,
        triggerAngleMultiplier,
        triggerEdge,
        primaryTriggerSpeed
    ) {
        TriggerPreviewModelDesktop.create(
            totalTeeth = totalTeeth,
            missingTeeth = missingTeeth,
            triggerAngleDeg = triggerAngleDeg,
            triggerAngleMultiplier = triggerAngleMultiplier,
            triggerEdge = triggerEdge,
            primaryTriggerSpeed = primaryTriggerSpeed
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(strings["label.triggerVisualPreview"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!isMissingToothPattern) {
                Text(strings["label.triggerVisualApproximation"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (preview == null) {
                Text(strings["label.triggerVisualInvalidInput"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Text(strings["label.triggerWheelPreview"], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            TriggerWheelCanvasDesktop(preview)
            Text(strings["label.triggerSignalPreview"], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            TriggerSignalCanvasDesktop(preview)

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendItemDesktop(MaterialTheme.colorScheme.primary, strings["label.syncMarker"])
                LegendItemDesktop(MaterialTheme.colorScheme.tertiary, strings["label.tdcMarker"])
                LegendItemDesktop(MaterialTheme.colorScheme.error, strings["label.missingWindow"])
            }
            Text(strings.format("label.syncToothFormat", preview.syncToothSlot + 1), style = MaterialTheme.typography.bodySmall)
            Text(strings.format("label.effectiveTriggerAngleFormat", preview.effectiveTriggerAngleDeg), style = MaterialTheme.typography.bodySmall)
            Text(strings.format("label.cycleScopeFormat", preview.cycleDegrees), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TriggerWheelCanvasDesktop(preview: TriggerPreviewModelDesktop) {
    val ringColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val toothColor = MaterialTheme.colorScheme.onSurface
    val syncColor = MaterialTheme.colorScheme.primary
    val tdcColor = MaterialTheme.colorScheme.tertiary
    val missingColor = MaterialTheme.colorScheme.error

    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.29f
        val toothInnerRadius = radius * 1.04f
        val toothOuterRadius = radius * 1.27f
        val markerOuterRadius = toothOuterRadius * 1.1f
        val toothStroke = (360f / preview.totalTeeth).coerceIn(1.2f, 8f)

        drawCircle(color = ringColor, radius = radius, center = center, style = Stroke(width = 6.dp.toPx()))

        if (preview.missingTeeth > 0) {
            val gapSweep = preview.slotAngleDeg * preview.missingTeeth
            val arcRadius = toothOuterRadius * 1.01f
            drawArc(
                color = missingColor.copy(alpha = 0.28f),
                startAngle = -90f,
                sweepAngle = gapSweep,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        for (slot in 0 until preview.totalTeeth) {
            if (slot < preview.missingTeeth) continue
            val angle = -90f + (slot * preview.slotAngleDeg)
            val start = polarDesktop(center, toothInnerRadius, angle)
            val end = polarDesktop(center, toothOuterRadius, angle)
            val tint = if (slot == preview.syncToothSlot) syncColor else toothColor
            drawLine(color = tint, start = start, end = end, strokeWidth = toothStroke, cap = StrokeCap.Round)
        }

        val syncAngle = -90f + (preview.syncToothSlot * preview.slotAngleDeg)
        val tdcAngle = -90f + (preview.tdcSlotPosition * preview.slotAngleDeg)
        drawMarkerDesktop(center, radius * 0.7f, markerOuterRadius, syncAngle, syncColor, 4f)
        drawMarkerDesktop(center, radius * 0.5f, markerOuterRadius * 0.95f, tdcAngle, tdcColor, 3f)
    }
}

@Composable
private fun TriggerSignalCanvasDesktop(preview: TriggerPreviewModelDesktop) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
    val syncColor = MaterialTheme.colorScheme.primary
    val tdcColor = MaterialTheme.colorScheme.tertiary
    val signalColor = MaterialTheme.colorScheme.onSurface
    val missingColor = MaterialTheme.colorScheme.error

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val left = 14.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val contentWidth = right - left
        val toothWidth = contentWidth / preview.totalTeeth
        val pulseLeadRatio = 0.12f
        val pulseDutyRatio = 0.44f
        val centerY = (top + bottom) / 2f
        val halfAmplitude = (bottom - top) * 0.25f
        val baselineY = if (preview.triggerEdge == TriggerSettings.SignalEdge.RISING) centerY + halfAmplitude else centerY - halfAmplitude
        val pulseY = if (preview.triggerEdge == TriggerSettings.SignalEdge.RISING) centerY - halfAmplitude else centerY + halfAmplitude

        drawRect(
            color = missingColor.copy(alpha = 0.11f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(toothWidth * preview.missingTeeth, bottom - top)
        )
        drawLine(color = axisColor, start = Offset(left, top), end = Offset(left, bottom), strokeWidth = 1.5f)
        drawLine(color = axisColor, start = Offset(left, baselineY), end = Offset(right, baselineY), strokeWidth = 1.5f)

        val path = Path().apply { moveTo(left, baselineY) }
        for (slot in 0 until preview.totalTeeth) {
            val x0 = left + (slot * toothWidth)
            val x1 = x0 + toothWidth
            val hasTooth = slot >= preview.missingTeeth
            val pulseStart = x0 + (toothWidth * pulseLeadRatio)
            val pulseEnd = pulseStart + (toothWidth * pulseDutyRatio)
            if (!hasTooth) {
                path.lineTo(x1, baselineY)
                continue
            }
            path.lineTo(pulseStart, baselineY)
            path.lineTo(pulseStart, pulseY)
            path.lineTo(pulseEnd, pulseY)
            path.lineTo(pulseEnd, baselineY)
            path.lineTo(x1, baselineY)
        }
        drawPath(path = path, color = signalColor, style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val syncSlotStart = left + (preview.syncToothSlot * toothWidth)
        val syncPulseStart = syncSlotStart + (toothWidth * pulseLeadRatio)
        val syncPulseEnd = syncPulseStart + (toothWidth * pulseDutyRatio)
        val syncX = if (preview.triggerEdge == TriggerSettings.SignalEdge.RISING) syncPulseStart else syncPulseEnd
        val tdcX = left + (preview.tdcSlotPosition / preview.totalTeeth) * contentWidth

        drawLine(color = syncColor, start = Offset(syncX, top), end = Offset(syncX, bottom), strokeWidth = 3f)
        drawLine(color = tdcColor, start = Offset(tdcX, top), end = Offset(tdcX, bottom), strokeWidth = 2.4f)
    }
}

@Composable
private fun LegendItemDesktop(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color = color, shape = MaterialTheme.shapes.extraSmall))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private data class TriggerPreviewModelDesktop(
    val totalTeeth: Int,
    val missingTeeth: Int,
    val syncToothSlot: Int,
    val tdcSlotPosition: Float,
    val triggerEdge: TriggerSettings.SignalEdge,
    val slotAngleDeg: Float,
    val effectiveTriggerAngleDeg: Int,
    val cycleDegrees: Float
) {
    companion object {
        fun create(
            totalTeeth: Int,
            missingTeeth: Int,
            triggerAngleDeg: Int,
            triggerAngleMultiplier: Int,
            triggerEdge: TriggerSettings.SignalEdge,
            primaryTriggerSpeed: TriggerSettings.TriggerSpeed
        ): TriggerPreviewModelDesktop? {
            if (totalTeeth < 2) return null
            val normalizedTotal = totalTeeth.coerceAtMost(255)
            val normalizedMissing = missingTeeth.coerceIn(0, normalizedTotal - 1)
            val syncToothSlot = normalizedMissing % normalizedTotal
            val cycleDegrees = if (primaryTriggerSpeed == TriggerSettings.TriggerSpeed.CAM) 720f else 360f
            val degreesPerSlot = cycleDegrees / normalizedTotal
            val effectiveAngle = triggerAngleDeg * triggerAngleMultiplier
            val tdcSlotPosition = wrapSlotDesktop(syncToothSlot - (effectiveAngle / degreesPerSlot), normalizedTotal.toFloat())
            return TriggerPreviewModelDesktop(
                totalTeeth = normalizedTotal,
                missingTeeth = normalizedMissing,
                syncToothSlot = syncToothSlot,
                tdcSlotPosition = tdcSlotPosition,
                triggerEdge = triggerEdge,
                slotAngleDeg = 360f / normalizedTotal,
                effectiveTriggerAngleDeg = effectiveAngle,
                cycleDegrees = cycleDegrees
            )
        }
    }
}

private fun wrapSlotDesktop(value: Float, total: Float): Float {
    val wrapped = value % total
    return if (wrapped < 0f) wrapped + total else wrapped
}

private fun polarDesktop(center: Offset, radius: Float, angleDeg: Float): Offset {
    val radians = (angleDeg * (kotlin.math.PI / 180f)).toFloat()
    return Offset(
        x = center.x + kotlin.math.cos(radians) * radius,
        y = center.y + kotlin.math.sin(radians) * radius
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarkerDesktop(
    center: Offset,
    radiusStart: Float,
    radiusEnd: Float,
    angleDeg: Float,
    color: Color,
    strokeWidth: Float
) {
    val start = polarDesktop(center, radiusStart, angleDeg)
    val end = polarDesktop(center, radiusEnd, angleDeg)
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

@Composable
private fun SensorsConfigScreenDesktop() {
    val strings = LocalStrings.current
    PlaceholderScreen(strings["route.sensorsConfig"], strings["label.sensorsNotSupported"])
}

@Composable
private fun EngineProtectionScreenDesktop() {
    val strings = LocalStrings.current
    var protectionCut by remember { mutableStateOf(ProtectionCutOption.BOTH) }
    var engineProtectionRpmMin by remember { mutableStateOf("1500") }
    var cutMethod by remember { mutableStateOf(CutMethodOption.FULL) }
    var engineProtectEnabled by remember { mutableStateOf(false) }
    var revLimiterEnabled by remember { mutableStateOf(false) }
    var boostLimitEnabled by remember { mutableStateOf(false) }
    var oilPressureProtectEnabled by remember { mutableStateOf(false) }
    var afrProtectEnabled by remember { mutableStateOf(false) }
    var coolantProtectEnabled by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings["label.engineProtectionTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings["label.engineProtectionSubtitle"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProtectionSectionHeader(strings["label.mainSettings"])
                DropdownField(
                    label = strings["label.engineProtectionCut"],
                    value = protectionCut.label(strings),
                    options = ProtectionCutOption.values().map { it.label(strings) }
                ) { value ->
                    protectionCut = ProtectionCutOption.values().first { it.label(strings) == value }
                    hasChanges = true
                }
                NumberField(
                    label = strings["label.engineProtectionRpmMin"],
                    value = engineProtectionRpmMin,
                    onValueChange = {
                        engineProtectionRpmMin = it
                        hasChanges = true
                    }
                )
                DropdownField(
                    label = strings["label.cutMethod"],
                    value = cutMethod.label(strings),
                    options = CutMethodOption.values().map { it.label(strings) }
                ) { value ->
                    cutMethod = CutMethodOption.values().first { it.label(strings) == value }
                    hasChanges = true
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProtectionSectionHeader(strings["label.activeProtections"])
                ToggleField(strings["label.engineProtect"], engineProtectEnabled) {
                    engineProtectEnabled = it
                    hasChanges = true
                }
                ToggleField(strings["label.revLimiter"], revLimiterEnabled) {
                    revLimiterEnabled = it
                    hasChanges = true
                }
                ToggleField(strings["label.boostLimit"], boostLimitEnabled) {
                    boostLimitEnabled = it
                    hasChanges = true
                }
                ToggleField(strings["label.oilPressureProtect"], oilPressureProtectEnabled) {
                    oilPressureProtectEnabled = it
                    hasChanges = true
                }
                ToggleField(strings["label.afrProtect"], afrProtectEnabled) {
                    afrProtectEnabled = it
                    hasChanges = true
                }
                ToggleField(strings["label.coolantProtect"], coolantProtectEnabled) {
                    coolantProtectEnabled = it
                    hasChanges = true
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { hasChanges = false },
                enabled = hasChanges
            ) { Text(strings["action.save"]) }
            FilledTonalButton(onClick = { hasChanges = false }) { Text(strings["action.loadEcu"]) }
        }
    }
}

private enum class ProtectionCutOption {
    FUEL,
    IGNITION,
    BOTH;

    fun label(strings: Strings): String {
        return when (this) {
            FUEL -> strings["label.cutFuel"]
            IGNITION -> strings["label.cutIgnition"]
            BOTH -> strings["label.cutBoth"]
        }
    }
}

private enum class CutMethodOption {
    FULL,
    PROGRESSIVE;

    fun label(strings: Strings): String {
        return when (this) {
            FULL -> strings["label.cutFull"]
            PROGRESSIVE -> strings["label.cutProgressive"]
        }
    }
}

@Composable
private fun ProtectionSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RealTimeMonitorScreenDesktop(
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?
) {
    val strings = LocalStrings.current
    val logState by controller.logState.collectAsState()
    val logSaveStatus by controller.logSaveStatus.collectAsState()
    val lastSavedLogPath by controller.lastSavedLogPath.collectAsState()
    val intervalMs by controller.streamIntervalMs.collectAsState()
    var selectedInterval by remember(intervalMs) { mutableStateOf(intervalMs.toString()) }
    var fileName by remember(strings) { mutableStateOf(strings["label.logFilenamePrefix"]) }
    var selectedSignals by remember { mutableStateOf(DefaultSelectedLogSignals) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    strings["label.monitorTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(strings["label.monitorSubtitle"], style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.intervalMs"], selectedInterval, { value ->
                        selectedInterval = value
                        value.toLongOrNull()?.let(controller::updateStreamInterval)
                    }, Modifier.width(160.dp))
                    FilledTonalButton(
                        onClick = {
                            if (logState.isRecording) {
                                controller.stopLogCapture()
                            } else {
                                controller.startLogCapture(intervalMs)
                            }
                        }
                    ) {
                        Text(
                            if (logState.isRecording) {
                                strings["action.stopCapture"]
                            } else {
                                strings["action.startCapture"]
                            }
                        )
                    }
                }
                Text(
                    text = strings.format("label.captureCount", logState.samplesCaptured),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings["label.currentStatus"], style = MaterialTheme.typography.titleMedium)
                InfoRow(strings["label.rpm"], liveData?.rpm?.toString() ?: strings["label.noData"])
                InfoRow(strings["label.map"], liveData?.mapPressure?.toString() ?: strings["label.noData"])
                InfoRow(strings["label.tps"], liveData?.tps?.toString() ?: strings["label.noData"])
                InfoRow(strings["label.coolant"], liveData?.coolantTemp?.toString() ?: strings["label.noData"])
                InfoRow(strings["label.iat"], liveData?.intakeTemp?.toString() ?: strings["label.noData"])
                InfoRow(
                    strings["label.battery"],
                    liveData?.batteryVoltage?.let { String.format("%.1f", it) } ?: strings["label.noData"]
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings["action.exportLog"], style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(strings["label.fileName"]) },
                    singleLine = true,
                    modifier = Modifier.width(280.dp)
                )
                Text(
                    text = strings["label.logSignalsSelect"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogExportSignals.forEach { signal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = selectedSignals.contains(signal.key),
                                onCheckedChange = { checked ->
                                    selectedSignals = if (checked) {
                                        selectedSignals + signal.key
                                    } else {
                                        selectedSignals - signal.key
                                    }
                                }
                            )
                            Text(strings[signal.labelKey], style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                FilledTonalButton(onClick = { controller.saveLogSnapshot(fileName, selectedSignals) }) {
                    Text(strings["action.saveCsv"])
                }
                if (!logSaveStatus.isNullOrBlank()) {
                    Text(
                        text = logSaveStatus!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (logSaveStatus!!.contains("Error", ignoreCase = true) || logSaveStatus!!.contains("erro", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (!lastSavedLogPath.isNullOrBlank()) {
                    Text(
                        text = strings.format("label.lastSavedLogPath", lastSavedLogPath!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LogViewerScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val snapshot by controller.logSnapshot.collectAsState()
    val entries = snapshot?.entries.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    strings["label.snapshotTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(strings["label.snapshotSubtitle"], style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = controller::captureSnapshot) { Text(strings["action.refreshSnapshot"]) }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entries.isEmpty()) {
                    Text(strings["label.noLogCaptured"], style = MaterialTheme.typography.bodyMedium)
                } else {
                    val series = remember(entries, strings) { buildLogSeries(entries, strings) }
                    var selected by remember(series) {
                        mutableStateOf(series.take(3).map { it.name }.toSet())
                    }

                    LogViewerFiltersRow(
                        signals = series.map { it.name },
                        selectedSignals = selected,
                        onToggle = { name ->
                            selected = if (selected.contains(name)) {
                                selected - name
                            } else {
                                selected + name
                            }
                        }
                    )

                    LogViewerChart(
                        series = series.filter { selected.contains(it.name) },
                        totalSamples = entries.size
                    )

                    LogMetadataSummary(entries = entries)
                }
            }
        }
    }
}

@Composable
private fun LogAnalyzerScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val currentVe by controller.veTable.collectAsState()
    val currentAfr by controller.afrTable.collectAsState()
    val analyzerResult by controller.analyzerResult.collectAsState()
    val analyzerBusy by controller.analyzerBusy.collectAsState()
    val analyzerError by controller.analyzerError.collectAsState()
    val analyzerLogFile by controller.analyzerLogFile.collectAsState()
    var strategy by remember { mutableStateOf(TuningStrategy.CONSERVATIVE) }
    var selectedCell by remember(analyzerResult) { mutableStateOf<CellRef?>(null) }
    var selectedClusterId by remember(analyzerResult) { mutableStateOf<String?>(null) }
    var includedClusters by remember(analyzerResult) {
        mutableStateOf(analyzerResult?.clusters?.map { it.id }?.toSet().orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    strings["label.logAnalyzerTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(strings["label.logAnalyzerSubtitle"], style = MaterialTheme.typography.bodyMedium)
                Text(
                    analyzerLogFile ?: strings["label.logAnalyzerNoFile"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            val file = chooseOpenFile(strings["label.logAnalyzerOpenTitle"])
                            controller.selectAnalyzerLogFile(file?.absolutePath)
                        }
                    ) { Text(strings["action.open"]) }
                    FilledTonalButton(
                        onClick = { controller.analyzeLogFile(strategy) },
                        enabled = !analyzerBusy && !analyzerLogFile.isNullOrBlank() && currentVe != null && currentAfr != null
                    ) { Text(strings["action.analyze"]) }
                    FilledTonalButton(
                        onClick = { controller.applyAnalyzerToVe(strategy, includedClusters) },
                        enabled = !analyzerBusy && analyzerResult != null && currentVe != null
                    ) { Text(strings["action.applyToVe"]) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings["label.strategy"], style = MaterialTheme.typography.bodyMedium)
                    StrategyChip("Conservative", strategy == TuningStrategy.CONSERVATIVE) {
                        strategy = TuningStrategy.CONSERVATIVE
                    }
                    StrategyChip("Standard", strategy == TuningStrategy.STANDARD) {
                        strategy = TuningStrategy.STANDARD
                    }
                    StrategyChip("Aggressive", strategy == TuningStrategy.AGGRESSIVE) {
                        strategy = TuningStrategy.AGGRESSIVE
                    }
                }
                analyzerError?.let {
                    Text(
                        strings.format("label.errorWithValue", it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (analyzerBusy) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            strings["label.analyzerLoading"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val signals = analyzerResult?.signalStatus
        if (signals != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(strings["label.analyzerSignals"], style = MaterialTheme.typography.titleMedium)
                    AnalyzerSignalRow("RPM", signals.hasRpm)
                    AnalyzerSignalRow("Load", signals.hasLoad)
                    AnalyzerSignalRow("AFR", signals.hasAfr)
                    AnalyzerSignalRow("AFR Target", signals.hasAfrTarget)
                }
            }
        }

        analyzerResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(strings["label.logAnalyzerSummary"], style = MaterialTheme.typography.titleMedium)
                    Text(strings.format("label.samplesUsed", result.summary.usedSamples, result.summary.totalSamples))
                    Text(strings.format("label.clustersCount", result.clusters.size))
                    Text(strings.format("label.durationSeconds", result.summary.durationSeconds))
                    result.summary.rpmRange?.let {
                        Text(strings.format("label.rpmRange", it.first, it.last))
                    }
                    result.summary.loadRange?.let {
                        Text(strings.format("label.loadRange", it.first, it.last, result.summary.loadLabel))
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings["label.heatmapTitle"], style = MaterialTheme.typography.titleMedium)
                    val highlighted = result.clusters.firstOrNull { it.id == selectedClusterId }?.cells?.toSet().orEmpty()
                    AnalyzerHeatmapGrid(
                        suggestions = result.cellSuggestions,
                        maxChangePct = strategy.maxChangePct,
                        highlightedCells = highlighted,
                        onCellSelected = { selectedCell = it }
                    )
                    selectedCell?.let { cell ->
                        val suggestion = result.cellSuggestions.getOrNull(cell.row)?.getOrNull(cell.col)
                        if (suggestion != null) {
                            val deltaPct = (suggestion.deltaPct * 100).toInt()
                            Text(
                                strings.format(
                                    "label.heatmapCellInfo",
                                    result.suggestedVeTable.rpmBins[cell.col],
                                    result.suggestedVeTable.loadBins[cell.row],
                                    deltaPct,
                                    suggestion.hitCount
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings["label.clusterSuggestions"], style = MaterialTheme.typography.titleMedium)
                    if (result.clusters.isEmpty()) {
                        Text(strings["label.noSuggestions"], style = MaterialTheme.typography.bodySmall)
                    } else {
                        result.clusters.forEach { cluster ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cluster.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(cluster.reason, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        strings.format("label.clusterDelta", (cluster.avgDeltaPct * 100).toInt(), cluster.avgHits),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(
                                            onClick = {
                                                selectedClusterId = if (selectedClusterId == cluster.id) null else cluster.id
                                            }
                                        ) { Text(strings["label.preview"]) }
                                        Switch(
                                            checked = includedClusters.contains(cluster.id),
                                            onCheckedChange = {
                                                includedClusters = if (includedClusters.contains(cluster.id)) {
                                                    includedClusters - cluster.id
                                                } else {
                                                    includedClusters + cluster.id
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val beforePath by controller.beforeAfterBeforeLogPath.collectAsState()
    val afterPath by controller.beforeAfterAfterLogPath.collectAsState()
    val busy by controller.beforeAfterBusy.collectAsState()
    val error by controller.beforeAfterError.collectAsState()
    val result by controller.beforeAfterResult.collectAsState()
    var selectedCell by remember(result) { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings["label.beforeAfterTitle"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(strings["label.beforeAfterSubtitle"], style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            val file = chooseOpenFile(strings["label.beforeAfterPickBefore"])
                            controller.setBeforeAfterBeforeLogPath(file?.absolutePath)
                        }
                    ) { Text(strings["label.beforeAfterBefore"]) }
                    Text(beforePath ?: strings["label.logAnalyzerNoFile"], style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            val file = chooseOpenFile(strings["label.beforeAfterPickAfter"])
                            controller.setBeforeAfterAfterLogPath(file?.absolutePath)
                        }
                    ) { Text(strings["label.beforeAfterAfter"]) }
                    Text(afterPath ?: strings["label.logAnalyzerNoFile"], style = MaterialTheme.typography.bodySmall)
                }
                FilledTonalButton(
                    onClick = { controller.compareBeforeAfterLogs() },
                    enabled = !busy && !beforePath.isNullOrBlank() && !afterPath.isNullOrBlank()
                ) { Text(strings["action.compare"]) }
                error?.let {
                    Text(strings.format("label.errorWithValue", it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (busy) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(strings["label.analyzerLoading"], style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        result?.let { compare ->
            BeforeAfterSummary(strings, compare)
            BeforeAfterHeatmap(compare, selectedCell) { row, col -> selectedCell = row to col }
            selectedCell?.let { (row, col) ->
                val cell = compare.cells.getOrNull(row)?.getOrNull(col)
                if (cell != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(strings.format("label.beforeAfterCell", row + 1, col + 1), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(strings.format("label.beforeAfterCellBefore", cell.beforeAvgAbsError ?: 0.0, cell.beforeSamples), style = MaterialTheme.typography.bodySmall)
                            Text(strings.format("label.beforeAfterCellAfter", cell.afterAvgAbsError ?: 0.0, cell.afterSamples), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterSummary(strings: Strings, result: LogCompareResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(strings["label.beforeAfterSummary"], style = MaterialTheme.typography.titleMedium)
            Text(strings.format("label.beforeAfterMetricAfrError", result.beforeSummary.avgAbsError, result.afterSummary.avgAbsError))
            Text(strings.format("label.beforeAfterMetricLean", result.beforeSummary.leanTimeRatio * 100.0, result.afterSummary.leanTimeRatio * 100.0))
            Text(strings.format("label.beforeAfterMetricRich", result.beforeSummary.richTimeRatio * 100.0, result.afterSummary.richTimeRatio * 100.0))
            Text(
                strings.format(
                    "label.beforeAfterMetricCoverage",
                    result.beforeSummary.coverageCells,
                    result.beforeSummary.totalCells,
                    result.afterSummary.coverageCells,
                    result.afterSummary.totalCells
                )
            )
        }
    }
}

@Composable
private fun BeforeAfterHeatmap(
    result: LogCompareResult,
    selectedCell: Pair<Int, Int>?,
    onSelectCell: (Int, Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(LocalStrings.current["label.beforeAfterHeatmap"], style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                result.cells.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEachIndexed { colIndex, cell ->
                            val color = when (cell.state) {
                                LogHeatCellState.IMPROVED -> Color(0xFF16A34A)
                                LogHeatCellState.WORSE -> Color(0xFFDC2626)
                                LogHeatCellState.UNCHANGED -> Color(0xFF9CA3AF)
                                LogHeatCellState.NOT_ENOUGH -> Color(0xFFE5E7EB)
                            }
                            val border = if (selectedCell?.first == rowIndex && selectedCell.second == colIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(2.dp, border, RoundedCornerShape(2.dp))
                                    .background(color, RoundedCornerShape(2.dp))
                                    .clickable { onSelectCell(rowIndex, colIndex) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StrategyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            modifier = Modifier.clickable { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun AnalyzerSignalRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            if (ok) "OK" else "Missing",
            style = MaterialTheme.typography.bodySmall,
            color = if (ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun AnalyzerHeatmapGrid(
    suggestions: List<List<com.speeduino.manager.tuning.CellSuggestion?>>,
    maxChangePct: Double,
    highlightedCells: Set<CellRef>,
    onCellSelected: (CellRef) -> Unit
) {
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        suggestions.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { colIndex, cell ->
                    val color = when {
                        cell == null -> Color(0xFFE5E7EB)
                        cell.deltaPct >= 0 -> analyzerHeatColor(Color(0xFFDC2626), abs(cell.deltaPct) / maxChangePct)
                        else -> analyzerHeatColor(Color(0xFF2563EB), abs(cell.deltaPct) / maxChangePct)
                    }
                    val border = if (highlightedCells.contains(CellRef(rowIndex, colIndex))) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(2.dp, border, shape = RoundedCornerShape(2.dp))
                            .background(color, shape = RoundedCornerShape(2.dp))
                            .clickable { onCellSelected(CellRef(rowIndex, colIndex)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun analyzerHeatColor(base: Color, intensity: Double): Color {
    val alpha = intensity.coerceIn(0.2, 1.0).toFloat()
    return base.copy(alpha = alpha)
}

private data class LogSeries(
    val name: String,
    val color: Color,
    val values: List<Float>,
    val min: Float,
    val max: Float
)

private fun buildLogSeries(
    entries: List<com.speeduino.manager.model.logging.LiveLogEntry>,
    strings: Strings
): List<LogSeries> {
    if (entries.isEmpty()) return emptyList()

    fun build(
        name: String,
        color: Color,
        extractor: (com.speeduino.manager.model.logging.LiveLogEntry) -> Float
    ): LogSeries {
        val values = entries.map(extractor)
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 0f
        return LogSeries(name, color, values, min, max)
    }

    return listOf(
        build(strings["label.rpm"], Color(0xFF2F6B5F)) { it.rpm.toFloat() },
        build(strings["label.map"], Color(0xFFC37B2C)) { it.mapKpa.toFloat() },
        build(strings["label.tps"], Color(0xFF5C6BC0)) { it.tps.toFloat() },
        build(strings["label.coolantShort"], Color(0xFFB04A3B)) { it.coolantTempC.toFloat() },
        build(strings["label.iat"], Color(0xFF8D6E63)) { it.intakeTempC.toFloat() },
        build(strings["label.batt"], Color(0xFF388E3C)) { it.batteryDeciVolt.toFloat() / 10f },
        build(strings["label.advance"], Color(0xFF6D4C41)) { it.advanceDeg.toFloat() },
        build(strings["label.o2"], Color(0xFF00897B)) { it.o2.toFloat() }
    )
}

@Composable
private fun LogViewerFiltersRow(
    signals: List<String>,
    selectedSignals: Set<String>,
    onToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        signals.forEach { name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (selectedSignals.contains(name)) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggle(name) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                androidx.compose.material3.Checkbox(
                    checked = selectedSignals.contains(name),
                    onCheckedChange = { onToggle(name) }
                )
                Text(name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LogViewerChart(series: List<LogSeries>, totalSamples: Int) {
    val strings = LocalStrings.current
    if (series.isEmpty()) {
        Text(strings["label.selectSignals"], style = MaterialTheme.typography.bodyMedium)
        return
    }

    val maxPoints = 800
    val downsampled = series.map { it.copy(values = downsample(it.values, maxPoints)) }
    val chartWidth = maxOf(720.dp, (downsampled.first().values.size * 4).dp)
    val chartHeight = 320.dp
    val horizontal = rememberScrollState()
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .horizontalScroll(horizontal)
            ) {
                Canvas(modifier = Modifier.width(chartWidth).height(chartHeight)) {
                    val padding = 24f
                    val width = size.width - padding * 2
                    val height = size.height - padding * 2

                    drawLine(
                        color = axisColor,
                        start = Offset(padding, padding),
                        end = Offset(padding, padding + height),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = axisColor,
                        start = Offset(padding, padding + height),
                        end = Offset(padding + width, padding + height),
                        strokeWidth = 1.5f
                    )

                    downsampled.forEach { s ->
                        val min = s.min
                        val max = if (s.max == s.min) s.min + 1f else s.max
                        val values = s.values
                        val stepX = if (values.size <= 1) width else width / (values.size - 1)
                        val path = Path()
                        values.forEachIndexed { index, value ->
                            val normalized = (value - min) / (max - min)
                            val x = padding + stepX * index
                            val y = padding + height - (normalized * height)
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = s.color,
                            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(horizontal),
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            downsampled.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(s.color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(s.color, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${s.name} ${formatRange(s.min, s.max)}", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(strings.format("label.samples", totalSamples), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LogMetadataSummary(entries: List<com.speeduino.manager.model.logging.LiveLogEntry>) {
    if (entries.isEmpty()) return
    val strings = LocalStrings.current
    val start = entries.first().timestampMs
    val end = entries.last().timestampMs
    val durationSec = (end - start).coerceAtLeast(0) / 1000

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(strings.format("label.duration", durationSec), style = MaterialTheme.typography.labelLarge)
        Text(strings.format("label.start", start), style = MaterialTheme.typography.labelLarge)
        Text(strings.format("label.end", end), style = MaterialTheme.typography.labelLarge)
    }
}

private fun downsample(values: List<Float>, maxPoints: Int): List<Float> {
    if (values.size <= maxPoints) return values
    val step = values.size.toFloat() / maxPoints
    return List(maxPoints) { index ->
        val idx = (index * step).toInt().coerceIn(0, values.size - 1)
        values[idx]
    }
}

private fun formatRange(min: Float, max: Float): String {
    return if (min == max) {
        String.format("%.1f", min)
    } else {
        String.format("%.1f-%.1f", min, max)
    }
}

@Composable
private fun BaseMapWizardScreenDesktop(controller: DesktopSpeeduinoController) {
    val engineConstants by controller.engineConstants.collectAsState()
    val strings = LocalStrings.current
    var cylinders by remember { mutableStateOf("4") }
    var displacement by remember { mutableStateOf("2000") }
    var maxRpm by remember { mutableStateOf("6500") }
    var compression by remember { mutableStateOf("10.5") }
    var injectorFlow by remember { mutableStateOf("28") }
    var mapMax by remember { mutableStateOf("110") }
    var fuelType by remember { mutableStateOf(FuelType.GASOLINE) }
    var richness by remember { mutableStateOf("0.0") }
    var advanceOffset by remember { mutableStateOf("0.0") }
    var aggressiveness by remember { mutableStateOf("0.0") }
    var generated by remember { mutableStateOf<GeneratedBaseMap?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    strings["label.baseMapTitle"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(strings["label.baseMapSubtitle"], style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.cylinders"], cylinders, { cylinders = it }, Modifier.width(140.dp))
                    NumberField(strings["label.displacementCc"], displacement, { displacement = it }, Modifier.width(160.dp))
                    NumberField(strings["label.rpmMax"], maxRpm, { maxRpm = it }, Modifier.width(140.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.compression"], compression, { compression = it }, Modifier.width(140.dp))
                    NumberField(strings["label.injectorFlow"], injectorFlow, { injectorFlow = it }, Modifier.width(160.dp))
                    NumberField(strings["label.mapMaxKpa"], mapMax, { mapMax = it }, Modifier.width(140.dp))
                }
                DropdownField(
                    strings["label.fuel"],
                    fuelType.name,
                    FuelType.values().map { it.name }
                ) { label ->
                    fuelType = FuelType.valueOf(label)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(strings["label.richness"], richness, { richness = it }, Modifier.width(140.dp))
                    NumberField(strings["label.advanceOffset"], advanceOffset, { advanceOffset = it }, Modifier.width(160.dp))
                    NumberField(strings["label.highLoadAgg"], aggressiveness, { aggressiveness = it }, Modifier.width(160.dp))
                }
                FilledTonalButton(
                    onClick = {
                        val profile = EngineProfile(
                            cylinders = cylinders.toIntOrNull() ?: 4,
                            displacementCc = displacement.toDoubleOrNull() ?: 2000.0,
                            maxRpm = maxRpm.toIntOrNull() ?: 6500,
                            compressionRatio = compression.toDoubleOrNull() ?: 10.5,
                            fuelType = fuelType,
                            injectorFlowLbsPerHour = injectorFlow.toDoubleOrNull() ?: 28.0,
                            mapMaxKpa = mapMax.toIntOrNull() ?: 110
                        )
                        val adjustments = BaseMapAdjustments(
                            richness = richness.toDoubleOrNull() ?: 0.0,
                            advanceOffset = advanceOffset.toDoubleOrNull() ?: 0.0,
                            highLoadAggressiveness = aggressiveness.toDoubleOrNull() ?: 0.0
                        )
                        generated = BaseMapGenerator().generate(profile, engineConstants, adjustments)
                    }
                ) { Text(strings["action.generateMaps"]) }
            }
        }

        generated?.let { map ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings["label.baseMapResult"], style = MaterialTheme.typography.titleMedium)
                    Text(strings.format("label.reqFuel", map.engineConstants.reqFuel))
                    Text(strings.format("label.stoich", map.engineConstants.stoichiometricRatio))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { controller.applyGeneratedBaseMap(map) }) { Text(strings["action.applyAll"]) }
                        FilledTonalButton(onClick = { controller.applyGeneratedBaseMap(map, writeConstants = false) }) { Text(strings["action.applyMaps"]) }
                        FilledTonalButton(onClick = { controller.applyGeneratedBaseMap(map, writeTables = false) }) { Text(strings["action.applyConstants"]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = strings["app.openDropdown"]
                    )
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(260.dp),
                properties = PopupProperties(focusable = true)
            ) {
                val items = if (options.isEmpty()) listOf(strings["app.noOptions"]) else options
                items.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            if (options.isNotEmpty()) {
                                onValueChange(option)
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun ToggleField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun triggerPatternOptions(strings: Strings): List<String> {
    return listOf(
        strings["trigger.missingTooth"],
        strings["trigger.basicDistributor"],
        strings["trigger.dualWheel"],
        strings["trigger.gm7x"],
        strings["trigger.4g63"],
        strings["trigger.gm24x"],
        strings["trigger.jeep2000"],
        strings["trigger.audi135"],
        strings["trigger.hondaD17"],
        strings["trigger.miata9905"],
        strings["trigger.mazdaAu"],
        strings["trigger.non360Dual"],
        strings["trigger.nissan360"],
        strings["trigger.subaru67"],
        strings["trigger.daihatsu1"],
        strings["trigger.harleyEvo"],
        strings["trigger.36_2_2_2"],
        strings["trigger.36_2_1"],
        strings["trigger.dsm420a"],
        strings["trigger.weberMarelli"],
        strings["trigger.fordSt170"],
        strings["trigger.drz400"],
        strings["trigger.chryslerNgc"],
        strings["trigger.yamahaVmax1990"],
        strings["trigger.renix"],
        strings["trigger.roverMems"],
        strings["trigger.k6a"],
        strings.format("label.patternValue", 27),
        strings.format("label.patternValue", 28),
        strings.format("label.patternValue", 29),
        strings.format("label.patternValue", 30),
        strings.format("label.patternValue", 31)
    )
}

private fun triggerPatternLabel(strings: Strings, value: Int): String {
    return triggerPatternOptions(strings).getOrNull(value) ?: strings.format("label.pattern", value)
}

private fun triggerPatternFromLabel(strings: Strings, label: String): Int {
    val index = triggerPatternOptions(strings).indexOf(label)
    return if (index >= 0) index else label.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private fun secondaryPatternOptions(strings: Strings): List<String> {
    return listOf(
        strings["trigger.singleToothCam"],
        strings["trigger.4_1_cam"],
        strings["label.pollLevel"],
        strings["trigger.rover532cam"],
        strings["trigger.toyota3tooth"]
    )
}

private fun secondaryPatternLabel(strings: Strings, value: Int): String {
    return secondaryPatternOptions(strings).getOrNull(value) ?: strings.format("label.typeWithValue", value)
}

private fun secondaryPatternFromLabel(strings: Strings, label: String): Int {
    val index = secondaryPatternOptions(strings).indexOf(label)
    return if (index >= 0) index else label.filter { it.isDigit() }.toIntOrNull() ?: 0
}

@Composable
private fun ConnectionCard(
    host: String,
    port: String,
    portIsValid: Boolean,
    connectionType: ConnectionType,
    serialPort: String,
    baudRate: String,
    serialPorts: List<SerialPortInfo>,
    connectionState: ConnectionState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectionTypeChange: (ConnectionType) -> Unit,
    onSerialPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit,
    onToggleConnection: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings["label.connectionScreenTitle"],
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            DropdownField(
                label = strings["label.connectionType"],
                value = connectionType.label(strings),
                options = ConnectionType.values().map { it.label(strings) }
            ) { label ->
                onConnectionTypeChange(ConnectionType.values().first { it.label(strings) == label })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (connectionType) {
                    ConnectionType.TCP -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            label = { Text(strings["label.host"]) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = onPortChange,
                            label = { Text(strings["label.port"]) },
                            singleLine = true,
                            modifier = Modifier.width(140.dp),
                            isError = port.isNotEmpty() && !portIsValid,
                            supportingText = {
                                if (port.isNotEmpty() && !portIsValid) {
                                    Text(strings["label.portInvalid"])
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    ConnectionType.USB,
                    ConnectionType.BLUETOOTH -> {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DropdownField(
                                label = strings["label.serialPort"],
                                value = serialPorts.firstOrNull { it.systemPortName == serialPort }?.displayName
                                    ?: if (serialPort.isBlank()) strings["label.none"] else serialPort,
                                options = serialPorts.map { it.displayName }
                            ) { label ->
                                val selected = serialPorts.firstOrNull { it.displayName == label }
                                if (selected != null) onSerialPortChange(selected.systemPortName)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = baudRate,
                            onValueChange = onBaudRateChange,
                            label = { Text(strings["label.baud"]) },
                            singleLine = true,
                            modifier = Modifier.width(140.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                FilledTonalButton(
                    onClick = onToggleConnection,
                    enabled = connectionState.isConnected || when (connectionType) {
                        ConnectionType.TCP -> portIsValid
                        ConnectionType.USB,
                        ConnectionType.BLUETOOTH -> serialPort.isNotBlank()
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        if (connectionState.isConnected) {
                            strings["action.disconnect"]
                        } else {
                            strings["action.connect"]
                        }
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings["status.label"],
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(connectionState = connectionState)
            }
        }
    }
}

@Composable
private fun StatusPill(connectionState: ConnectionState) {
    val strings = LocalStrings.current
    val isConnected = connectionState.isConnected
    val background = if (isConnected) Color(0xFFE0F2E9) else Color(0xFFFDE9E4)
    val content = if (isConnected) Color(0xFF1F5F3D) else Color(0xFF7A3626)
    val message = when (connectionState.status) {
        ConnectionStatus.Connected -> strings["status.connected"]
        ConnectionStatus.Disconnected -> strings["status.disconnected"]
        ConnectionStatus.Connecting -> strings["status.connecting"]
        ConnectionStatus.Failed -> strings.format("status.failed", connectionState.detail ?: "")
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, content.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(8.dp)
                    .background(content, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GaugeGrid(
    gauges: List<GaugeSpec>,
    availableSignals: List<GaugeSpec>,
    onChangeGauge: (Int, String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 900.dp) 2 else 4
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) {
            items(gauges.size) { index ->
                GaugeCard(
                    spec = gauges[index],
                    options = availableSignals,
                    onChangeSignal = { key -> onChangeGauge(index, key) }
                )
            }
        }
    }
}

@Composable
private fun GaugeCard(
    spec: GaugeSpec,
    options: List<GaugeSpec>,
    onChangeSignal: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onChangeSignal(option.key)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val sweep = ((spec.value - spec.min) / (spec.max - spec.min)).coerceIn(0f, 1f) * 240f
                val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                val fillColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(110.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = fillColor,
                        startAngle = 150f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = spec.display,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = spec.unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(items: List<StatItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private data class GaugeSpec(
    val key: String,
    val label: String,
    val value: Float,
    val min: Float,
    val max: Float,
    val unit: String,
    val display: String
)

private fun dashboardGaugeSignals(liveData: SpeeduinoLiveData?, strings: Strings): List<GaugeSpec> {
    fun valueOrZero(value: Float?): Float = value ?: 0f
    fun display0(value: Float?): String = if (value == null) strings["label.noData"] else value.toInt().toString()
    fun display1(value: Float?): String = if (value == null) strings["label.noData"] else String.format(Locale.US, "%.1f", value)

    val afrMeasured = liveData?.afrMeasured?.toFloat()
    val afrTarget = liveData?.afrTarget?.toFloat()
    return listOf(
        GaugeSpec("rpm", strings["label.rpm"], valueOrZero(liveData?.rpm?.toFloat()), 0f, 7000f, strings["unit.rpm"], display0(liveData?.rpm?.toFloat())),
        GaugeSpec("map", strings["label.map"], valueOrZero(liveData?.mapPressure?.toFloat()), 0f, 250f, strings["unit.kpa"], display0(liveData?.mapPressure?.toFloat())),
        GaugeSpec("tps", strings["label.tps"], valueOrZero(liveData?.tps?.toFloat()), 0f, 100f, strings["unit.percent"], display0(liveData?.tps?.toFloat())),
        GaugeSpec("coolant", strings["label.coolant"], valueOrZero(liveData?.coolantTemp?.toFloat()), -20f, 120f, strings["unit.celsius"], display0(liveData?.coolantTemp?.toFloat())),
        GaugeSpec("iat", strings["label.iat"], valueOrZero(liveData?.intakeTemp?.toFloat()), -20f, 120f, strings["unit.celsius"], display0(liveData?.intakeTemp?.toFloat())),
        GaugeSpec("battery", strings["label.battery"], valueOrZero(liveData?.batteryVoltage?.toFloat()), 8f, 16f, "V", display1(liveData?.batteryVoltage?.toFloat())),
        GaugeSpec("advance", strings["label.advance"], valueOrZero(liveData?.advance?.toFloat()), -20f, 60f, "deg", display0(liveData?.advance?.toFloat())),
        GaugeSpec("afr", strings["label.afr"], valueOrZero(afrMeasured), 8f, 22f, strings["label.afr"], display1(afrMeasured)),
        GaugeSpec("afr_target", strings["label.afrTarget"], valueOrZero(afrTarget), 8f, 22f, strings["label.afr"], display1(afrTarget))
    )
}

private data class StatItem(
    val label: String,
    val value: String
)

private data class LogExportSignal(
    val key: String,
    val header: String,
    val labelKey: String
)

private val LogExportSignals = listOf(
    LogExportSignal("rpm", "rpm", "label.rpm"),
    LogExportSignal("map", "map_kpa", "label.map"),
    LogExportSignal("tps", "tps", "label.tps"),
    LogExportSignal("coolant", "coolant_c", "label.coolant"),
    LogExportSignal("iat", "iat_c", "label.iat"),
    LogExportSignal("battery", "battery_v", "label.battery"),
    LogExportSignal("advance", "advance_deg", "label.advance"),
    LogExportSignal("afr", "afr", "label.afr"),
    LogExportSignal("afr_target", "afr_target", "label.afrTarget")
)

private val DefaultSelectedLogSignals: Set<String> = LogExportSignals.map { it.key }.toSet()


private enum class ConnectionStatus {
    Connected,
    Disconnected,
    Connecting,
    Failed
}

private data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val detail: String? = null
) {
    val isConnected: Boolean
        get() = status == ConnectionStatus.Connected
}

private data class ConfigSyncState(
    val isBusy: Boolean = false,
    val progressPercent: Int = 0,
    val message: String? = null,
    val lastSessionDir: File? = null
)

private data class RestoreOutcome(
    val warnings: List<String>,
    val inconsistentPages: List<Byte>,
    val completed: Boolean
)

private data class SyncPrompt(
    val localSessionDir: File,
    val ecuSessionDir: File
)

private enum class ConnectionType(val labelKey: String) {
    TCP("label.connectionTypeTcp"),
    USB("label.connectionTypeUsb"),
    BLUETOOTH("label.connectionTypeBluetooth");

    fun label(strings: Strings): String = strings[labelKey]
}

private data class SerialPortInfo(
    val systemPortName: String,
    val displayName: String
)

private class DesktopSpeeduinoController(
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

    private val _ignitionTable = MutableStateFlow<IgnitionTable?>(null)
    val ignitionTable = _ignitionTable.asStateFlow()

    private val _afrTable = MutableStateFlow<AfrTable?>(null)
    val afrTable = _afrTable.asStateFlow()

    private val _firmwareInfo = MutableStateFlow<FirmwareInfo?>(null)
    val firmwareInfo = _firmwareInfo.asStateFlow()

    private val _productString = MutableStateFlow<String?>(null)
    val productString = _productString.asStateFlow()

    private val _connectionInfo = MutableStateFlow<String?>(null)
    val connectionInfo = _connectionInfo.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

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
    private var client: SpeeduinoClient? = null
    private var localSessionDir: File? = null
    private var ecuSessionDir: File? = null
    private val beforeAfterComparator = BeforeAfterLogComparator()

    fun connectTcp(host: String, port: Int) {
        connectInternal(SpeeduinoTcpConnection(host, port))
    }

    fun connectSerial(portDescriptor: String, baudRate: Int) {
        connectInternal(SpeeduinoSerialConnection(portDescriptor, baudRate))
    }

    private fun connectInternal(newConnection: ISpeeduinoConnection) {
        disconnect()

        connection = newConnection
        client = SpeeduinoClient(
            connection = checkNotNull(connection),
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
        _connectionState.value = ConnectionState(ConnectionStatus.Connecting)

        pollingJob = scope.launch(Dispatchers.IO) {
            try {
                client?.connect()
                _firmwareInfo.value = client?.getFirmwareInfoCached()
                _productString.value = client?.getProductString()
                _connectionInfo.value = client?.getConnectionInfo()
                client?.startLiveDataStream(_streamIntervalMs.value)
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
            _ignitionTable.value = null
            _afrTable.value = null
        }
        _syncPrompt.value = null
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
                    restoreConfigToEcu(prompt.localSessionDir, stopOnRangeErr = true)
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

    fun loadVeTable() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _veTable.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readVeTable()
                } else {
                    localSessionDir?.let { configManager.loadVeTable(it) }
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveVeTable(table: VeTable) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeVeTable(table)
                }
                updateLocalVeTable(table)
                _veTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadIgnitionTable() {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                _ignitionTable.value = if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.readIgnitionTable()
                } else {
                    localSessionDir?.let { configManager.loadIgnitionTable(it) }
                }
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveIgnitionTable(table: IgnitionTable) {
        scope.launch(Dispatchers.IO) {
            try {
                val activeClient = client
                if (activeClient != null && _connectionState.value.isConnected) {
                    activeClient.writeIgnitionTable(table)
                }
                updateLocalIgnitionTable(table)
                _ignitionTable.value = table
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

        val wasStreaming = activeClient.isStreaming()
        if (wasStreaming) {
            activeClient.pauseLiveDataStream()
        }

        val result = configManager.downloadAllConfigs(activeClient) { current, total, message ->
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
            if (localDir == null && sessionDir != null) {
                localSessionDir = sessionDir
                loadTablesFromSession(sessionDir)
            } else if (localDir != null && sessionDir != null) {
                val ecuSignature = sessionSignature(sessionDir)
                val localSignature = sessionSignature(localDir)
                if (ecuSignature != localSignature) {
                    _syncPrompt.value = SyncPrompt(localDir, sessionDir)
                } else {
                    localSessionDir = sessionDir
                    loadTablesFromSession(sessionDir)
                }
            }
        } else {
            _configState.value = _configState.value.copy(
                isBusy = false,
                message = "Erro: ${result.error}"
            )
        }

        if (autoRestartStream && wasStreaming && _connectionState.value.isConnected) {
            activeClient.startLiveDataStream(_streamIntervalMs.value)
        }

        return@withContext result.success
    }

    private suspend fun loadTablesFromSession(sessionDir: File) {
        _veTable.value = configManager.loadVeTable(sessionDir)
        _ignitionTable.value = configManager.loadIgnitionTable(sessionDir)
        _afrTable.value = configManager.loadAfrTable(sessionDir)
        _engineConstants.value = configManager.loadEngineConstants(sessionDir)
        _triggerSettings.value = configManager.loadTriggerSettings(sessionDir)
    }

    private suspend fun sessionSignature(sessionDir: File): Map<Byte, Long> {
        val pages = runCatching { configManager.loadConfig(sessionDir) }.getOrDefault(emptyMap())
        return pages.mapValues { (_, data) ->
            val crc = CRC32()
            crc.update(data)
            crc.value
        }
    }

    private suspend fun restoreConfigToEcu(
        sessionDir: File,
        skipPages: Set<Byte> = emptySet(),
        stopOnRangeErr: Boolean = false,
        applyTriggerFix: Boolean = false
    ): RestoreOutcome = withContext(Dispatchers.IO) {
        val activeClient = client ?: throw IllegalStateException("ECU não conectada")
        val wasStreaming = activeClient.isStreaming()
        if (wasStreaming) {
            activeClient.pauseLiveDataStream()
        }

        val pages = configManager.loadConfig(sessionDir)
        if (pages.isEmpty()) {
            throw IllegalStateException("Backup sem páginas válidas")
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val inconsistentPages = mutableListOf<Byte>()
        var wroteAnyPage = false
        val restoreSkipPages = skipPages + 0.toByte()
        var stopDueToRangeErr = false

        pageLoop@ for ((pageNum, pageSize) in ConfigManager.PAGE_SIZES) {
            if (restoreSkipPages.contains(pageNum)) {
                val reason = when (pageNum.toInt()) {
                    0 -> "read-only/status"
                    else -> "compatibilidade"
                }
                warnings.add("Página $pageNum ignorada no restore ($reason)")
                continue
            }
            val data = pages[pageNum]
            if (data == null) {
                warnings.add("Página $pageNum ausente no backup")
                continue
            }
            if (data.size != pageSize) {
                errors.add("Página $pageNum com tamanho inesperado (${data.size} != $pageSize)")
                continue
            }

            var attempt = 0
            var success = false
            while (attempt < 3 && !success) {
                attempt++
                try {
                    activeClient.writeRawPageWithoutBurn(pageNum, data)
                    wroteAnyPage = true
                    success = true
                } catch (e: Exception) {
                    val errorText = e.message ?: "Erro desconhecido"
                    if (errorText.contains("RANGE_ERR")) {
                        if (pageNum.toInt() == 6) {
                            val sanitized = Page6Validator.sanitize(data)
                            if (sanitized.changed) {
                                try {
                                    activeClient.writeRawPageWithoutBurn(pageNum, sanitized.data)
                                    wroteAnyPage = true
                                    warnings.add("Página 6 corrigida (sanitização)")
                                    success = true
                                    break
                                } catch (sanitizeError: Exception) {
                                    warnings.add("Falha ao corrigir página 6: ${sanitizeError.message}")
                                }
                            }
                        }

                        warnings.add("Página $pageNum rejeitada (RANGE_ERR)")
                        if (stopOnRangeErr) {
                            inconsistentPages.add(pageNum)
                            stopDueToRangeErr = true
                            break
                        }
                        success = true
                        break
                    }
                    val message = "Falha ao gravar página $pageNum (tentativa $attempt): $errorText"
                    if (attempt >= 3) {
                        errors.add(message)
                    } else {
                        delay(400)
                    }
                }
            }
            if (stopDueToRangeErr) {
                break@pageLoop
            }
            delay(150)
        }

        if (!stopDueToRangeErr && applyTriggerFix) {
            val triggerPage = pages[TriggerSettings.PAGE_NUMBER.toByte()]
            if (triggerPage != null) {
                try {
                    val triggerSettings = TriggerSettings.fromPageData(triggerPage)
                    activeClient.writeTriggerSettings(triggerSettings, burn = false)
                    wroteAnyPage = true
                    warnings.add("Página ${TriggerSettings.PAGE_NUMBER} corrigida (Trigger Settings)")
                } catch (e: Exception) {
                    warnings.add("Falha ao corrigir página ${TriggerSettings.PAGE_NUMBER}: ${e.message}")
                }
            } else {
                warnings.add("Página ${TriggerSettings.PAGE_NUMBER} ausente no backup")
            }
        }

        if (!stopDueToRangeErr && wroteAnyPage) {
            try {
                activeClient.burnConfigs()
            } catch (e: Exception) {
                errors.add("Falha ao executar burn: ${e.message}")
            }
        }

        if (errors.isNotEmpty()) {
            throw IllegalStateException(errors.joinToString(" | "))
        }

        if (wasStreaming && _connectionState.value.isConnected) {
            activeClient.startLiveDataStream(_streamIntervalMs.value)
        }

        return@withContext RestoreOutcome(
            warnings = warnings,
            inconsistentPages = inconsistentPages,
            completed = !stopDueToRangeErr
        )
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

    private fun updateLocalVeTable(table: VeTable) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, "page_2.bin")
        val format = resolveTableFormat(pageFile, VeTable.StorageFormat.MODERN_288, VeTable.StorageFormat.LEGACY_304)
        pageFile.writeBytes(table.toByteArray(format))
    }

    private fun updateLocalIgnitionTable(table: IgnitionTable) {
        val sessionDir = ensureLocalSessionDir() ?: return
        val pageFile = File(sessionDir, "page_3.bin")
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
                                "afr" -> entry.afrMeasuredDeci?.let { (it / 10.0).toString() } ?: ""
                                "afr_target" -> entry.afrTargetDeci?.let { (it / 10.0).toString() } ?: ""
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
}

private data class NavSection(
    val titleKey: String,
    val routes: List<DesktopRoute>
)

private fun chooseSaveFile(title: String, defaultName: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.file = defaultName
    dialog.isVisible = true
    val fileName = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, fileName)
}

private fun chooseOpenFile(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val fileName = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, fileName)
}

private fun navSections(): List<NavSection> {
    return listOf(
        NavSection(
            titleKey = "nav.sectionApp",
            routes = listOf(
                DesktopRoute.Settings
            )
        ),
        NavSection(
            titleKey = "nav.sectionDashboard",
            routes = listOf(
                DesktopRoute.Dashboard
            )
        ),
        NavSection(
            titleKey = "nav.sectionMaps",
            routes = listOf(
                DesktopRoute.VeTable,
                DesktopRoute.IgnitionTable,
                DesktopRoute.AfrTable,
                DesktopRoute.BaseMapWizard
            )
        ),
        NavSection(
            titleKey = "nav.sectionConfigs",
            routes = listOf(
                DesktopRoute.EngineConstants,
                DesktopRoute.TriggerSettings,
                DesktopRoute.SensorsConfig,
                DesktopRoute.EngineProtection
            )
        ),
        NavSection(
            titleKey = "nav.sectionLogs",
            routes = listOf(
                DesktopRoute.Connection,
                DesktopRoute.LogViewer,
                DesktopRoute.RealTimeMonitor,
                DesktopRoute.LogAnalyzer,
                DesktopRoute.BeforeAfter
            )
        )
    )
}

private enum class DesktopRoute(val labelKey: String, val titleKey: String) {
    Settings("app.settingsLabel", "app.settingsTitle"),
    Dashboard("route.dashboard", "route.dashboard"),
    Connection("route.connection", "route.connection"),
    VeTable("route.veTable", "route.veTable"),
    IgnitionTable("route.ignitionTable", "route.ignitionTable"),
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
