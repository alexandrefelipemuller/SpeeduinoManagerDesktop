package com.speeduino.manager.desktop.feature.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.AppLanguage
import com.speeduino.manager.desktop.AppProtocol
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.IniSelectionMode
import com.speeduino.manager.desktop.IniSelectionSource
import com.speeduino.manager.desktop.LocalizationManager
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.MANUAL_FIRMWARE_PROFILES
import com.speeduino.manager.desktop.ui.chooseOpenFile
import com.speeduino.manager.desktop.ui.chooseSaveFile
import com.speeduino.manager.desktop.ui.NumberField
import com.speeduino.manager.desktop.ui.DropdownField
import com.speeduino.manager.desktop.SHIFT_LIGHT_RPM_MAX
import com.speeduino.manager.desktop.SHIFT_LIGHT_RPM_MIN
import com.speeduino.manager.units.UnitSystem
import com.speeduino.manager.units.resolveEffectiveUnitSystem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SettingsScreen(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val language by LocalizationManager.language.collectAsState()
    val configState by controller.configState.collectAsState()
    val desktopSettings by controller.desktopSettings.collectAsState()
    val availableIniDefinitions by controller.availableIniDefinitions.collectAsState()
    val importedIniDefinitions by controller.importedIniDefinitions.collectAsState()
    val languageOptions = listOf(
        AppLanguage.EN to strings["app.languageEnglish"],
        AppLanguage.PT to strings["app.languagePortuguese"],
        AppLanguage.ES to strings["app.languageSpanish"],
        AppLanguage.FR to strings["app.languageFrench"],
        AppLanguage.ID to strings["app.languageIndonesian"],
        AppLanguage.ZH to strings["app.languageChinese"]
    )
    var draftSettings by remember(desktopSettings) { mutableStateOf(desktopSettings) }
    var shiftLightRpmText by remember(desktopSettings.shiftLightRpm) {
        mutableStateOf(desktopSettings.shiftLightRpm.toString())
    }
    val effectiveUnitSystem = resolveEffectiveUnitSystem(draftSettings.unitSystem)
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

                DropdownField(
                    label = "Protocol",
                    value = when (draftSettings.protocol) {
                        AppProtocol.MS_PROTOCOL -> "Speeduino / MS"
                        AppProtocol.ELM327_OBD2 -> "ELM327 OBD2"
                    },
                    options = listOf("Speeduino / MS", "ELM327 OBD2")
                ) { label ->
                    draftSettings = draftSettings.copy(
                        protocol = if (label == "ELM327 OBD2") AppProtocol.ELM327_OBD2 else AppProtocol.MS_PROTOCOL
                    )
                }

                DropdownField(
                    label = "Unit system",
                    value = when (draftSettings.unitSystem) {
                        UnitSystem.AUTO -> "Automatic"
                        UnitSystem.METRIC -> "Metric"
                        UnitSystem.IMPERIAL -> "Imperial"
                    },
                    options = listOf("Automatic", "Metric", "Imperial")
                ) { label ->
                    draftSettings = draftSettings.copy(
                        unitSystem = when (label) {
                            "Metric" -> UnitSystem.METRIC
                            "Imperial" -> UnitSystem.IMPERIAL
                            else -> UnitSystem.AUTO
                        }
                    )
                }
                Text(
                    text = "Effective unit system: " + when (effectiveUnitSystem) {
                        UnitSystem.AUTO -> "Automatic"
                        UnitSystem.METRIC -> "Metric"
                        UnitSystem.IMPERIAL -> "Imperial"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-connect on start",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Reconnects the last successful endpoint when the app opens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = draftSettings.autoConnectOnStart,
                        onCheckedChange = { checked ->
                            draftSettings = draftSettings.copy(autoConnectOnStart = checked)
                        }
                    )
                }

                NumberField(
                    label = "Shift light RPM",
                    value = shiftLightRpmText,
                    onValueChange = { value ->
                        val filtered = value.filter(Char::isDigit)
                        if (filtered.isNotBlank()) {
                            shiftLightRpmText = filtered
                            filtered.toIntOrNull()?.let { parsed ->
                                draftSettings = draftSettings.copy(
                                    shiftLightRpm = parsed.coerceIn(SHIFT_LIGHT_RPM_MIN, SHIFT_LIGHT_RPM_MAX)
                                )
                            }
                        }
                    }
                )
                Text(
                    text = "Range: $SHIFT_LIGHT_RPM_MIN - $SHIFT_LIGHT_RPM_MAX RPM",
                    style = MaterialTheme.typography.bodySmall,
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
                Text(
                    text = "INI Definition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                DropdownField(
                    label = "Definition mode",
                    value = if (draftSettings.iniSelectionMode == IniSelectionMode.AUTOMATIC) "Automatic" else "Manual",
                    options = listOf("Automatic", "Manual")
                ) { label ->
                    draftSettings = draftSettings.copy(
                        iniSelectionMode = if (label == "Automatic") IniSelectionMode.AUTOMATIC else IniSelectionMode.MANUAL,
                        iniDefinitionId = if (label == "Automatic") null else draftSettings.iniDefinitionId
                    )
                }
                if (draftSettings.iniSelectionMode == IniSelectionMode.MANUAL) {
                    DropdownField(
                        label = "Definition source",
                        value = if (draftSettings.iniSelectionSource == IniSelectionSource.CATALOG) "Remote catalog" else "Imported file",
                        options = listOf("Remote catalog", "Imported file")
                    ) { label ->
                        draftSettings = draftSettings.copy(
                            iniSelectionSource = if (label == "Remote catalog") IniSelectionSource.CATALOG else IniSelectionSource.IMPORTED,
                            iniDefinitionId = null
                        )
                    }
                    val definitionOptions = if (draftSettings.iniSelectionSource == IniSelectionSource.CATALOG) {
                        availableIniDefinitions.map { "${it.id} (${it.version})" }
                    } else {
                        importedIniDefinitions.map { "${it.fileName} (${it.signature})" }
                    }
                    val selectedDefinition = when (draftSettings.iniSelectionSource) {
                        IniSelectionSource.CATALOG -> availableIniDefinitions.firstOrNull { it.id == draftSettings.iniDefinitionId }
                            ?.let { "${it.id} (${it.version})" }
                        IniSelectionSource.IMPORTED -> importedIniDefinitions.firstOrNull { it.fileName == draftSettings.iniDefinitionId }
                            ?.let { "${it.fileName} (${it.signature})" }
                    }.orEmpty()
                    DropdownField(
                        label = "INI definition",
                        value = selectedDefinition.ifBlank { strings["app.noOptions"] },
                        options = definitionOptions.ifEmpty { listOf(strings["app.noOptions"]) }
                    ) { label ->
                        draftSettings = draftSettings.copy(
                            iniDefinitionId = when (draftSettings.iniSelectionSource) {
                                IniSelectionSource.CATALOG -> availableIniDefinitions.firstOrNull { "${it.id} (${it.version})" == label }?.id
                                IniSelectionSource.IMPORTED -> importedIniDefinitions.firstOrNull { "${it.fileName} (${it.signature})" == label }?.fileName
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { controller.refreshIniDefinitions(forceCatalogRefresh = true) }) {
                            Text("Refresh catalog")
                        }
                        FilledTonalButton(onClick = {
                            val source = chooseOpenFile("Import .ini file")
                            if (source != null) {
                                controller.importIniDefinition(source)
                            }
                        }) {
                            Text("Import .ini")
                        }
                    }
                }
                DropdownField(
                    label = "Manual firmware profile",
                    value = MANUAL_FIRMWARE_PROFILES.firstOrNull { it.signature == draftSettings.manualFirmwareProfile }?.label
                        ?: "Automatic detection",
                    options = listOf("Automatic detection") + MANUAL_FIRMWARE_PROFILES.map { it.label }
                ) { label ->
                    draftSettings = draftSettings.copy(
                        manualFirmwareProfile = MANUAL_FIRMWARE_PROFILES.firstOrNull { it.label == label }?.signature
                    )
                }
                Text(
                    text = "Manual firmware profile forces read-only safe mode, like Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = { controller.saveDesktopSettings(draftSettings) },
                    enabled = draftSettings != desktopSettings &&
                        (draftSettings.iniSelectionMode != IniSelectionMode.MANUAL || draftSettings.iniDefinitionId != null)
                ) {
                    Text("Save definition settings")
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
                    LinearProgressIndicator(
                        progress = { configState.progressPercent.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth()
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
