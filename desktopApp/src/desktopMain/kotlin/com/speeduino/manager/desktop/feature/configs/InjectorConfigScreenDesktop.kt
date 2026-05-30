package com.speeduino.manager.desktop.feature.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.ui.DropdownField
import com.speeduino.manager.desktop.ui.NumberField
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.InjectorBatteryCorrectionMode

@Composable
internal fun InjectorConfigScreenDesktop(controller: DesktopSpeeduinoController) {
    val constants by controller.engineConstants.collectAsState()

    var openTime by remember(constants) { mutableStateOf(constants?.injectorOpenTimeMs?.toString() ?: "1.0") }
    var dutyLimit by remember(constants) { mutableStateOf(constants?.injectorDutyLimit?.toString() ?: "85") }
    var closeAngle by remember(constants) { mutableStateOf(constants?.injectorCloseAngle?.toString() ?: "355") }
    var correctionMode by remember(constants) { mutableStateOf(constants?.injectorBatteryCorrectionMode ?: InjectorBatteryCorrectionMode.OPEN_TIME_ONLY) }
    val voltageBins = remember(constants) { mutableStateListOf(*(constants?.batteryVoltageBins ?: listOf(6f, 8f, 10f, 12f, 14f, 16f)).map { it.toString() }.toTypedArray()) }
    val correctionRates = remember(constants) { mutableStateListOf(*(constants?.injectorVoltageCorrectionRates ?: listOf(160, 135, 115, 100, 92, 88)).map { it.toString() }.toTypedArray()) }
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
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Injector Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Dedicated injector setup brought over from the Android flow using the newer shared fields.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = controller::loadEngineConstants) { Text("Load from ECU") }
                    FilledTonalButton(
                        enabled = hasChanges && constants != null,
                        onClick = {
                            constants?.let { current ->
                                controller.saveEngineConstants(
                                    current.copy(
                                        injectorOpenTimeMs = openTime.toFloatOrNull() ?: current.injectorOpenTimeMs,
                                        injectorDutyLimit = dutyLimit.toIntOrNull() ?: current.injectorDutyLimit,
                                        injectorCloseAngle = closeAngle.toIntOrNull() ?: current.injectorCloseAngle,
                                        injectorBatteryCorrectionMode = correctionMode,
                                        batteryVoltageBins = voltageBins.map { it.toFloatOrNull() ?: 0f },
                                        injectorVoltageCorrectionRates = correctionRates.map { it.toIntOrNull() ?: 100 },
                                    )
                                )
                            }
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
                Text("Core Injector Values", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Open time (ms)", openTime, { openTime = it; hasChanges = true }, Modifier.weight(1f))
                    NumberField("Duty limit (%)", dutyLimit, { dutyLimit = it; hasChanges = true }, Modifier.weight(1f))
                    NumberField("Close angle", closeAngle, { closeAngle = it; hasChanges = true }, Modifier.weight(1f))
                }
                DropdownField("Battery correction target", correctionMode.name, InjectorBatteryCorrectionMode.values().map { it.name }) { value ->
                    correctionMode = InjectorBatteryCorrectionMode.valueOf(value)
                    hasChanges = true
                }
                HorizontalDivider()
                Text("Voltage Correction Table", style = MaterialTheme.typography.titleMedium)
                voltageBins.indices.forEach { index ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField("Voltage ${index + 1}", voltageBins[index], { value -> voltageBins[index] = value; hasChanges = true }, Modifier.weight(1f))
                        NumberField("Rate ${index + 1}", correctionRates[index], { value -> correctionRates[index] = value; hasChanges = true }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
