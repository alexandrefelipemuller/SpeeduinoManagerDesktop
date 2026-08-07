package com.speeduino.manager.desktop.navigation

internal fun toolsNavEntry(): NavEntry {
    return NavEntry(
        DesktopRoute.Tools,
        children = listOf(
            connectionNavEntry(),
            NavEntry(DesktopRoute.RealTimeMonitor),
            NavEntry(DesktopRoute.LogViewer),
            NavEntry(DesktopRoute.LogAnalyzer),
            NavEntry(DesktopRoute.BeforeAfter),
            NavEntry(DesktopRoute.VirtualDyno),
            NavEntry(DesktopRoute.Settings),
            NavEntry(DesktopRoute.Institutional),
        )
    )
}
