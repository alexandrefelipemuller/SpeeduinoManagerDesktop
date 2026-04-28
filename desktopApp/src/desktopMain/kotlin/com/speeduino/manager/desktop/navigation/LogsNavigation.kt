package com.speeduino.manager.desktop.navigation

internal fun logsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionLogs",
        routes = listOf(DesktopRoute.LogsEcuTools)
    )
}
