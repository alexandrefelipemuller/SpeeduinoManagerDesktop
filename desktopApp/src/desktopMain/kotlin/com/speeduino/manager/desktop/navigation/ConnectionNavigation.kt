package com.speeduino.manager.desktop.navigation

internal fun connectionNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionConnection",
        entries = listOf(
            NavEntry(
                DesktopRoute.Connection,
                children = listOf(
                    DesktopRoute.ConnectionSettings,
                    DesktopRoute.BluetoothConnection,
                    DesktopRoute.UsbSerialConnection
                )
            )
        )
    )
}
