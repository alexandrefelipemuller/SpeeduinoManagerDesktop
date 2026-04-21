package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LogsEcuToolsScreenDesktop
import com.speeduino.manager.desktop.feature.logs.BeforeAfterScreenDesktop
import com.speeduino.manager.desktop.feature.logs.LogAnalyzerScreenDesktop
import com.speeduino.manager.desktop.feature.logs.LogViewerScreenDesktop
import com.speeduino.manager.desktop.feature.logs.RealTimeMonitorScreenDesktop

@Composable
internal fun LogsRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?,
    onOpenConnection: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenBeforeAfter: () -> Unit
) {
    when (route) {
        DesktopRoute.LogsEcuTools -> LogsEcuToolsScreenDesktop(
            onOpenDiagnostic = onOpenConnection,
            onOpenLogViewer = onOpenLogViewer,
            onOpenRealTimeMonitor = onOpenRealTimeMonitor,
            onOpenBeforeAfter = onOpenBeforeAfter
        )
        DesktopRoute.RealTimeMonitor -> RealTimeMonitorScreenDesktop(controller, liveData)
        DesktopRoute.LogViewer -> LogViewerScreenDesktop(controller)
        DesktopRoute.LogAnalyzer -> LogAnalyzerScreenDesktop(controller)
        DesktopRoute.BeforeAfter -> BeforeAfterScreenDesktop(controller)
        else -> Unit
    }
}
