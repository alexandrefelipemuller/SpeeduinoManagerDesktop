package com.speeduino.manager.desktop.navigation

internal fun ignitionNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionIgnition",
        entries = listOf(
            NavEntry(DesktopRoute.Ignition),
            NavEntry(DesktopRoute.IgnitionTable),
            NavEntry(DesktopRoute.IgnitionTable2),
            NavEntry(DesktopRoute.DwellTable),
            NavEntry(DesktopRoute.TriggerSettings)
        )
    )
}

internal fun engineSetupNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionEngineSetup",
        entries = listOf(
            NavEntry(DesktopRoute.EngineSetup),
            NavEntry(DesktopRoute.ConfigsTuning),
            NavEntry(DesktopRoute.EngineConstants),
            NavEntry(DesktopRoute.InjectorConfig),
            NavEntry(DesktopRoute.InputOutputConfig),
            NavEntry(DesktopRoute.SensorsConfig),
            NavEntry(DesktopRoute.SecondarySerial)
        )
    )
}

internal fun engineOperationNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionEngineOperation",
        entries = listOf(
            NavEntry(DesktopRoute.EngineOperation),
            NavEntry(DesktopRoute.IdleControl),
            NavEntry(DesktopRoute.ClosedLoopCorrections),
            NavEntry(DesktopRoute.EngineProtection),
            NavEntry(DesktopRoute.RevLimiterConfig)
        )
    )
}
