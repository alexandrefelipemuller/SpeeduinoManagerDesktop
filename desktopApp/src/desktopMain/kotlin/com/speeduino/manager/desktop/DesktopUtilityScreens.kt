package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenLogAnalyzer: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onOpenHistoricalLogViewer: (String) -> Unit
) {
    val strings = LocalStrings.current
    UtilityScreenFrame(
        title = strings["route.tools"],
        subtitle = strings["label.toolsSubtitle"]
    ) {
        UtilityActionCard(
            title = strings["route.realTimeMonitor"],
            description = "Watch live data and capture it to CSV or MSL.",
            buttonLabel = strings["label.institutionalRealtimeCta"],
            onClick = onOpenRealTimeMonitor
        )
        UtilityActionCard(
            title = strings["route.logViewer"],
            description = "Inspect captured logs or open an older CSV log manually.",
            buttonLabel = strings["label.institutionalLogViewerCta"],
            onClick = onOpenLogViewer
        )
        UtilityActionCard(
            title = strings["route.logAnalyzer"],
            description = "Analyze a CSV log and generate VE tuning suggestions.",
            buttonLabel = strings["action.analyze"],
            onClick = onOpenLogAnalyzer
        )
        UtilityActionCard(
            title = strings["label.logViewerOpenCsvAction"],
            description = "Choose an older CSV log file and open it directly in the desktop viewer.",
            buttonLabel = strings["label.logViewerChooseCsv"],
            onClick = {
                chooseOpenFile(strings["label.logViewerOpenCsvTitle"])?.absolutePath?.let(onOpenHistoricalLogViewer)
            }
        )
        UtilityActionCard(
            title = strings["route.beforeAfter"],
            description = "Compare before and after logs to see AFR improvements.",
            buttonLabel = strings["label.institutionalBeforeAfterCta"],
            onClick = onOpenBeforeAfter
        )
    }
}

@Composable
internal fun InstitutionalScreenDesktop() {
    val strings = LocalStrings.current
    val repoUrl = "https://github.com/alexandrefelipemuller/SpeeduinoManagerDesktop"

    UtilityScreenFrame(
        title = strings["label.institutionalTitle"],
        subtitle = strings["label.institutionalSubtitle"]
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Speeduino Manager Desktop",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Desktop build with shared ECU tooling, tuning flows and log analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { openExternalUri(repoUrl) }) {
                        Text(strings["label.institutionalRepoButton"])
                    }
                    Button(onClick = { openExternalUri(repoUrl) }) {
                        Text("Source code")
                    }
                }
            }
        }

        UtilityActionCard(
            title = "What is covered",
            description = "Shared OBD2, VE analyzer, before/after comparison, configuration editors and connection workflows.",
            buttonLabel = "",
            onClick = { }
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

private fun openExternalUri(uri: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(uri))
        }
    }
}
