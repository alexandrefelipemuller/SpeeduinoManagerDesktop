package com.speeduino.manager.desktop.navigation

internal fun fuelNavEntry(): NavEntry {
    return NavEntry(
        DesktopRoute.Fuel,
        children = listOf(
            NavEntry(DesktopRoute.VeTable),
            NavEntry(DesktopRoute.VeTable2),
            NavEntry(DesktopRoute.AfrTable),
            NavEntry(DesktopRoute.BaseMapWizard),
            NavEntry(DesktopRoute.TuningAssistant)
        )
    )
}
