package com.speeduino.manager.desktop.feature.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onOpenInstitutional: () -> Unit,
) {
    val strings = LocalStrings.current
    val firmwareInfo by controller.firmwareInfo.collectAsState()
    val productString by controller.productString.collectAsState()
    val settings by controller.desktopSettings.collectAsState()
    var onboardingDismissed by remember(settings.gettingStartedDismissed) { mutableStateOf(settings.gettingStartedDismissed) }
    var feedbackDismissed by remember(settings.feedbackPromptDismissed) { mutableStateOf(settings.feedbackPromptDismissed) }
    var ratingDismissed by remember(settings.ratingPromptDismissed) { mutableStateOf(settings.ratingPromptDismissed) }
    var launchCountRecorded by remember { mutableStateOf(false) }


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
    LaunchedEffect(Unit) {
        if (!launchCountRecorded) {
            launchCountRecorded = true
            controller.saveDesktopSettings(settings.copy(appLaunchCount = settings.appLaunchCount + 1))
        }
    }

    val connectionLabel = when {
        connectionState.isConnected -> strings["status.connected"]
        connectionState.status == ConnectionStatus.Connecting -> strings["status.connecting"]
        connectionState.status == ConnectionStatus.Failed -> strings.format(
            "status.failed",
            connectionState.detail ?: strings["label.noData"]
        )
        else -> strings["status.disconnected"]
    }

    val hasFirstBootGaps = connectionState.isConnected && (
        !settings.firstBootEngineConstantsDone || !settings.firstBootInjectorsDone || !settings.firstBootIgnitionDone ||
            !settings.firstBootFuelDone || !settings.firstBootSensorsDone || !settings.firstBootOutputsDone || !settings.firstBootLivePanelDone
    )
    val firstBootChecklist = listOf(
        Triple(strings["home.firstBootEngineConstants"], settings.firstBootEngineConstantsDone, DesktopRoute.EngineConstants),
        Triple(strings["home.firstBootInjectors"], settings.firstBootInjectorsDone, DesktopRoute.InjectorConfig),
        Triple(strings["home.firstBootIgnition"], settings.firstBootIgnitionDone, DesktopRoute.Ignition),
        Triple(strings["home.firstBootFuel"], settings.firstBootFuelDone, DesktopRoute.Fuel),
        Triple(strings["home.firstBootSensors"], settings.firstBootSensorsDone, DesktopRoute.SensorsConfig),
        Triple(strings["home.firstBootOutputs"], settings.firstBootOutputsDone, DesktopRoute.InputOutputConfig),
        Triple(strings["home.firstBootLivePanel"], settings.firstBootLivePanelDone, DesktopRoute.Dashboard),
    )
    val shouldShowRatingPrompt = !ratingDismissed && !feedbackDismissed && settings.appLaunchCount >= 3

    fun markFirstBootProgress(route: DesktopRoute) {
        val updated = when (route) {
            DesktopRoute.EngineConstants -> settings.copy(firstBootEngineConstantsDone = true)
            DesktopRoute.InjectorConfig -> settings.copy(firstBootInjectorsDone = true)
            DesktopRoute.Ignition -> settings.copy(firstBootIgnitionDone = true)
            DesktopRoute.Fuel -> settings.copy(firstBootFuelDone = true)
            DesktopRoute.SensorsConfig -> settings.copy(firstBootSensorsDone = true)
            DesktopRoute.InputOutputConfig -> settings.copy(firstBootOutputsDone = true)
            DesktopRoute.Dashboard -> settings.copy(firstBootLivePanelDone = true)
            else -> null
        }
        if (updated != null) {
            controller.saveDesktopSettings(updated)
        }
    }

    fun openRoute(route: DesktopRoute) {
        markFirstBootProgress(route)
        onOpenRoute(route)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!onboardingDismissed) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings["home.gettingStartedTitle"],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings["home.gettingStartedBody"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings["home.gettingStartedStep1"])
                        Text(strings["home.gettingStartedStep2"])
                        Text(strings["home.gettingStartedStep3"])
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { openRoute(DesktopRoute.Connection) }) {
                            Text(strings["home.gettingStartedOpenConnection"])
                        }
                        FilledTonalButton(onClick = {
                            onboardingDismissed = true
                            controller.saveDesktopSettings(settings.copy(gettingStartedDismissed = true))
                        }) {
                            Text(strings["home.gettingStartedDismiss"])
                        }
                    }
                }
            }
        }
        if (hasFirstBootGaps) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings["home.firstBootTitle"],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings["home.firstBootSubtitle"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    firstBootChecklist.forEach { (label, checked, route) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { openRoute(route) }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { openRoute(route) },
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = strings["home.firstBootOpen"],
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }

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
                            OutlinedButton(onClick = { openRoute(DesktopRoute.Connection) }) {
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
            onOpenRoute = ::openRoute,
        )

        HomeSection(
            title = strings["home.setupTitle"],
            description = strings["home.setupSubtitle"],
            actions = setupActions,
            onOpenRoute = ::openRoute,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { openRoute(DesktopRoute.Settings) }) {
                        Text(strings["home.openBackupSettings"])
                    }
                    OutlinedButton(onClick = { controller.downloadAllConfigs() }) {
                        Text(strings["action.downloadConfig"])
                    }
                }
            }
        }

        if (!feedbackDismissed) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings["home.feedbackTitle"],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings["home.feedbackBody"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = onOpenInstitutional) {
                            Text(strings["home.feedbackOpenSupport"])
                        }
                        OutlinedButton(onClick = {
                            feedbackDismissed = true
                            controller.saveDesktopSettings(settings.copy(feedbackPromptDismissed = true))
                        }) {
                            Text(strings["home.feedbackDismiss"])
                        }
                    }
                }
            }
        }

        if (shouldShowRatingPrompt) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings["home.ratingTitle"],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings["home.ratingBody"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = onOpenInstitutional) {
                            Text(strings["home.ratingAction"])
                        }
                        OutlinedButton(onClick = {
                            ratingDismissed = true
                            controller.saveDesktopSettings(settings.copy(ratingPromptDismissed = true))
                        }) {
                            Text(strings["home.ratingDismiss"])
                        }
                    }
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
