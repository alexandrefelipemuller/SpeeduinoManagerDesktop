package com.speeduino.manager.desktop.navigation

internal fun logsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionLogs",
        route = DesktopRoute.LogsEcuTools,
        activeRoutes = setOf(
            DesktopRoute.LogsEcuTools,
            DesktopRoute.LogViewer,
            DesktopRoute.RealTimeMonitor,
            DesktopRoute.LogAnalyzer,
            DesktopRoute.BeforeAfter
        )
    )
}
