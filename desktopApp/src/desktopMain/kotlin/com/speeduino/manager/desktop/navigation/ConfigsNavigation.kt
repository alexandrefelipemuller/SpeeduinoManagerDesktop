package com.speeduino.manager.desktop.navigation

internal fun configsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionConfigs",
        routes = listOf(DesktopRoute.ConfigsTuning)
    )
}
