package com.speeduino.manager.desktop.navigation

internal fun ignitionNavEntry(): NavEntry {
    return NavEntry(
        DesktopRoute.Ignition,
        children = listOf(
            NavEntry(DesktopRoute.IgnitionConfig),
            NavEntry(DesktopRoute.IgnitionTable),
            NavEntry(DesktopRoute.IgnitionTable2),
            NavEntry(DesktopRoute.DwellTable),
            NavEntry(DesktopRoute.TriggerSettings)
        )
    )
}

internal fun engineSetupNavEntry(): NavEntry {
    return NavEntry(
        DesktopRoute.EngineSetup,
        children = listOf(
            NavEntry(DesktopRoute.EngineConstants),
            NavEntry(DesktopRoute.InjectorConfig),
            NavEntry(DesktopRoute.InputOutputConfig),
            NavEntry(DesktopRoute.SensorsConfig),
            NavEntry(DesktopRoute.SecondarySerial)
        )
    )
}

internal fun engineOperationNavEntry(): NavEntry {
    return NavEntry(
        DesktopRoute.EngineOperation,
        children = listOf(
            NavEntry(DesktopRoute.IdleControl),
            NavEntry(DesktopRoute.ClosedLoopCorrections),
            NavEntry(DesktopRoute.EngineProtection),
            NavEntry(DesktopRoute.RevLimiterConfig)
        )
    )
}
