package com.speeduino.manager.desktop.navigation

internal fun appNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionApp",
        routes = listOf(DesktopRoute.Settings)
    )
}

internal fun dashboardNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionDashboard",
        routes = listOf(DesktopRoute.Dashboard)
    )
}
