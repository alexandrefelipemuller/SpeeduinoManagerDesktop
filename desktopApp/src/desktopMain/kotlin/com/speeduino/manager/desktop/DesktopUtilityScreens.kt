@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ui.chooseOpenFile
import java.awt.Desktop
import java.net.URI

@Composable
internal fun LogsEcuToolsScreenDesktop(
    onOpenConnection: () -> Unit,
    onOpenConnectionSettings: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenLogAnalyzer: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onOpenVirtualDyno: () -> Unit,
    onOpenHistoricalLogViewer: (String) -> Unit
) {
    val strings = LocalStrings.current
    UtilityScreenFrame(
        title = strings["route.tools"],
        subtitle = strings["label.toolsSubtitle"]
    ) {
        UtilityActionCard(
            title = strings["route.connection"],
            description = strings["label.toolsConnectionDesc"],
            buttonLabel = strings["route.connection"],
            onClick = onOpenConnection
        )
        UtilityActionCard(
            title = strings["label.wifiTcp"],
            description = strings["label.toolsConnectionSettingsDesc"],
            buttonLabel = strings["app.settingsTitle"],
            onClick = onOpenConnectionSettings
        )
        UtilityActionCard(
            title = strings["label.runtimeDiagnostics"],
            description = strings["msg_diagnostic_runtime_description"],
            buttonLabel = strings["action.open"],
            onClick = onOpenConnection
        )
        UtilityActionCard(
            title = strings["route.realTimeMonitor"],
            description = strings["label.realtimeUtilityDesc"],
            buttonLabel = strings["label.institutionalRealtimeCta"],
            onClick = onOpenRealTimeMonitor
        )
        UtilityActionCard(
            title = strings["route.logViewer"],
            description = strings["label.logViewerUtilitySummary"],
            buttonLabel = strings["label.institutionalLogViewerCta"],
            onClick = onOpenLogViewer
        )
        UtilityActionCard(
            title = strings["route.logAnalyzer"],
            description = strings["label.logAnalyzerUtilityDesc"],
            buttonLabel = strings["action.analyze"],
            onClick = onOpenLogAnalyzer
        )
        UtilityActionCard(
            title = strings["label.logViewerOpenCsvAction"],
            description = strings["label.logViewerUtilityDesc"],
            buttonLabel = strings["label.logViewerChooseCsv"],
            onClick = {
                chooseOpenFile(strings["label.logViewerOpenCsvTitle"])?.absolutePath?.let(onOpenHistoricalLogViewer)
            }
        )
        UtilityActionCard(
            title = strings["route.beforeAfter"],
            description = strings["label.beforeAfterUtilityDesc"],
            buttonLabel = strings["label.institutionalBeforeAfterCta"],
            onClick = onOpenBeforeAfter
        )
        UtilityActionCard(
            title = strings["route.virtualDyno"],
            description = strings["label.virtualDynoUtilityDesc"],
            buttonLabel = strings["action.open"],
            onClick = onOpenVirtualDyno
        )
    }
}

@Composable
internal fun InstitutionalScreenDesktop(onReportProblem: () -> Unit) {
    val strings = LocalStrings.current
    val repoUrl = "https://github.com/alexandrefelipemuller/SpeeduinoManagerDesktop"

    UtilityScreenFrame(
        title = strings["label.institutionalTitle"],
        subtitle = strings["label.institutionalSubtitle"]
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = strings["label.institutionalHero"],
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings["label.institutionalHeroDetail"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PromoChip(strings["label.institutionalFeatureOne"])
                    PromoChip(strings["label.institutionalFeatureTwo"])
                    PromoChip(strings["label.institutionalFeatureThree"])
                    PromoChip(strings["label.institutionalFeatureFour"])
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { openExternalUri(repoUrl) }) {
                        Text(strings["label.institutionalPrimaryCta"])
                    }
                    Button(onClick = onReportProblem) {
                        Text(strings["label.institutionalSecondaryCta"])
                    }
                    FilledTonalButton(onClick = { openExternalUri(repoUrl) }) {
                        Text(strings["label.institutionalTertiaryCta"])
                    }
                }
            }
        }

        UtilityActionCard(
            title = strings["label.institutionalCoverageTitle"],
            description = strings["label.institutionalCoverageBody"],
            buttonLabel = strings["label.institutionalPrimaryCta"],
            onClick = { openExternalUri(repoUrl) }
        )
    }
}

@Composable
private fun UtilityScreenFrame(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF5F2EC),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun UtilityActionCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (buttonLabel.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onClick) {
                        Text(buttonLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun openExternalUri(uri: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(uri))
        }
    }
}
