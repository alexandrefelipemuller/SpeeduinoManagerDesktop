package com.speeduino.manager.desktop.navigation

internal fun connectionNavEntry(): NavEntry {
    return NavEntry(
        DesktopRoute.Connection,
        children = listOf(
            NavEntry(DesktopRoute.ConnectionSettings),
            NavEntry(DesktopRoute.BluetoothConnection),
            NavEntry(DesktopRoute.UsbSerialConnection)
        )
    )
}
