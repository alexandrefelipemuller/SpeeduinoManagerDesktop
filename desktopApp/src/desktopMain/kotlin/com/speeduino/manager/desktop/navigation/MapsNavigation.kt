package com.speeduino.manager.desktop.navigation

internal fun mapsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionMaps",
        routes = listOf(DesktopRoute.MapsTables)
    )
}
