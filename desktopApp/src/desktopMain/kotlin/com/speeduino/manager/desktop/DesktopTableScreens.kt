package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ecucore.model.AfrTable
import io.ecucore.model.DwellTable
import io.ecucore.model.Color as SharedColor
import io.ecucore.model.IgnitionTable
import io.ecucore.model.VeTable

@Composable
internal fun PlaceholderScreen(title: String, message: String) {
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
internal fun VeTableScreenDesktop(controller: DesktopSpeeduinoController, mapIndex: Int) {
    val table by controller.veTableState(mapIndex).collectAsState()
    val strings = LocalStrings.current
    val description = buildString {
        append(strings["label.mapVe"])
        table?.let {
            append(" Load axis: ")
            append(if (it.loadType == VeTable.LoadType.TPS) "TPS %" else "MAP kPa")
        }
    }
    MapTableScreen(
        title = if (mapIndex == 1) strings["route.veTable"] else strings["route.veTable2"],
        description = description,
        table = table,
        onLoad = { controller.loadVeTable(mapIndex) },
        onSave = { controller.saveVeTable(it, mapIndex) },
        formatValue = { it.toString() },
        parseValue = { it.toIntOrNull() },
        valueRange = 0..255,
        cellColor = { VeTable.getColorForValue(it).toComposeColor() },
        loadAxisLabel = { if (it.loadType == VeTable.LoadType.TPS) "TPS % / RPM" else "MAP kPa / RPM" },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

@Composable
internal fun IgnitionTableScreenDesktop(controller: DesktopSpeeduinoController, mapIndex: Int) {
    val table by controller.ignitionTableState(mapIndex).collectAsState()
    val strings = LocalStrings.current
    val description = buildString {
        append(strings["label.mapIgnition"])
        table?.let {
            append(" Load axis: ")
            append(if (it.loadType == IgnitionTable.LoadType.TPS) "TPS %" else "MAP kPa")
        }
    }
    MapTableScreen(
        title = if (mapIndex == 1) strings["route.ignitionTable"] else strings["route.ignitionTable2"],
        description = description,
        table = table,
        onLoad = { controller.loadIgnitionTable(mapIndex) },
        onSave = { controller.saveIgnitionTable(it, mapIndex) },
        formatValue = { it.toString() },
        parseValue = { it.toIntOrNull() },
        valueRange = -40..70,
        cellColor = { IgnitionTable.getColorForValue(it).toComposeColor() },
        loadAxisLabel = { if (it.loadType == IgnitionTable.LoadType.TPS) "TPS % / RPM" else "MAP kPa / RPM" },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

@Composable
internal fun AfrTableScreenDesktop(controller: DesktopSpeeduinoController) {
    val table by controller.afrTable.collectAsState()
    val strings = LocalStrings.current
    val description = buildString {
        append(strings["label.mapAfr"])
        table?.let {
            append(" Load axis: ")
            append(if (it.loadType == AfrTable.LoadType.TPS) "TPS %" else "MAP kPa")
        }
    }
    MapTableScreen(
        title = strings["route.afrTable"],
        description = description,
        table = table,
        onLoad = controller::loadAfrTable,
        onSave = controller::saveAfrTable,
        formatValue = { AfrTable.formatValue(it) },
        parseValue = { parseAfrValue(it) },
        valueRange = 100..200,
        cellColor = { AfrTable.getColorForValue(it).toComposeColor() },
        loadAxisLabel = { if (it.loadType == AfrTable.LoadType.TPS) "TPS % / RPM" else "MAP kPa / RPM" },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

@Composable
internal fun DwellTableScreenDesktop(controller: DesktopSpeeduinoController) {
    val table by controller.dwellTable.collectAsState()
    val strings = LocalStrings.current
    val description = buildString {
        append("Ignition coil charge time table.")
        table?.let {
            append(" Load axis: ")
            append(if (it.loadType == IgnitionTable.LoadType.TPS) "TPS %" else "MAP kPa")
        }
    }
    MapTableScreen(
        title = strings["route.dwellTable"],
        description = description,
        table = table,
        onLoad = controller::loadDwellTable,
        onSave = controller::saveDwellTable,
        formatValue = { it.toString() },
        parseValue = { it.toIntOrNull() },
        valueRange = 0..10,
        cellColor = { dwellColor(it) },
        loadAxisLabel = { if (it.loadType == IgnitionTable.LoadType.TPS) "TPS % / RPM" else "MAP kPa / RPM" },
        rpmBins = { it.rpmBins },
        loadBins = { it.loadBins },
        values = { it.values },
        updateCell = { t, row, col, value -> t.setValue(row, col, value) },
        updateRpm = { t, index, value -> t.setRpmBin(index, value) },
        updateLoad = { t, index, value -> t.setLoadBin(index, value) }
    )
}

private fun dwellColor(value: Int): Color {
    val normalized = value.coerceIn(0, 10) / 10.0f
    return Color(
        red = 0.18f + (0.70f * normalized),
        green = 0.22f + (0.35f * (1f - normalized)),
        blue = 0.55f + (0.20f * (1f - normalized)),
        alpha = 1f
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
    loadAxisLabel: (T) -> String,
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
            androidx.compose.material3.IconButton(onClick = { invertYAxis = !invertYAxis }) {
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
                            HeaderCell(workingTable?.let(loadAxisLabel) ?: strings["label.loadRpm"])
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
        normalized.contains('.') -> normalized.toFloatOrNull()?.let { (it * 10).toInt() }
        else -> normalized.toIntOrNull()
    }
}
