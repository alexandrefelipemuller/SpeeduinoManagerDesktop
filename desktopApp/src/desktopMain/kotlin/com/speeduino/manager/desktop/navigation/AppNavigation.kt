package com.speeduino.manager.desktop.navigation

internal fun appNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionApp",
        route = DesktopRoute.Settings,
        activeRoutes = setOf(DesktopRoute.Settings)
    )
}

internal fun dashboardNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionDashboard",
        route = DesktopRoute.Dashboard
    )
}
