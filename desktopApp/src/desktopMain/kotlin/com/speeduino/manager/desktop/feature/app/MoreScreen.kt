package com.speeduino.manager.desktop.feature.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.LocalStrings

@Composable
internal fun MoreScreenDesktop(
    onOpenEngineSetup: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogsEcuTools: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenLogAnalyzer: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onOpenVirtualDyno: () -> Unit,
    onReportProblem: () -> Unit,
    onOpenInstitutional: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HubCard(strings["nav.sectionMore"], strings["label.logsEcuToolsSubtitle"])
        SectionCard(strings["label.morePrimaryGroup"]) {
            MoreAction(strings["route.engineSetup"], strings["route.engineSetup"], Icons.Default.Tune, onOpenEngineSetup)
            MoreAction(strings["route.connection"], strings["route.connection"], Icons.Default.Cable, onOpenConnection)
            MoreAction(strings["app.settingsTitle"], strings["app.settingsTitle"], Icons.Default.Settings, onOpenSettings)
        }
        SectionCard(strings["label.moreSecondaryGroup"]) {
            MoreAction(strings["label.runtimeDiagnostics"], strings["label.runtimeDiagnostics"], Icons.Default.Cable, onOpenConnection)
            MoreAction(strings["label.logsEcuToolsTitle"], strings["label.logsEcuToolsSubtitle"], Icons.Default.Speed, onOpenLogsEcuTools)
            MoreAction(strings["label.logViewerTitle"], strings["label.logViewerUtilitySummary"], Icons.Default.Speed, onOpenLogViewer)
            MoreAction(strings["route.realTimeMonitor"], strings["label.realtimeUtilityDesc"], Icons.Default.Speed, onOpenRealTimeMonitor)
            MoreAction(strings["route.logAnalyzer"], strings["label.toolsLogAnalyzerDesc"], Icons.Default.Speed, onOpenLogAnalyzer)
            MoreAction(strings["route.beforeAfter"], strings["label.toolsBeforeAfterDesc"], Icons.Default.Speed, onOpenBeforeAfter)
            MoreAction(strings["route.virtualDyno"], strings["label.virtualDynoUtilityDesc"], Icons.Default.Speed, onOpenVirtualDyno)
            MoreAction(strings["label.reportProblem"], strings["more.reportProblemDesc"], Icons.Default.BugReport, onReportProblem)
            MoreAction(strings["label.institutionalTitle"], strings["more.institutionalDesc"], Icons.Default.Info, onOpenInstitutional)
        }
    }
}

@Composable
private fun HubCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            content()
        }
    }
}

@Composable
private fun MoreAction(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
