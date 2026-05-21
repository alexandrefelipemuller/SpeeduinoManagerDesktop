package com.speeduino.manager.desktop.navigation

internal fun toolsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionTools",
        entries = listOf(
            NavEntry(DesktopRoute.Tools),
            NavEntry(DesktopRoute.LogsEcuTools),
            NavEntry(DesktopRoute.RealTimeMonitor),
            NavEntry(DesktopRoute.LogViewer),
            NavEntry(DesktopRoute.LogAnalyzer),
            NavEntry(DesktopRoute.BeforeAfter)
        )
    )
}
