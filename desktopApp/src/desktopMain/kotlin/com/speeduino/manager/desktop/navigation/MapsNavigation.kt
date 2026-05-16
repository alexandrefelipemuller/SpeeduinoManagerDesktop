package com.speeduino.manager.desktop.navigation

internal fun fuelNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionFuel",
        routes = listOf(
            DesktopRoute.Fuel,
            DesktopRoute.MapsTables,
            DesktopRoute.VeTable,
            DesktopRoute.VeTable2,
            DesktopRoute.AfrTable,
            DesktopRoute.BaseMapWizard,
            DesktopRoute.TuningAssistant
        )
    )
}
