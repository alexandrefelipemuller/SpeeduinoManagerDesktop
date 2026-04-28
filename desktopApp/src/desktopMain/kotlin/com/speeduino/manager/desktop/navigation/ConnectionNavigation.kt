package com.speeduino.manager.desktop.navigation

internal fun connectionNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionConnection",
        routes = listOf(DesktopRoute.Connection)
    )
}
