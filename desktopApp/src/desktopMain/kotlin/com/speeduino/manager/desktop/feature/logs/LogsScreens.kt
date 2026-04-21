package com.speeduino.manager.desktop.feature.logs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.compare.LogCompareResult
import com.speeduino.manager.compare.LogHeatCellState
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.DefaultSelectedLogSignals
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.LogExportSignals
import com.speeduino.manager.desktop.ui.InfoRow
import com.speeduino.manager.desktop.ui.NumberField
import com.speeduino.manager.desktop.ui.ToggleField
import com.speeduino.manager.desktop.ui.chooseOpenFile
import com.speeduino.manager.model.logging.LiveLogEntry
import com.speeduino.manager.model.logging.LiveLogSnapshot
import com.speeduino.manager.tuning.CellRef
import com.speeduino.manager.tuning.CellSuggestion
import com.speeduino.manager.tuning.TuningStrategy
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun RealTimeMonitorScreenDesktop(
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
                    liveData?.batteryVoltage?.let { String.format(Locale.US, "%.1f", it) } ?: strings["label.noData"]
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
internal fun LogViewerScreenDesktop(controller: DesktopSpeeduinoController) {
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
internal fun LogAnalyzerScreenDesktop(controller: DesktopSpeeduinoController) {
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
internal fun BeforeAfterScreenDesktop(controller: DesktopSpeeduinoController) {
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
private fun BeforeAfterSummary(strings: com.speeduino.manager.desktop.Strings, result: LogCompareResult) {
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
    suggestions: List<List<CellSuggestion?>>,
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
    entries: List<LiveLogEntry>,
    strings: com.speeduino.manager.desktop.Strings
): List<LogSeries> {
    if (entries.isEmpty()) return emptyList()

    fun build(
        name: String,
        color: Color,
        extractor: (LiveLogEntry) -> Float
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
private fun LogMetadataSummary(entries: List<LiveLogEntry>) {
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
        String.format(Locale.US, "%.1f", min)
    } else {
        String.format(Locale.US, "%.1f-%.1f", min, max)
    }
}
