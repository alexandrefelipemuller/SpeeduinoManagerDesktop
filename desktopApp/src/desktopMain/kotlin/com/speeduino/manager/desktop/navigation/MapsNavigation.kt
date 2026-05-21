package com.speeduino.manager.desktop.navigation

internal fun fuelNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionFuel",
        entries = listOf(
            NavEntry(DesktopRoute.Fuel),
            NavEntry(DesktopRoute.MapsTables),
            NavEntry(DesktopRoute.VeTable),
            NavEntry(DesktopRoute.VeTable2),
            NavEntry(DesktopRoute.AfrTable),
            NavEntry(DesktopRoute.BaseMapWizard),
            NavEntry(DesktopRoute.TuningAssistant)
        )
    )
}
