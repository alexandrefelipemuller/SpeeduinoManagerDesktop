package com.speeduino.manager.desktop.feature.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.ConnectionStatus
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.navigation.DesktopRoute

private data class HomeAction(
    val route: DesktopRoute,
    val title: String,
    val description: String,
)

@Composable
internal fun HomeScreenDesktop(
    controller: DesktopSpeeduinoController,
    connectionState: ConnectionState,
    onToggleConnection: () -> Unit,
    onOpenRoute: (DesktopRoute) -> Unit,
) {
    val strings = LocalStrings.current
    val firmwareInfo by controller.firmwareInfo.collectAsState()
    val productString by controller.productString.collectAsState()

    val exploreActions = listOf(
        HomeAction(
            route = DesktopRoute.Dashboard,
            title = strings[DesktopRoute.Dashboard.labelKey],
            description = strings["home.dashboardDescription"],
        ),
        HomeAction(
            route = DesktopRoute.Fuel,
            title = strings[DesktopRoute.Fuel.labelKey],
            description = strings["home.fuelDescription"],
        ),
        HomeAction(
            route = DesktopRoute.Ignition,
            title = strings[DesktopRoute.Ignition.labelKey],
            description = strings["home.ignitionDescription"],
        ),
        HomeAction(
            route = DesktopRoute.EngineSetup,
            title = strings[DesktopRoute.EngineSetup.labelKey],
            description = strings["home.engineSetupDescription"],
        ),
        HomeAction(
            route = DesktopRoute.EngineOperation,
            title = strings[DesktopRoute.EngineOperation.labelKey],
            description = strings["home.engineOperationDescription"],
        ),
        HomeAction(
            route = DesktopRoute.Tools,
            title = strings[DesktopRoute.Tools.labelKey],
            description = strings["home.toolsDescription"],
        ),
    )
    val setupActions = listOf(
        HomeAction(
            route = DesktopRoute.Connection,
            title = strings[DesktopRoute.Connection.labelKey],
            description = strings["home.connectionDescription"],
        ),
        HomeAction(
            route = DesktopRoute.Settings,
            title = strings[DesktopRoute.Settings.labelKey],
            description = strings["home.settingsDescription"],
        ),
    )
    val connectionLabel = when {
        connectionState.isConnected -> strings["status.connected"]
        connectionState.status == ConnectionStatus.Connecting -> strings["status.connecting"]
        connectionState.status == ConnectionStatus.Failed -> strings.format(
            "status.failed",
            connectionState.detail ?: strings["label.noData"]
        )
        else -> strings["status.disconnected"]
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings[DesktopRoute.Home.labelKey],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings["home.subtitle"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings["status.label"],
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = connectionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = productString ?: firmwareInfo?.signature ?: strings["label.noData"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onToggleConnection) {
                                Text(
                                    if (connectionState.isConnected) {
                                        strings["action.disconnect"]
                                    } else {
                                        strings["action.connect"]
                                    }
                                )
                            }
                            OutlinedButton(onClick = { onOpenRoute(DesktopRoute.Connection) }) {
                                Text(strings[DesktopRoute.Connection.labelKey])
                            }
                        }
                    }
                }
            }
        }

        HomeSection(
            title = strings["home.exploreTitle"],
            description = strings["home.exploreSubtitle"],
            actions = exploreActions,
            onOpenRoute = onOpenRoute,
        )

        HomeSection(
            title = strings["home.setupTitle"],
            description = strings["home.setupSubtitle"],
            actions = setupActions,
            onOpenRoute = onOpenRoute,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                }
                Spacer(modifier = Modifier.size(12.dp))
                FilledTonalButton(onClick = { onOpenRoute(DesktopRoute.Settings) }) {
                    Text(strings["home.openBackupSettings"])
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    description: String,
    actions: List<HomeAction>,
    onOpenRoute: (DesktopRoute) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HomeActionGrid(actions = actions, onOpenRoute = onOpenRoute)
    }
}

@Composable
private fun HomeActionGrid(
    actions: List<HomeAction>,
    onOpenRoute: (DesktopRoute) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth < 760.dp -> 1
            maxWidth < 1120.dp -> 2
            else -> 4
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { action ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenRoute(action.route) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    BoxWithConstraints(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = action.route.icon,
                                            contentDescription = action.title,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
