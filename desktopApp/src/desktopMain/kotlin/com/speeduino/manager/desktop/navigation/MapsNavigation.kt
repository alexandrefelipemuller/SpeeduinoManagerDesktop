package com.speeduino.manager.desktop.navigation

internal fun mapsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionMaps",
        route = DesktopRoute.MapsTables,
        activeRoutes = setOf(
            DesktopRoute.MapsTables,
            DesktopRoute.VeTable,
            DesktopRoute.VeTable2,
            DesktopRoute.IgnitionTable,
            DesktopRoute.IgnitionTable2,
            DesktopRoute.AfrTable,
            DesktopRoute.BaseMapWizard
        )
    )
}
