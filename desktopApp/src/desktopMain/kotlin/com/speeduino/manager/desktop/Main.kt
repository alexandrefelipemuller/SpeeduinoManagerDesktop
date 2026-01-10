package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.speeduino.manager.FirmwareInfo
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.SpeeduinoClient
import com.speeduino.manager.connection.SpeeduinoTcpConnection
import com.speeduino.manager.connection.SpeeduinoSerialConnection
import com.speeduino.manager.connection.ISpeeduinoConnection
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
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.basemap.BaseMapAdjustments
import com.speeduino.manager.model.basemap.BaseMapGenerator
import com.speeduino.manager.model.basemap.EngineProfile
import com.speeduino.manager.model.basemap.FuelType
import com.speeduino.manager.model.basemap.GeneratedBaseMap
import com.speeduino.manager.model.logging.LiveLogRecorder
import com.speeduino.manager.model.logging.LiveLogSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    Window(
        onCloseRequest = ::exitApplication,
        title = "SpeeduinoManager Desktop",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        SpeeduinoDesktopTheme {
            DesktopApp()
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
                            title = currentRoute.title,
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
                            }
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
}

@Composable
private fun HeaderBar(
    title: String,
    connectionState: ConnectionState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Speeduino Manager Desktop",
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
            isConnected = connectionState.isConnected,
            message = connectionState.message
        )
    }
}

@Composable
private fun NavigationSidebar(
    currentRoute: DesktopRoute,
    onRouteSelected: (DesktopRoute) -> Unit
) {
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
                        text = "Speeduino",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Manager Desktop",
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
                            text = section.title.uppercase(),
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
                                            text = route.label,
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
    onToggleConnection: () -> Unit
) {
    when (route) {
        DesktopRoute.Dashboard -> DashboardScreen(liveData)
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
            isConnected = connectionState.isConnected,
            statusMessage = connectionState.message,
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
                    text = "Diagnostico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                InfoRow("Firmware", firmwareInfo?.signature ?: "--")
                InfoRow("Produto", productString ?: "--")
                InfoRow("Conexao", connectionInfo ?: "--")
                InfoRow("Versao do app", appVersion)
                if (!lastError.isNullOrBlank()) {
                    Text(
                        text = "Erro: $lastError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9A3B2E)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(liveData: SpeeduinoLiveData?) {
    val gauges = remember(liveData) {
        listOf(
            GaugeSpec(
                label = "RPM",
                value = (liveData?.rpm ?: 0).toFloat(),
                min = 0f,
                max = 7000f,
                unit = "rpm"
            ),
            GaugeSpec(
                label = "MAP",
                value = (liveData?.mapPressure ?: 0).toFloat(),
                min = 0f,
                max = 250f,
                unit = "kPa"
            ),
            GaugeSpec(
                label = "TPS",
                value = (liveData?.tps ?: 0).toFloat(),
                min = 0f,
                max = 100f,
                unit = "%"
            ),
            GaugeSpec(
                label = "Coolant",
                value = (liveData?.coolantTemp ?: 0).toFloat(),
                min = -20f,
                max = 120f,
                unit = "C"
            )
        )
    }

    val stats = remember(liveData) {
        listOf(
            StatItem("Bateria", liveData?.batteryVoltage?.let { String.format("%.1f V", it) } ?: "--"),
            StatItem("Ignicao", liveData?.advance?.let { "$it deg" } ?: "--")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GaugeGrid(gauges)
        StatRow(stats)
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
    MapTableScreen(
        title = "VE Table",
        description = "Mapa de combustivel (VE).",
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
    MapTableScreen(
        title = "Ignition Table",
        description = "Mapa de ignicao (graus BTDC).",
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
    MapTableScreen(
        title = "AFR Table",
        description = "Mapa de metas AFR (lambda).",
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
    var workingTable by remember(table) { mutableStateOf(table) }
    var hasChanges by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TableEditTarget?>(null) }
    var editValue by remember { mutableStateOf("") }

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
                    FilledTonalButton(onClick = onLoad) { Text("Carregar ECU") }
                    FilledTonalButton(
                        onClick = { workingTable?.let(onSave); hasChanges = false },
                        enabled = workingTable != null && hasChanges
                    ) { Text("Enviar ECU") }
                }
            }
        }

        if (workingTable == null) {
            PlaceholderScreen(title, "Nenhum dado carregado.")
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
                            HeaderCell("Load/RPM")
                            rpm.forEachIndexed { index, value ->
                                HeaderCell(value.toString()) {
                                    editTarget = TableEditTarget.Rpm(index)
                                    editValue = value.toString()
                                }
                            }
                        }
                        load.forEachIndexed { rowIndex, loadValue ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HeaderCell(loadValue.toString()) {
                                    editTarget = TableEditTarget.Load(rowIndex)
                                    editValue = loadValue.toString()
                                }
                            grid.getOrNull(rowIndex)?.forEachIndexed { colIndex, cell ->
                                ValueCell(
                                    value = formatValue(cell),
                                    background = cellColor(cell),
                                    onClick = {
                                        editTarget = TableEditTarget.Cell(rowIndex, colIndex)
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
            title = { Text("Editar valor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Faixa recomendada: ${valueRange.first}..${valueRange.last}")
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
                ) { Text("Aplicar") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { editTarget = null }) { Text("Cancelar") }
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
                Text("Constantes do Motor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Parametros globais do motor e injecao.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = controller::loadEngineConstants) { Text("Carregar ECU") }
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
                    ) { Text("Salvar ECU") }
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
                Text("Combustivel e referencia", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Req Fuel (ms)", reqFuel, {
                        reqFuel = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Voltagem", batteryVoltage, {
                        batteryVoltage = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }

                HorizontalDivider()

                Text("Algoritmo e injecao", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("Algoritmo", algorithm.displayName, Algorithm.values().map { it.displayName }) { label ->
                        algorithm = Algorithm.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    DropdownField("Staging", injectorStaging.displayName, InjectorStaging.values().map { it.displayName }) { label ->
                        injectorStaging = InjectorStaging.values().first { it.displayName == label }
                        hasChanges = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("Stroke", engineStroke.displayName, EngineStroke.values().map { it.displayName }) { label ->
                        engineStroke = EngineStroke.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    DropdownField("Porta", injectorPortType.displayName, InjectorPortType.values().map { it.displayName }) { label ->
                        injectorPortType = InjectorPortType.values().first { it.displayName == label }
                        hasChanges = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("Tipo Motor", engineType.displayName, EngineType.values().map { it.displayName }) { label ->
                        engineType = EngineType.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    DropdownField("Layout", injectorLayout.displayName, InjectorLayout.values().map { it.displayName }) { label ->
                        injectorLayout = InjectorLayout.values().first { it.displayName == label }
                        hasChanges = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Squirts/Ciclo", squirtsPerCycle, {
                        squirtsPerCycle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Cilindros", numberOfCylinders, {
                        numberOfCylinders = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Injetores", numberOfInjectors, {
                        numberOfInjectors = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }

                HorizontalDivider()

                Text("MAP e estequiometria", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("MAP Sample", mapSampleMethod.displayName, MapSampleMethod.values().map { it.displayName }) { label ->
                        mapSampleMethod = MapSampleMethod.values().first { it.displayName == label }
                        hasChanges = true
                    }
                    NumberField("Stoich AFR", stoich, {
                        stoich = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("MAP Switch", mapSwitchPoint, {
                        mapSwitchPoint = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }

                HorizontalDivider()

                Text("Oddfire Angles", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Canal 2", channel2Angle, {
                        channel2Angle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Canal 3", channel3Angle, {
                        channel3Angle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Canal 4", channel4Angle, {
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
                Text("Gatilho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Configuracao de sensor de rotacao.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = controller::loadTriggerSettings) { Text("Carregar ECU") }
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
                    ) { Text("Salvar ECU") }
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
                    NumberField("Trigger Angle", triggerAngle, {
                        triggerAngle = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Angle Multiplier", triggerMultiplier, {
                        triggerMultiplier = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }
                DropdownField(
                    "Trigger Pattern",
                    triggerPatternLabel(triggerPattern),
                    triggerPatternOptions()
                ) { label ->
                    triggerPattern = triggerPatternFromLabel(label)
                    hasChanges = true
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Base Teeth", baseTeeth, {
                        baseTeeth = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    NumberField("Missing Teeth", missingTeeth, {
                        missingTeeth = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                }
                DropdownField(
                    "Primary Speed",
                    primarySpeed.name,
                    TriggerSettings.TriggerSpeed.values().map { it.name }
                ) { label ->
                    primarySpeed = TriggerSettings.TriggerSpeed.valueOf(label)
                    hasChanges = true
                }
                DropdownField(
                    "Trigger Edge",
                    triggerEdge.name,
                    TriggerSettings.SignalEdge.values().map { it.name }
                ) { label ->
                    triggerEdge = TriggerSettings.SignalEdge.valueOf(label)
                    hasChanges = true
                }
                DropdownField(
                    "Secondary Edge",
                    secondaryEdge.name,
                    TriggerSettings.SignalEdge.values().map { it.name }
                ) { label ->
                    secondaryEdge = TriggerSettings.SignalEdge.valueOf(label)
                    hasChanges = true
                }
                DropdownField(
                    "Secondary Pattern",
                    secondaryPatternLabel(secondaryType),
                    secondaryPatternOptions()
                ) { label ->
                    secondaryType = secondaryPatternFromLabel(label)
                    hasChanges = true
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Skip Revolutions", skipRevs, {
                        skipRevs = it
                        hasChanges = true
                    }, Modifier.weight(1f))
                    DropdownField(
                        "Trigger Filter",
                        filter.name,
                        TriggerSettings.TriggerFilter.values().map { it.name }
                    ) { label ->
                        filter = TriggerSettings.TriggerFilter.valueOf(label)
                        hasChanges = true
                    }
                }
                ToggleField("Primeira fase alta", phaseHigh) {
                    phaseHigh = it
                    hasChanges = true
                }
                ToggleField("Resync every cycle", reSyncEveryCycle) {
                    reSyncEveryCycle = it
                    hasChanges = true
                }
            }
        }
    }
}

@Composable
private fun SensorsConfigScreenDesktop() {
    PlaceholderScreen("Calibracao", "Calibracao de sensores ainda nao suportada no desktop.")
}

@Composable
private fun EngineProtectionScreenDesktop() {
    PlaceholderScreen("Protecao", "Protecao e limitadores ainda nao suportados no desktop.")
}

@Composable
private fun RealTimeMonitorScreenDesktop(
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?
) {
    val logState by controller.logState.collectAsState()
    val intervalMs by controller.streamIntervalMs.collectAsState()
    var selectedInterval by remember(intervalMs) { mutableStateOf(intervalMs.toString()) }
    var fileName by remember { mutableStateOf("speeduino_log") }

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
                Text("Monitor em Tempo Real", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Captura e monitoramento de live data.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Intervalo (ms)", selectedInterval, { value ->
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
                        Text(if (logState.isRecording) "Parar captura" else "Iniciar captura")
                    }
                }
                Text(
                    text = "Capturas: ${logState.samplesCaptured}",
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
                Text("Status Atual", style = MaterialTheme.typography.titleMedium)
                InfoRow("RPM", liveData?.rpm?.toString() ?: "--")
                InfoRow("MAP", liveData?.mapPressure?.toString() ?: "--")
                InfoRow("TPS", liveData?.tps?.toString() ?: "--")
                InfoRow("Coolant", liveData?.coolantTemp?.toString() ?: "--")
                InfoRow("IAT", liveData?.intakeTemp?.toString() ?: "--")
                InfoRow("Bateria", liveData?.batteryVoltage?.let { String.format("%.1f", it) } ?: "--")
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
                Text("Exportar log", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Nome do arquivo") },
                    singleLine = true,
                    modifier = Modifier.width(280.dp)
                )
                FilledTonalButton(onClick = { controller.saveLogSnapshot(fileName) }) {
                    Text("Salvar CSV")
                }
            }
        }
    }
}

@Composable
private fun LogViewerScreenDesktop(controller: DesktopSpeeduinoController) {
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
                Text("Visualizador de Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Snapshot atual da captura em memoria.", style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = controller::captureSnapshot) { Text("Atualizar snapshot") }
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
                    Text("Nenhum log capturado.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val logScroll = rememberScrollState()
                    Box(modifier = Modifier.heightIn(max = 420.dp)) {
                        Column(
                            modifier = Modifier.verticalScroll(logScroll),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            entries.take(200).forEach { entry ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("RPM ${entry.rpm}")
                                        Text("MAP ${entry.mapKpa}")
                                        Text("TPS ${entry.tps}")
                                        Text("CLT ${entry.coolantTempC}")
                                    }
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(logScroll),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                    Text(
                        text = "Mostrando ${entries.take(200).size} de ${entries.size} amostras",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BaseMapWizardScreenDesktop(controller: DesktopSpeeduinoController) {
    val engineConstants by controller.engineConstants.collectAsState()
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
                Text("Base Map Wizard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Gera mapas base a partir do perfil do motor.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Cilindros", cylinders, { cylinders = it }, Modifier.width(140.dp))
                    NumberField("Cilindrada cc", displacement, { displacement = it }, Modifier.width(160.dp))
                    NumberField("RPM Max", maxRpm, { maxRpm = it }, Modifier.width(140.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Compressao", compression, { compression = it }, Modifier.width(140.dp))
                    NumberField("Injector lbs/hr", injectorFlow, { injectorFlow = it }, Modifier.width(160.dp))
                    NumberField("MAP Max kPa", mapMax, { mapMax = it }, Modifier.width(140.dp))
                }
                DropdownField(
                    "Combustivel",
                    fuelType.name,
                    FuelType.values().map { it.name }
                ) { label ->
                    fuelType = FuelType.valueOf(label)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Richness", richness, { richness = it }, Modifier.width(140.dp))
                    NumberField("Advance Offset", advanceOffset, { advanceOffset = it }, Modifier.width(160.dp))
                    NumberField("High Load Agg.", aggressiveness, { aggressiveness = it }, Modifier.width(160.dp))
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
                ) { Text("Gerar mapas") }
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
                    Text("Resultado", style = MaterialTheme.typography.titleMedium)
                    Text("ReqFuel: ${String.format("%.2f", map.engineConstants.reqFuel)} ms")
                    Text("Stoich: ${String.format("%.1f", map.engineConstants.stoichiometricRatio)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { controller.applyGeneratedBaseMap(map) }) { Text("Aplicar tudo") }
                        FilledTonalButton(onClick = { controller.applyGeneratedBaseMap(map, writeConstants = false) }) { Text("Aplicar mapas") }
                        FilledTonalButton(onClick = { controller.applyGeneratedBaseMap(map, writeTables = false) }) { Text("Aplicar constantes") }
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
                        contentDescription = "Abrir"
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
                val items = if (options.isEmpty()) listOf("Nenhuma opcao") else options
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

private fun triggerPatternOptions(): List<String> {
    return listOf(
        "Missing Tooth",
        "Basic Distributor",
        "Dual Wheel",
        "GM 7X",
        "4G63 / Miata / 3000GT",
        "GM 24X",
        "Jeep 2000",
        "Audi 135",
        "Honda D17",
        "Miata 99-05",
        "Mazda AU",
        "Non-360 Dual",
        "Nissan 360",
        "Subaru 6/7",
        "Daihatsu +1",
        "Harley EVO",
        "36-2-2-2",
        "36-2-1",
        "DSM 420a",
        "Weber-Marelli",
        "Ford ST170",
        "DRZ400",
        "Chrysler NGC",
        "Yamaha Vmax 1990+",
        "Renix",
        "Rover MEMS",
        "K6A",
        "Pattern 27",
        "Pattern 28",
        "Pattern 29",
        "Pattern 30",
        "Pattern 31"
    )
}

private fun triggerPatternLabel(value: Int): String {
    return triggerPatternOptions().getOrNull(value) ?: "Pattern $value"
}

private fun triggerPatternFromLabel(label: String): Int {
    val index = triggerPatternOptions().indexOf(label)
    return if (index >= 0) index else label.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private fun secondaryPatternOptions(): List<String> {
    return listOf(
        "Single tooth cam",
        "4-1 cam",
        "Poll level",
        "Rover 5-3-2 cam",
        "Toyota 3 Tooth"
    )
}

private fun secondaryPatternLabel(value: Int): String {
    return secondaryPatternOptions().getOrNull(value) ?: "Tipo $value"
}

private fun secondaryPatternFromLabel(label: String): Int {
    val index = secondaryPatternOptions().indexOf(label)
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
    isConnected: Boolean,
    statusMessage: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectionTypeChange: (ConnectionType) -> Unit,
    onSerialPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit,
    onToggleConnection: () -> Unit
) {
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
                text = "Conexao",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            DropdownField(
                label = "Tipo de conexao",
                value = connectionType.label,
                options = ConnectionType.values().map { it.label }
            ) { label ->
                onConnectionTypeChange(ConnectionType.values().first { it.label == label })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (connectionType) {
                    ConnectionType.TCP -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            label = { Text("Host") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = onPortChange,
                            label = { Text("Porta") },
                            singleLine = true,
                            modifier = Modifier.width(140.dp),
                            isError = port.isNotEmpty() && !portIsValid,
                            supportingText = {
                                if (port.isNotEmpty() && !portIsValid) {
                                    Text("Porta invalida")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    ConnectionType.USB,
                    ConnectionType.BLUETOOTH -> {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DropdownField(
                                label = "Porta serial",
                                value = serialPorts.firstOrNull { it.systemPortName == serialPort }?.displayName
                                    ?: if (serialPort.isBlank()) "Selecione" else serialPort,
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
                            label = { Text("Baud") },
                            singleLine = true,
                            modifier = Modifier.width(140.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                FilledTonalButton(
                    onClick = onToggleConnection,
                    enabled = isConnected || when (connectionType) {
                        ConnectionType.TCP -> portIsValid
                        ConnectionType.USB,
                        ConnectionType.BLUETOOTH -> serialPort.isNotBlank()
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(if (isConnected) "Desconectar" else "Conectar")
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(isConnected = isConnected, message = statusMessage)
            }
        }
    }
}

@Composable
private fun StatusPill(isConnected: Boolean, message: String) {
    val background = if (isConnected) Color(0xFFE0F2E9) else Color(0xFFFDE9E4)
    val content = if (isConnected) Color(0xFF1F5F3D) else Color(0xFF7A3626)

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
private fun GaugeGrid(gauges: List<GaugeSpec>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 900.dp) 2 else 4
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) {
            items(gauges) { gauge ->
                GaugeCard(gauge)
            }
        }
    }
}

@Composable
private fun GaugeCard(spec: GaugeSpec) {
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
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
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
                        text = spec.displayValue(),
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
    val label: String,
    val value: Float,
    val min: Float,
    val max: Float,
    val unit: String
) {
    fun displayValue(): String {
        return if (value.isNaN()) "--" else value.toInt().toString()
    }
}

private data class StatItem(
    val label: String,
    val value: String
)


private data class ConnectionState(
    val isConnected: Boolean = false,
    val message: String = "Desconectado"
)

private enum class ConnectionType(val label: String) {
    TCP("Wi-Fi/TCP"),
    USB("USB Serial"),
    BLUETOOTH("Bluetooth (Serial)")
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

    private var pollingJob: Job? = null
    private var connection: ISpeeduinoConnection? = null
    private var client: SpeeduinoClient? = null

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
                    ConnectionState(true, "Conectado")
                } else {
                    ConnectionState(false, "Desconectado")
                }
            },
            onError = { error ->
                _lastError.value = error
            }
        )
        _connectionState.value = ConnectionState(false, "Conectando...")

        pollingJob = scope.launch(Dispatchers.IO) {
            try {
                client?.connect()
                _firmwareInfo.value = client?.getFirmwareInfoCached()
                _productString.value = client?.getProductString()
                _connectionInfo.value = client?.getConnectionInfo()
                client?.startLiveDataStream(_streamIntervalMs.value)
            } catch (e: Exception) {
                _lastError.value = e.message
                _connectionState.value = ConnectionState(false, "Falha ao conectar: ${e.message}")
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
        _engineConstants.value = null
        _triggerSettings.value = null
        _veTable.value = null
        _ignitionTable.value = null
        _afrTable.value = null
        if (_connectionState.value.isConnected) {
            _connectionState.value = ConnectionState(false, "Desconectado")
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
                _engineConstants.value = client?.readEngineConstants()
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveEngineConstants(constants: EngineConstants) {
        scope.launch(Dispatchers.IO) {
            try {
                client?.writeEngineConstants(constants)
                _engineConstants.value = constants
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadTriggerSettings() {
        scope.launch(Dispatchers.IO) {
            try {
                _triggerSettings.value = client?.readTriggerSettings()
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveTriggerSettings(settings: TriggerSettings) {
        scope.launch(Dispatchers.IO) {
            try {
                client?.writeTriggerSettings(settings)
                _triggerSettings.value = settings
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadVeTable() {
        scope.launch(Dispatchers.IO) {
            try {
                _veTable.value = client?.readVeTable()
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveVeTable(table: VeTable) {
        scope.launch(Dispatchers.IO) {
            try {
                client?.writeVeTable(table)
                _veTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadIgnitionTable() {
        scope.launch(Dispatchers.IO) {
            try {
                _ignitionTable.value = client?.readIgnitionTable()
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveIgnitionTable(table: IgnitionTable) {
        scope.launch(Dispatchers.IO) {
            try {
                client?.writeIgnitionTable(table)
                _ignitionTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun loadAfrTable() {
        scope.launch(Dispatchers.IO) {
            try {
                _afrTable.value = client?.readAfrTable()
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
    }

    fun saveAfrTable(table: AfrTable) {
        scope.launch(Dispatchers.IO) {
            try {
                client?.writeAfrTable(table)
                _afrTable.value = table
            } catch (e: Exception) {
                _lastError.value = e.message
            }
        }
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

    fun saveLogSnapshot(fileName: String) {
        val snapshot = logRecorder.snapshot() ?: return
        val sanitizedName = fileName.ifBlank { "speeduino_log" }
        val targetDir = java.nio.file.Paths.get(
            System.getProperty("user.home"),
            "SpeeduinoManagerDesktop",
            "logs"
        )
        java.nio.file.Files.createDirectories(targetDir)
        val targetFile = targetDir.resolve("$sanitizedName.csv").toFile()

        targetFile.bufferedWriter().use { writer ->
            writer.appendLine("timestamp_ms,rpm,map_kpa,tps,coolant_c,iat_c,battery_v,advance_deg,o2")
            snapshot.entries.forEach { entry ->
                writer.appendLine(
                    listOf(
                        entry.timestampMs,
                        entry.rpm,
                        entry.mapKpa,
                        entry.tps,
                        entry.coolantTempC,
                        entry.intakeTempC,
                        entry.batteryDeciVolt / 10.0,
                        entry.advanceDeg,
                        entry.o2
                    ).joinToString(",")
                )
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
    val title: String,
    val routes: List<DesktopRoute>
)

private fun navSections(): List<NavSection> {
    return listOf(
        NavSection(
            title = "Dashboard",
            routes = listOf(
                DesktopRoute.Dashboard
            )
        ),
        NavSection(
            title = "Mapas & Tabelas",
            routes = listOf(
                DesktopRoute.VeTable,
                DesktopRoute.IgnitionTable,
                DesktopRoute.AfrTable,
                DesktopRoute.BaseMapWizard
            )
        ),
        NavSection(
            title = "Configs",
            routes = listOf(
                DesktopRoute.EngineConstants,
                DesktopRoute.TriggerSettings,
                DesktopRoute.SensorsConfig,
                DesktopRoute.EngineProtection
            )
        ),
        NavSection(
            title = "Logs",
            routes = listOf(
                DesktopRoute.Connection,
                DesktopRoute.LogViewer,
                DesktopRoute.RealTimeMonitor
            )
        )
    )
}

private enum class DesktopRoute(val label: String, val title: String) {
    Dashboard("Dashboard", "Dashboard"),
    Connection("Conexao", "Conexao"),
    VeTable("VE Table", "VE Table"),
    IgnitionTable("Ignition Table", "Ignition Table"),
    AfrTable("AFR Table", "AFR Table"),
    BaseMapWizard("Base Map Wizard", "Base Map Wizard"),
    EngineConstants("Constantes do Motor", "Constantes do Motor"),
    TriggerSettings("Gatilho", "Gatilho"),
    SensorsConfig("Calibracao", "Calibracao"),
    EngineProtection("Protecao", "Protecao"),
    RealTimeMonitor("Monitor em Tempo Real", "Monitor em Tempo Real"),
    LogViewer("Visualizador de Logs", "Visualizador de Logs")
}
