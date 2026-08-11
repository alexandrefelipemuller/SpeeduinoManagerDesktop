package com.speeduino.manager.desktop.feature.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ecucore.SpeeduinoLiveData
import com.speeduino.manager.desktop.DesktopDashboardMode
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.Strings
import java.util.Locale

@Composable
internal fun DashboardScreen(
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    val availableSignals = remember(liveData, strings) { dashboardGaugeSignals(liveData, strings) }
    val dashboardMode = controller.desktopSettings.collectAsState().value.dashboardMode
    var selectedGaugeKeys by remember(dashboardMode) {
        mutableStateOf(defaultGaugeKeys(dashboardMode))
    }

    LaunchedEffect(availableSignals.map { it.key }) {
        selectedGaugeKeys = defaultGaugeKeys(dashboardMode).map { key ->
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
        AppSectionCard(onOpenSettings = onOpenSettings, dashboardMode = dashboardMode)
        GaugeGrid(
            dashboardMode = dashboardMode,
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
private fun AppSectionCard(onOpenSettings: () -> Unit, dashboardMode: DesktopDashboardMode) {
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.format("label.dashboardMode", dashboardMode.name.lowercase().replaceFirstChar(Char::uppercaseChar)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = onOpenSettings) {
                    Text(strings["app.settingsLabel"])
                }
            }
        }
    }
}

@Composable
private fun GaugeGrid(
    dashboardMode: DesktopDashboardMode,
    gauges: List<GaugeSpec>,
    availableSignals: List<GaugeSpec>,
    onChangeGauge: (Int, String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when (dashboardMode) {
            DesktopDashboardMode.APEX -> 3
            DesktopDashboardMode.FUTURE -> if (maxWidth < 900.dp) 3 else 6
            DesktopDashboardMode.PETROL -> 4
            DesktopDashboardMode.DEFAULT -> if (maxWidth < 900.dp) 2 else 4
        }
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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

private data class StatItem(
    val label: String,
    val value: String
)

private fun dashboardGaugeSignals(liveData: SpeeduinoLiveData?, strings: Strings): List<GaugeSpec> {
    fun valueOrZero(value: Float?): Float = value ?: 0f
    fun display0(value: Float?): String = if (value == null) strings["label.noData"] else value.toInt().toString()
    fun display1(value: Float?): String = if (value == null) strings["label.noData"] else String.format(Locale.US, "%.1f", value)

    val afrMeasured = liveData?.o2?.toFloat()
    val afrTarget: Float? = null
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

private fun defaultGaugeKeys(mode: DesktopDashboardMode): List<String> = when (mode) {
    DesktopDashboardMode.DEFAULT -> listOf("rpm", "map", "tps", "coolant")
    DesktopDashboardMode.PETROL -> listOf("rpm", "map", "afr", "advance")
    DesktopDashboardMode.FUTURE -> listOf("rpm", "map", "tps", "coolant", "iat", "battery")
    DesktopDashboardMode.APEX -> listOf("rpm", "advance", "afr", "battery")
}
