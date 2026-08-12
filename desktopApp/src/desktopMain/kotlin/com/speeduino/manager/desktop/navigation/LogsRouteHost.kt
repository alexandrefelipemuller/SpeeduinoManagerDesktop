package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import io.ecucore.SpeeduinoLiveData
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LogsEcuToolsScreenDesktop
import com.speeduino.manager.desktop.feature.logs.LogViewerScreenDesktop
import com.speeduino.manager.desktop.feature.logs.RealTimeMonitorScreenDesktop

@Composable
internal fun LogsRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?,
    onOpenConnection: () -> Unit,
    onOpenConnectionSettings: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInstitutional: () -> Unit,
    onOpenHistoricalLogViewer: (String) -> Unit
) {
    when (route) {
        DesktopRoute.Tools,
        DesktopRoute.LogsEcuTools -> LogsEcuToolsScreenDesktop(
            onOpenConnection = onOpenConnection,
            onOpenConnectionSettings = onOpenConnectionSettings,
            onOpenLogViewer = onOpenLogViewer,
            onOpenRealTimeMonitor = onOpenRealTimeMonitor,
            onOpenSettings = onOpenSettings,
            onOpenInstitutional = onOpenInstitutional,
            onOpenHistoricalLogViewer = onOpenHistoricalLogViewer
        )
        DesktopRoute.RealTimeMonitor -> RealTimeMonitorScreenDesktop(controller, liveData)
        DesktopRoute.LogViewer -> LogViewerScreenDesktop(controller)
        else -> Unit
    }
}
