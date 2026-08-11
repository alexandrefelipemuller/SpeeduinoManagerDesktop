package com.speeduino.manager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ui.*

// Stub screens - Minimal implementation for iOS compilation

@Composable
fun IosToolsScreen(
    isConnected: Boolean = false,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToConnection: () -> Unit = {},
    onNavigateToMapsTables: () -> Unit = {},
    onNavigateToLogsEcuTools: () -> Unit = {},
    onNavigateToConfigs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiagnostic: () -> Unit = {},
    onNavigateToRealTimeMonitor: () -> Unit = {},
    onNavigateToLogViewer: () -> Unit = {},
    onNavigateToTuningAssistant: () -> Unit = {},
    onNavigateToBeforeAfter: () -> Unit = {},
    onNavigateToInstitutional: () -> Unit = {},
) {
    ScrollableScreen {
        SectionHeader(
            title = "Speeduino Manager",
            description = "ECU tuning and diagnostics",
        )

        ListCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusDot(active = isConnected)
                Text(
                    text = if (isConnected) "ECU Connected" else "ECU Disconnected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isConnected) SpeeduinoSuccess else SpeeduinoError,
                )
                if (isConnected) {
                    StatusPill(text = "Live", tone = SpeeduinoTone.Success)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dashboard")
            }
            Button(
                onClick = onNavigateToConnection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connections")
            }
            Button(
                onClick = onNavigateToMapsTables,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Maps & Tables")
            }
            Button(
                onClick = onNavigateToConfigs,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configuration")
            }
            Button(
                onClick = onNavigateToLogsEcuTools,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Logs & Tools")
            }
        }
    }
}

@Composable
fun IosHomeScreen(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToConnection: () -> Unit = {},
    onNavigateToMapsTables: () -> Unit = {},
    onNavigateToLogsEcuTools: () -> Unit = {},
    onNavigateToConfigs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiagnostic: () -> Unit = {},
    onNavigateToRealTimeMonitor: () -> Unit = {},
    onNavigateToLogViewer: () -> Unit = {},
    onNavigateToTuningAssistant: () -> Unit = {},
    onNavigateToBeforeAfter: () -> Unit = {},
    onNavigateToInstitutional: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
) {
    IosToolsScreen(
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToConnection = onNavigateToConnection,
        onNavigateToMapsTables = onNavigateToMapsTables,
        onNavigateToLogsEcuTools = onNavigateToLogsEcuTools,
        onNavigateToConfigs = onNavigateToConfigs,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToDiagnostic = onNavigateToDiagnostic,
        onNavigateToRealTimeMonitor = onNavigateToRealTimeMonitor,
        onNavigateToLogViewer = onNavigateToLogViewer,
        onNavigateToTuningAssistant = onNavigateToTuningAssistant,
        onNavigateToBeforeAfter = onNavigateToBeforeAfter,
        onNavigateToInstitutional = onNavigateToInstitutional,
    )
}

@Composable
fun IosConfigsScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Configuration", onBack = onBack)
        Text(
            text = "Configuration screen coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosLogsEcuToolsScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Logs & Tools", onBack = onBack)
        Text(
            text = "Logs and ECU tools coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosMapsTablesScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Maps & Tables", onBack = onBack)
        Text(
            text = "Maps and tables editor coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosDiagnosticScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Diagnostic Info", onBack = onBack)
        Text(
            text = "Diagnostic screen coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosRealTimeMonitorScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Real-Time Monitor", onBack = onBack)
        Text(
            text = "Real-time monitoring coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosLogViewerScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Log Viewer", onBack = onBack)
        Text(
            text = "Log viewer coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosBeforeAfterScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Before/After", onBack = onBack)
        Text(
            text = "Before/After comparison coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosBackupRestoreScreen(onBack: () -> Unit = {}) {
    ScrollableScreen {
        BackRow(title = "Backup & Restore", onBack = onBack)
        Text(
            text = "Backup & Restore coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosSettingsScreen(onBack: (() -> Unit)? = null) {
    ScrollableScreen {
        if (onBack != null) {
            BackRow(title = "Settings", onBack = onBack)
        } else {
            SectionHeader(
                title = "Settings",
                description = "App preferences",
            )
        }
        Text(
            text = "Settings screen coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun IosInstitutionalScreen() {
    ScrollableScreen {
        SectionHeader(
            title = "About",
            description = "App information",
        )
        Text(
            text = "Speeduino Manager for iOS\nVersion 1.0.4",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
