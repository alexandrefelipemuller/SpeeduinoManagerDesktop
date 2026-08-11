package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import io.ecucore.SpeeduinoLiveData
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.InstitutionalScreenDesktop
import com.speeduino.manager.desktop.feature.app.DashboardScreen
import com.speeduino.manager.desktop.feature.app.HomeScreenDesktop
import com.speeduino.manager.desktop.feature.app.MoreScreenDesktop
import com.speeduino.manager.desktop.feature.app.SettingsScreen

@Composable
internal fun AppRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?,
    connectionState: ConnectionState,
    onToggleConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoute: (DesktopRoute) -> Unit,
    onOpenLogAnalyzer: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onReportProblem: () -> Unit,
    onOpenInstitutional: () -> Unit,
) {
    when (route) {
        DesktopRoute.Home -> HomeScreenDesktop(
            controller = controller,
            connectionState = connectionState,
            onToggleConnection = onToggleConnection,
            onOpenRoute = onOpenRoute,
            onOpenInstitutional = onOpenInstitutional
        )
        DesktopRoute.Settings -> SettingsScreen(controller)
        DesktopRoute.Institutional -> InstitutionalScreenDesktop(onReportProblem = onReportProblem)
        DesktopRoute.More -> MoreScreenDesktop(
            onOpenEngineSetup = { onOpenRoute(DesktopRoute.EngineSetup) },
            onOpenConnection = { onOpenRoute(DesktopRoute.Connection) },
            onOpenSettings = onOpenSettings,
            onOpenLogsEcuTools = { onOpenRoute(DesktopRoute.LogsEcuTools) },
            onOpenLogViewer = { onOpenRoute(DesktopRoute.LogViewer) },
            onOpenRealTimeMonitor = { onOpenRoute(DesktopRoute.RealTimeMonitor) },
            onOpenLogAnalyzer = onOpenLogAnalyzer,
            onOpenBeforeAfter = onOpenBeforeAfter,
            onOpenVirtualDyno = { onOpenRoute(DesktopRoute.VirtualDyno) },
            onReportProblem = onReportProblem,
            onOpenInstitutional = onOpenInstitutional
        )
        DesktopRoute.Dashboard -> DashboardScreen(controller, liveData, onOpenSettings = onOpenSettings)
        else -> Unit
    }
}
