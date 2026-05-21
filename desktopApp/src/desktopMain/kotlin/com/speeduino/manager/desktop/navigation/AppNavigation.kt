package com.speeduino.manager.desktop.navigation

internal fun appNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionApp",
        entries = listOf(NavEntry(DesktopRoute.Settings))
    )
}

internal fun dashboardNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionDashboard",
        entries = listOf(NavEntry(DesktopRoute.Dashboard))
    )
}
