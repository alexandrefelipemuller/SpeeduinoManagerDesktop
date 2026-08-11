package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ecucore.model.RusefiInputOutputSnapshot
import io.ecucore.model.SecondarySerialConfig
import io.ecucore.model.SecondarySerialProtocol
import io.ecucore.tuning.CellRef
import com.speeduino.manager.desktop.LocalStrings
import io.ecucore.tuning.TuningStrategy
import com.speeduino.manager.desktop.ui.DropdownField
import com.speeduino.manager.desktop.ui.InfoRow
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.math.abs

@Composable
internal fun MapsTablesScreenDesktop(
    onOpenVeTable: () -> Unit,
    onOpenVeTable2: () -> Unit,
    onOpenAfrTable: () -> Unit,
    onOpenBaseMapWizard: () -> Unit,
    onOpenTuningAssistant: () -> Unit,
    onOpenInjectorConfig: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    TuningSectionScreen(
        title = strings["route.mapsTables"],
        subtitle = strings["label.mapsTablesSubtitle"],
    ) {
        SectionPanel(strings["label.fuelSection"]) {
            ActionCard(strings["label.injectorConfigTitle"], strings["label.injectorConfigSubtitle"]) { RowButtons(onOpenInjectorConfig, null, strings["action.open"], null) }
            ActionCard(strings["route.veTable"], strings["label.veTableDesc"]) { RowButtons(onOpenVeTable, onOpenVeTable2, strings["label.veTable1"], strings["label.veTable2"]) }
            ActionCard(strings["route.afrTable"], strings["label.afrTableDesc"]) { RowButtons(onOpenAfrTable, null, strings["action.open"], null) }
        }
        SectionPanel(strings["label.workflowSection"]) {
            ActionCard(strings["route.baseMapWizard"], strings["label.baseMapWizardDesc"]) { RowButtons(onOpenBaseMapWizard, null, strings["action.open"], null) }
            ActionCard(strings["label.tuningAssistantTitle"], strings["label.tuningAssistantSubtitle"]) { RowButtons(onOpenTuningAssistant, null, strings["action.open"], null) }
            ActionCard(strings["route.beforeAfter"], strings["label.beforeAfterSubtitle"]) { RowButtons(onOpenBeforeAfter, null, strings["action.open"], null) }
            ActionCard(strings["home.openBackupSettings"], strings["maps_tables_backup_action_desc"]) { RowButtons(onOpenSettings, null, strings["action.open"], null) }
        }
    }
}

@Composable
internal fun IgnitionScreenDesktop(
    onOpenIgnitionConfig: () -> Unit,
    onOpenIgnitionTable: () -> Unit,
    onOpenIgnitionTable2: () -> Unit,
    onOpenDwellTable: () -> Unit,
    onOpenTriggerSettings: () -> Unit
) {
    val strings = LocalStrings.current
    TuningSectionScreen(
        title = strings["route.ignition"],
        subtitle = strings["label.ignitionHubSubtitle"],
    ) {
        ActionCard(strings["label.ignitionConfigTitle"], strings["label.ignitionConfigSubtitle"]) { RowButtons(onOpenIgnitionConfig, null, strings["action.open"], null) }
        ActionCard(strings["route.ignitionTable"], strings["label.ignitionTableDesc"]) { RowButtons(onOpenIgnitionTable, onOpenIgnitionTable2, strings["label.ignitionTable1"], strings["label.ignitionTable2"]) }
        ActionCard(strings["route.dwellTable"], strings["label.dwellTableDesc"]) { RowButtons(onOpenDwellTable, null, strings["action.open"], null) }
        ActionCard(strings["route.triggerSettings"], strings["label.triggerSettingsDesc"]) { RowButtons(onOpenTriggerSettings, null, strings["action.open"], null) }
    }
}

@Composable
internal fun ConfigsTuningScreenDesktop(
    onOpenEngineConstants: () -> Unit,
    onOpenInputOutput: () -> Unit,
    onOpenSensorCalibration: () -> Unit,
    onOpenInjectorConfig: () -> Unit,
    onOpenSecondarySerial: () -> Unit,
    onOpenIgnitionConfig: () -> Unit,
) {
    val strings = LocalStrings.current
    TuningSectionScreen(
        title = strings["route.configsTuning"],
        subtitle = strings["label.configsTuningSubtitle"],
    ) {
        SectionPanel(strings["label.coreSetupSection"]) {
            ActionCard(strings["label.engineConstantsTitle"], strings["label.engineConstantsSubtitle"]) { RowButtons(onOpenEngineConstants, null, strings["action.open"], null) }
            ActionCard(strings["label.injectorConfigTitle"], strings["label.injectorConfigSubtitle"]) { RowButtons(onOpenInjectorConfig, null, strings["action.open"], null) }
            ActionCard(strings["label.ignitionConfigTitle"], strings["label.ignitionConfigSubtitle"]) { RowButtons(onOpenIgnitionConfig, null, strings["action.open"], null) }
        }
        SectionPanel(strings["label.hardwareSection"]) {
            ActionCard(strings["label.inputOutputTitle"], strings["label.inputOutputDesc"]) { RowButtons(onOpenInputOutput, null, strings["action.open"], null) }
            ActionCard(strings["route.sensorsConfig"], strings["label.sensorsCalibrationSubtitle"]) { RowButtons(onOpenSensorCalibration, null, strings["action.open"], null) }
            ActionCard(strings["label.secondarySerialTitle"], strings["label.secondarySerialSubtitle"]) { RowButtons(onOpenSecondarySerial, null, strings["action.open"], null) }
        }
    }
}

@Composable
internal fun EngineOperationScreenDesktop(
    onOpenIdleControl: () -> Unit,
    onOpenClosedLoopCorrections: () -> Unit,
    onOpenEngineProtection: () -> Unit,
    onOpenRevLimiter: () -> Unit,
) {
    val strings = LocalStrings.current
    TuningSectionScreen(
        title = strings["route.engineOperation"],
        subtitle = strings["label.engineOperationSubtitle"],
    ) {
        ActionCard(strings["route.idleControl"], strings["label.idleControlSubtitle"]) { RowButtons(onOpenIdleControl, null, strings["action.open"], null) }
        ActionCard(strings["route.closedLoopCorrections"], strings["label.closedLoopSubtitle"]) { RowButtons(onOpenClosedLoopCorrections, null, strings["action.open"], null) }
        ActionCard(strings["route.engineProtection"], strings["label.engineProtectionSubtitle"]) { RowButtons(onOpenEngineProtection, null, strings["action.open"], null) }
        ActionCard(strings["route.revLimiter"], strings["label.revLimiterSubtitle"]) { RowButtons(onOpenRevLimiter, null, strings["action.open"], null) }
    }
}

@Composable
internal fun InjectorConfigScreenDesktop(controller: DesktopSpeeduinoController) {
    com.speeduino.manager.desktop.feature.configs.InjectorConfigScreenDesktop(controller)
}

@Composable
internal fun RevLimiterConfigScreenDesktop() {
    com.speeduino.manager.desktop.feature.configs.RevLimiterConfigScreenDesktop()
}

@Composable
internal fun InputOutputConfigScreenDesktop(
    controller: DesktopSpeeduinoController,
    onOpenSecondarySerial: () -> Unit
) {
    val strings = LocalStrings.current
    val tuningState by controller.tuningConfigState.collectAsState()
    val snapshot = tuningState.rusefiSnapshot
    TuningSectionScreen(
        title = strings["label.inputOutputTitle"],
        subtitle = strings["label.ioLoadInstructions"],
    ) {
        ActionCard(strings["label.secondarySerialTitle"], strings["label.secondarySerialTitle"]) {
            RowButtons(onOpenSecondarySerial, null, strings["action.open"], null)
        }
        ActionCard(strings["label.rusefiSnapshotAction"], strings["label.ioLoadInstructions"]) {
            RowButtons({ controller.loadRusefiInputOutputSnapshot() }, null, strings["action.loadEcu"], null)
        }
        if (snapshot != null) {
            SnapshotGroup(strings["label.rusefiInputs"], snapshot.inputs)
            SnapshotGroup(strings["label.rusefiFuelOutputs"], snapshot.fuelOutputs)
            SnapshotGroup(strings["label.rusefiIgnitionOutputs"], snapshot.ignitionOutputs)
            SnapshotGroup(strings["label.rusefiAuxOutputs"], snapshot.auxiliaryOutputs)
        } else {
            PlaceholderScreen(strings["label.rusefiSnapshotAction"], strings["label.ioLoadInstructions"])
        }
    }
}

@Composable
internal fun SecondarySerialScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val tuningState by controller.tuningConfigState.collectAsState()
    val config = tuningState.secondarySerialConfig ?: SecondarySerialConfig(
        enabled = false,
        protocol = SecondarySerialProtocol.TUNERSTUDIO,
        protocolRaw = SecondarySerialProtocol.TUNERSTUDIO.rawValue
    )
    var enabled by remember(config) { mutableStateOf(config.enabled) }
    var protocol by remember(config) { mutableStateOf(config.protocol) }
    var protocolRaw by remember(config) { mutableStateOf(config.protocolRaw.toString()) }
    var hasChanges by remember { mutableStateOf(false) }

    TuningSectionScreen(
        title = strings["label.secondarySerialTitle"],
        subtitle = strings["label.secondarySerialTitle"],
    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings["label.enabled"], style = MaterialTheme.typography.bodyMedium)
                        Text(strings["label.secondarySerialHelp"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        hasChanges = true
                    })
                }
                DropdownField(
                    label = strings["label.protocol"],
                    value = protocol.displayName(),
                    options = SecondarySerialProtocol.values().filter { it != SecondarySerialProtocol.UNKNOWN }.map { it.displayName() }
                ) { value ->
                    protocol = SecondarySerialProtocol.values().first { it.displayName() == value }
                    protocolRaw = protocol.rawValue.toString()
                    hasChanges = true
                }
                OutlinedTextField(
                    value = protocolRaw,
                    onValueChange = {
                        protocolRaw = it.filter(Char::isDigit)
                        protocol = SecondarySerialProtocol.UNKNOWN
                        hasChanges = true
                    },
                    label = { Text(strings["label.secondarySerialRawProtocol"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = controller::loadSecondarySerialConfig) { Text(strings["action.reload"]) }
                    FilledTonalButton(
                        onClick = {
                            controller.saveSecondarySerialConfig(
                                SecondarySerialConfig(
                                    enabled = enabled,
                                    protocol = protocol,
                                    protocolRaw = protocolRaw.toIntOrNull() ?: protocol.rawValue
                                )
                            )
                            hasChanges = false
                        },
                        enabled = hasChanges
                    ) { Text(strings["action.saveEcu"]) }
                }
            }
        }
    }
}

@Composable
internal fun TuningAssistantScreenDesktop(
    controller: DesktopSpeeduinoController,
    onOpenBeforeAfter: () -> Unit
) {
    val strings = LocalStrings.current
    val logPath by controller.analyzerLogFile.collectAsState()
    val lastSavedLogPath by controller.lastSavedLogPath.collectAsState()
    val analyzerResult by controller.analyzerResult.collectAsState()
    val analyzerBusy by controller.analyzerBusy.collectAsState()
    val analyzerError by controller.analyzerError.collectAsState()
    val analyzerUndoTable by controller.analyzerUndoAvailable.collectAsState()
    val veTable by controller.veTable.collectAsState()
    val afrTable by controller.afrTable.collectAsState()
    val hasUndo = analyzerUndoTable != null

    var strategy by remember { mutableStateOf(TuningStrategy.CONSERVATIVE) }
    var showSavedLogsDialog by remember { mutableStateOf(false) }
    val savedLogFiles = remember(lastSavedLogPath) { collectSavedLogFiles(lastSavedLogPath) }
    var selectedClusterId by remember(analyzerResult) { mutableStateOf<String?>(null) }
    var selectedCell by remember(analyzerResult) { mutableStateOf<CellRef?>(null) }
    var includedClusterIds by remember(analyzerResult) {
        mutableStateOf(analyzerResult?.clusters?.map { it.id }?.toSet().orEmpty())
    }

    LaunchedEffect(logPath, veTable, afrTable, strategy) {
        if (!logPath.isNullOrBlank() && veTable != null && afrTable != null) {
            controller.analyzeLogFile(strategy)
        }
    }

    LaunchedEffect(analyzerResult) {
        includedClusterIds = analyzerResult?.clusters?.map { it.id }?.toSet().orEmpty()
        selectedClusterId = null
        selectedCell = null
    }

    LaunchedEffect(savedLogFiles, logPath) {
        if (logPath.isNullOrBlank()) {
            savedLogFiles.firstOrNull()?.let { controller.selectAnalyzerLogFile(it.absolutePath) }
        }
    }

    if (showSavedLogsDialog) {
        SavedLogsDialogDesktop(
            files = savedLogFiles,
            onDismiss = { showSavedLogsDialog = false },
            onSelect = { path ->
                showSavedLogsDialog = false
                controller.selectAnalyzerLogFile(path)
            }
        )
    }

    val currentResult = analyzerResult
    val ready = currentResult?.signalStatus?.isReady == true
    val highlightedCells = currentResult?.clusters
        ?.firstOrNull { it.id == selectedClusterId }
        ?.cells
        ?.toSet()
        ?: emptySet()
    val loadLabel = currentResult?.summary?.loadLabel
        ?: if (veTable?.loadType == io.ecucore.model.VeTable.LoadType.MAP) "kPa" else "%"

    TuningSectionScreen(
        title = strings["label.tuningAssistantTitle"],
        subtitle = strings["label.tuningAssistantSubtitle"],
    ) {
        ActionCard(
            title = strings["label.tuningAssistantTitle"],
            description = strings["label.tuningAssistantSubtitle"]
        ) {
            val currentFile = logPath?.let { File(it).name } ?: strings["label.tuningAssistantNoLogSelected"]
            InfoRow(strings["label.log"], currentFile)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    chooseOpenFile(strings["label.tuningAssistantOpenLog"])?.let { file ->
                        controller.selectAnalyzerLogFile(file.absolutePath)
                    }
                }) {
                    Text(strings["label.tuningAssistantOpenLog"])
                }
                OutlinedButton(onClick = { showSavedLogsDialog = true }, enabled = savedLogFiles.isNotEmpty()) {
                    Text(strings["label.logViewerSavedLogsTitle"])
                }
                FilledTonalButton(
                    onClick = { controller.analyzeLogFile(strategy) },
                    enabled = !analyzerBusy && !logPath.isNullOrBlank()
                ) {
                    Text(strings["label.tuningAssistantAnalyze"])
                }
                if (analyzerBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings["label.tuningAssistantBusy"])
                    }
                }
            }

            analyzerError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            currentResult?.summary?.let { summary ->
                HorizontalDivider()
                InfoRow(strings["label.samples"], "${summary.usedSamples}/${summary.totalSamples}")
                InfoRow(strings["label.duration"], "%.1fs".format(summary.durationSeconds))
                InfoRow(strings["label.rpmRange"], summary.rpmRange?.let { "${it.first} - ${it.last}" } ?: "--")
                InfoRow(strings["label.loadRange"], summary.loadRange?.let { "${it.first} - ${it.last} $loadLabel" } ?: "--")
            }
        }

        if (currentResult == null) {
            PlaceholderScreen(
                strings["label.tuningAssistantTitle"],
                strings["label.tuningAssistantNoResult"]
            )
            return@TuningSectionScreen
        }

        if (!ready) {
            PlaceholderScreen(
                strings["label.tuningAssistantSignalsTitle"],
                strings["label.tuningAssistantMissingSignals"]
            )
            SignalReadinessCard(currentResult.signalStatus, afrTable != null)
            return@TuningSectionScreen
        }

        SignalReadinessCard(currentResult.signalStatus, afrTable != null)
        HeatmapCard(
            result = currentResult,
            highlightedCells = highlightedCells,
            selectedCell = selectedCell,
            onCellSelected = { selectedCell = it }
        )
        ClusterSuggestionsCard(
            clusters = currentResult.clusters,
            includedClusterIds = includedClusterIds,
            onToggleInclude = { clusterId ->
                includedClusterIds = if (includedClusterIds.contains(clusterId)) {
                    includedClusterIds - clusterId
                } else {
                    includedClusterIds + clusterId
                }
            },
            onPreview = { clusterId ->
                selectedClusterId = if (selectedClusterId == clusterId) null else clusterId
            }
        )
        StrategyCard(
            strategy = strategy,
            onChange = { strategy = it }
        )
        ActionsCard(
            hasUndo = hasUndo,
            isBusy = analyzerBusy,
            onApply = { controller.applyAnalyzerToVe(strategy, includedClusterIds) },
            onUndo = { controller.undoLastAnalyzerApply() },
            onReload = { controller.reloadAnalyzerVeTable() },
            onCompare = onOpenBeforeAfter
        )
    }
}

@Composable
private fun TuningSectionScreen(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
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
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun SectionPanel(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            content()
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun RowButtons(
    primary: () -> Unit,
    secondary: (() -> Unit)?,
    primaryLabel: String,
    secondaryLabel: String?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = primary) { Text(primaryLabel) }
        if (secondary != null && secondaryLabel != null) {
            FilledTonalButton(onClick = secondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
private fun SnapshotGroup(title: String, items: List<io.ecucore.model.RusefiIoEntry>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            items.forEach { item ->
                InfoRow(item.label, item.value)
            }
        }
    }
}

private fun SecondarySerialProtocol.displayName(): String {
    return when (this) {
        SecondarySerialProtocol.TUNERSTUDIO -> "TunerStudio"
        SecondarySerialProtocol.REALDASH -> "RealDash"
        SecondarySerialProtocol.MSDROID -> "MSDroid"
        SecondarySerialProtocol.CAN -> "CAN"
        SecondarySerialProtocol.GENERIC_FIXED -> "Generic (Fixed List)"
        SecondarySerialProtocol.GENERIC_INI -> "Generic (ini File)"
        SecondarySerialProtocol.UNKNOWN -> "Unknown"
    }
}

@Composable
private fun SignalReadinessCard(signalStatus: io.ecucore.tuning.AnalyzerSignalStatus, afrAvailable: Boolean) {
    val strings = LocalStrings.current
    val rows = listOf(
        "RPM" to signalStatus.hasRpm,
        "Load" to signalStatus.hasLoad,
        "AFR" to signalStatus.hasAfr,
        "AFR Target" to (signalStatus.hasAfrTarget && afrAvailable)
    )
    ActionCard(
        title = strings["label.tuningAssistantSignalsTitle"],
        description = strings["label.tuningAssistantSignalsDesc"]
    ) {
        rows.forEach { (label, ok) ->
            InfoRow(label, if (ok) "OK" else "Missing")
        }
    }
}

@Composable
private fun HeatmapCard(
    result: io.ecucore.tuning.AnalyzerResult,
    highlightedCells: Set<CellRef>,
    selectedCell: CellRef?,
    onCellSelected: (CellRef) -> Unit
) {
    val strings = LocalStrings.current
    val maxChangePct = when {
        result.clusters.isNotEmpty() -> result.clusters.maxOfOrNull { abs(it.avgDeltaPct) }?.coerceAtLeast(0.01) ?: 0.08
        else -> 0.08
    }
    ActionCard(
        title = strings["label.tuningAssistantHeatmapTitle"],
        description = strings["label.tuningAssistantHeatmapDesc"]
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            result.cellSuggestions.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEachIndexed { colIndex, cell ->
                        val ref = CellRef(rowIndex, colIndex)
                        val color = when {
                            cell == null -> Color(0xFFE5E7EB)
                            cell.deltaPct >= 0 -> heatColor(Color(0xFFDC2626), abs(cell.deltaPct) / maxChangePct)
                            else -> heatColor(Color(0xFF2563EB), abs(cell.deltaPct) / maxChangePct)
                        }
                        val borderColor = when {
                            selectedCell == ref -> MaterialTheme.colorScheme.primary
                            highlightedCells.contains(ref) -> MaterialTheme.colorScheme.secondary
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(color, RoundedCornerShape(3.dp))
                                .clickable { onCellSelected(ref) }
                                .padding(0.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        selectedCell?.let {
            val suggestion = result.cellSuggestions.getOrNull(it.row)?.getOrNull(it.col)
            if (suggestion != null) {
                HorizontalDivider()
                InfoRow(strings["label.cell"], strings.format("label.cellCoords", suggestion.row, suggestion.col))
                InfoRow(strings["label.delta"], strings.format("label.deltaPct", suggestion.deltaPct * 100.0))
                InfoRow(strings["label.hits"], suggestion.hitCount.toString())
                InfoRow(strings["label.afrPair"], strings.format("label.afrPairValue", suggestion.meanAfrMeasured, suggestion.meanAfrTarget))
            }
        }
    }
}

@Composable
private fun ClusterSuggestionsCard(
    clusters: List<io.ecucore.tuning.SuggestionCluster>,
    includedClusterIds: Set<String>,
    onToggleInclude: (String) -> Unit,
    onPreview: (String) -> Unit
) {
    val strings = LocalStrings.current
    ActionCard(
        title = strings["label.tuningAssistantSuggestionsTitle"],
        description = if (clusters.isEmpty()) strings["label.noSuggestions"] else "Choose which suggestion clusters to apply."
    ) {
        if (clusters.isEmpty()) {
            Text(strings["label.noSuggestions"])
        } else {
            clusters.forEach { cluster ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (includedClusterIds.contains(cluster.id)) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cluster.label, fontWeight = FontWeight.Medium)
                            Text(
                                text = cluster.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = strings.format("label.clusterSummary", cluster.avgDeltaPct * 100.0, cluster.avgHits, cluster.rpmRange.first, cluster.rpmRange.last),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FilledTonalButton(onClick = { onPreview(cluster.id) }) {
                            Text(strings["label.preview"])
                        }
                        Switch(
                            checked = includedClusterIds.contains(cluster.id),
                            onCheckedChange = { onToggleInclude(cluster.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyCard(
    strategy: TuningStrategy,
    onChange: (TuningStrategy) -> Unit
) {
    val strings = LocalStrings.current
    ActionCard(
        title = strings["label.tuningAssistantStrategy"],
        description = strings["label.tuningAssistantStrategyDesc"]
    ) {
        DropdownField(
            label = strings["label.tuningAssistantStrategy"],
            value = strategy.displayName(strings),
            options = TuningStrategy.values().map { it.displayName(strings) }
        ) { value ->
            onChange(TuningStrategy.values().first { it.displayName(strings) == value })
        }
    }
}

@Composable
private fun ActionsCard(
    hasUndo: Boolean,
    isBusy: Boolean,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    onReload: () -> Unit,
    onCompare: () -> Unit
) {
    val strings = LocalStrings.current
    ActionCard(
        title = strings["label.tuningAssistantActions"],
        description = strings["label.tuningAssistantActionsDesc"]
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onApply, enabled = !isBusy) { Text(strings["label.tuningAssistantApply"]) }
            FilledTonalButton(onClick = onUndo, enabled = hasUndo && !isBusy) { Text(strings["label.tuningAssistantUndo"]) }
            FilledTonalButton(onClick = onReload, enabled = !isBusy) { Text(strings["label.tuningAssistantReload"]) }
            FilledTonalButton(onClick = onCompare) { Text(strings["label.tuningAssistantCompareAfterApply"]) }
        }
    }
}

private fun TuningStrategy.displayName(strings: Strings): String {
    return when (this) {
        TuningStrategy.CONSERVATIVE -> strings["label.tuningAssistantConservative"]
        TuningStrategy.STANDARD -> strings["label.tuningAssistantStandard"]
        TuningStrategy.AGGRESSIVE -> strings["label.tuningAssistantAggressive"]
    }
}

private fun heatColor(base: Color, intensity: Double): Color {
    val alpha = intensity.coerceIn(0.2, 1.0).toFloat()
    return base.copy(alpha = alpha)
}

private fun chooseOpenFile(title: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val fileName = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, fileName)
}


@Composable
private fun SavedLogsDialogDesktop(
    files: List<File>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sortedFiles = remember(files) { files.sortedByDescending { it.lastModified() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Saved logs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sortedFiles.isEmpty()) {
                    Text("No saved logs found.")
                } else {
                    sortedFiles.forEach { file ->
                        Text(
                            text = file.name,
                            modifier = Modifier.clickable { onSelect(file.absolutePath) }
                        )
                    }
                }
            }
        }
    )
}

private fun collectSavedLogFiles(lastSavedLogPath: String?): List<File> {
    val parent = lastSavedLogPath?.let { File(it).parentFile } ?: return emptyList()
    if (!parent.exists()) return emptyList()
    return parent.listFiles { file -> file.isFile && file.name.endsWith(".csv", ignoreCase = true) }?.toList().orEmpty()
}
