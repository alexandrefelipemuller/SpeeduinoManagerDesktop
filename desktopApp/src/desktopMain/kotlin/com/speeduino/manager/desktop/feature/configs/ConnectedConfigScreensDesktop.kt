package com.speeduino.manager.desktop.feature.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.ui.DropdownField
import com.speeduino.manager.desktop.ui.NumberField
import com.speeduino.manager.desktop.ui.ToggleField
import com.speeduino.manager.model.CutMethod
import com.speeduino.manager.model.EngineProtectionConfig
import com.speeduino.manager.model.PressureCalibration
import com.speeduino.manager.model.ProtectionCut
import com.speeduino.manager.model.TpsCalibration
import kotlinx.coroutines.launch

@Composable
internal fun SensorsCalibrationScreenDesktop(controller: DesktopSpeeduinoController) {
    val connectionState by controller.connectionState.collectAsState()
    val scope = rememberCoroutineScope()
    var mapMin by remember { mutableStateOf("10") }
    var mapMax by remember { mutableStateOf("260") }
    var baroMin by remember { mutableStateOf("10") }
    var baroMax by remember { mutableStateOf("260") }
    var emapMin by remember { mutableStateOf("10") }
    var emapMax by remember { mutableStateOf("260") }
    var tpsMin by remember { mutableStateOf("0") }
    var tpsMax by remember { mutableStateOf("255") }
    var status by remember { mutableStateOf("Load the current calibration from the ECU before editing.") }

    fun loadFromEcu() {
        scope.launch {
            val pressure = controller.readPressureCalibration()
            val tps = controller.readTpsCalibration()
            if (pressure != null) {
                mapMin = pressure.mapMin.toString()
                mapMax = pressure.mapMax.toString()
                baroMin = pressure.baroMin.toString()
                baroMax = pressure.baroMax.toString()
                emapMin = pressure.emapMin.toString()
                emapMax = pressure.emapMax.toString()
            }
            if (tps != null) {
                tpsMin = tps.tpsMin.toString()
                tpsMax = tps.tpsMax.toString()
            }
            status = if (pressure != null || tps != null) "Calibration loaded from ECU." else "Unable to read calibration from ECU."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sensor Calibration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Pressure and TPS calibration path ported from Android, adapted to desktop.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = ::loadFromEcu, enabled = connectionState.isConnected) { Text("Load from ECU") }
                    FilledTonalButton(
                        enabled = connectionState.isConnected,
                        onClick = {
                            scope.launch {
                                val pressureResult = controller.writePressureCalibration(
                                    PressureCalibration(
                                        mapMin = mapMin.toIntOrNull() ?: 10,
                                        mapMax = mapMax.toIntOrNull() ?: 260,
                                        baroMin = baroMin.toIntOrNull() ?: 10,
                                        baroMax = baroMax.toIntOrNull() ?: 260,
                                        emapMin = emapMin.toIntOrNull() ?: 10,
                                        emapMax = emapMax.toIntOrNull() ?: 260,
                                    )
                                )
                                val tpsResult = controller.writeTpsCalibration(
                                    TpsCalibration(
                                        tpsMin = tpsMin.toIntOrNull() ?: 0,
                                        tpsMax = tpsMax.toIntOrNull() ?: 255,
                                    )
                                )
                                status = if (pressureResult.isSuccess && tpsResult.isSuccess) "Calibration saved to ECU." else "Failed to save one or more calibration blocks."
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pressure Sensors", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("MAP min", mapMin, { mapMin = it }, Modifier.weight(1f))
                    NumberField("MAP max", mapMax, { mapMax = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("BARO min", baroMin, { baroMin = it }, Modifier.weight(1f))
                    NumberField("BARO max", baroMax, { baroMax = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("EMAP min", emapMin, { emapMin = it }, Modifier.weight(1f))
                    NumberField("EMAP max", emapMax, { emapMax = it }, Modifier.weight(1f))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TPS Calibration", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Closed ADC", tpsMin, { tpsMin = it }, Modifier.weight(1f))
                    NumberField("Open ADC", tpsMax, { tpsMax = it }, Modifier.weight(1f))
                }
            }
        }
    }

    LaunchedEffect(connectionState.isConnected) {
        if (connectionState.isConnected) loadFromEcu()
    }
}

@Composable
internal fun EngineProtectionEditorScreenDesktop(controller: DesktopSpeeduinoController) {
    val config by controller.engineProtectionConfig.collectAsState()
    var protectionCut by remember(config) { mutableStateOf(config?.protectionCut ?: ProtectionCut.BOTH) }
    var cutMethod by remember(config) { mutableStateOf(config?.cutMethod ?: CutMethod.FULL) }
    var rpmMin by remember(config) { mutableStateOf(config?.engineProtectionRpmMin?.toString() ?: "1500") }
    var engineProtectEnabled by remember(config) { mutableStateOf(config?.engineProtectEnabled ?: false) }
    var revLimiterEnabled by remember(config) { mutableStateOf(config?.revLimiterEnabled ?: false) }
    var boostLimitEnabled by remember(config) { mutableStateOf(config?.boostLimitEnabled ?: false) }
    var oilEnabled by remember(config) { mutableStateOf(config?.oilPressureProtectionEnabled ?: false) }
    var afrEnabled by remember(config) { mutableStateOf(config?.afrProtectionEnabled ?: false) }
    var coolantEnabled by remember(config) { mutableStateOf(config?.coolantProtectionEnabled ?: false) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        hasChanges = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Engine Protection & Limiters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Controller-backed editor aligned with the Android flow.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = controller::loadEngineProtectionConfig) { Text("Load from ECU") }
                    FilledTonalButton(
                        enabled = hasChanges,
                        onClick = {
                            controller.saveEngineProtectionConfig(
                                EngineProtectionConfig(
                                    protectionCut = protectionCut,
                                    cutMethod = cutMethod,
                                    engineProtectionRpmMin = rpmMin.toIntOrNull() ?: 1500,
                                    engineProtectEnabled = engineProtectEnabled,
                                    revLimiterEnabled = revLimiterEnabled,
                                    boostLimitEnabled = boostLimitEnabled,
                                    oilPressureProtectionEnabled = oilEnabled,
                                    afrProtectionEnabled = afrEnabled,
                                    coolantProtectionEnabled = coolantEnabled,
                                )
                            )
                            hasChanges = false
                        }
                    ) { Text("Save") }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField("Protection cut", protectionCut.name, ProtectionCut.values().filter { it != ProtectionCut.OFF }.map { it.name }) { value ->
                    protectionCut = ProtectionCut.valueOf(value)
                    hasChanges = true
                }
                DropdownField("Cut method", cutMethod.name, CutMethod.values().map { it.name }) { value ->
                    cutMethod = CutMethod.valueOf(value)
                    hasChanges = true
                }
                NumberField("Minimum RPM", rpmMin, { rpmMin = it; hasChanges = true })
                ToggleField("Engine protection enabled", engineProtectEnabled) { engineProtectEnabled = it; hasChanges = true }
                ToggleField("Rev limiter enabled", revLimiterEnabled) { revLimiterEnabled = it; hasChanges = true }
                ToggleField("Boost limit enabled", boostLimitEnabled) { boostLimitEnabled = it; hasChanges = true }
                ToggleField("Oil pressure protection", oilEnabled) { oilEnabled = it; hasChanges = true }
                ToggleField("AFR protection", afrEnabled) { afrEnabled = it; hasChanges = true }
                ToggleField("Coolant protection", coolantEnabled) { coolantEnabled = it; hasChanges = true }
            }
        }
    }
}
