package com.speeduino.manager.desktop.navigation

internal fun toolsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionTools",
        routes = listOf(
            DesktopRoute.Tools,
            DesktopRoute.LogsEcuTools,
            DesktopRoute.RealTimeMonitor,
            DesktopRoute.LogViewer,
            DesktopRoute.LogAnalyzer,
            DesktopRoute.BeforeAfter
        )
    )
}
