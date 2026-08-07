package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import io.ecucore.SpeeduinoLiveData
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LogsEcuToolsScreenDesktop
import com.speeduino.manager.desktop.feature.logs.BeforeAfterScreenDesktop
import com.speeduino.manager.desktop.feature.logs.LogAnalyzerScreenDesktop
import com.speeduino.manager.desktop.feature.logs.LogViewerScreenDesktop
import com.speeduino.manager.desktop.feature.logs.RealTimeMonitorScreenDesktop
import com.speeduino.manager.desktop.feature.logs.VirtualDynoScreenDesktop

@Composable
internal fun LogsRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?,
    onOpenConnection: () -> Unit,
    onOpenConnectionSettings: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenLogAnalyzer: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onOpenVirtualDyno: () -> Unit,
    onOpenHistoricalLogViewer: (String) -> Unit
) {
    when (route) {
        DesktopRoute.Tools,
        DesktopRoute.LogsEcuTools -> LogsEcuToolsScreenDesktop(
            onOpenConnection = onOpenConnection,
            onOpenConnectionSettings = onOpenConnectionSettings,
            onOpenLogViewer = onOpenLogViewer,
            onOpenRealTimeMonitor = onOpenRealTimeMonitor,
            onOpenLogAnalyzer = onOpenLogAnalyzer,
            onOpenBeforeAfter = onOpenBeforeAfter,
            onOpenVirtualDyno = onOpenVirtualDyno,
            onOpenHistoricalLogViewer = onOpenHistoricalLogViewer
        )
        DesktopRoute.RealTimeMonitor -> RealTimeMonitorScreenDesktop(controller, liveData)
        DesktopRoute.LogViewer -> LogViewerScreenDesktop(controller)
        DesktopRoute.LogAnalyzer -> LogAnalyzerScreenDesktop(controller)
        DesktopRoute.BeforeAfter -> BeforeAfterScreenDesktop(controller)
        DesktopRoute.VirtualDyno -> VirtualDynoScreenDesktop(controller)
        else -> Unit
    }
}
