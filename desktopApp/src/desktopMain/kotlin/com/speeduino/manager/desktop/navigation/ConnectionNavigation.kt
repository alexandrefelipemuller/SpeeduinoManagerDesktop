package com.speeduino.manager.desktop.navigation

internal fun connectionNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionConnection",
        route = DesktopRoute.Connection,
        activeRoutes = setOf(
            DesktopRoute.Connection,
            DesktopRoute.ConnectionSettings,
            DesktopRoute.BluetoothConnection,
            DesktopRoute.UsbSerialConnection
        )
    )
}
