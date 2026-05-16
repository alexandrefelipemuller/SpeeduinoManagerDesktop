package com.speeduino.manager.desktop.navigation

internal fun ignitionNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionIgnition",
        routes = listOf(
            DesktopRoute.Ignition,
            DesktopRoute.IgnitionTable,
            DesktopRoute.IgnitionTable2,
            DesktopRoute.DwellTable,
            DesktopRoute.TriggerSettings
        )
    )
}

internal fun engineSetupNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionEngineSetup",
        routes = listOf(
            DesktopRoute.EngineSetup,
            DesktopRoute.ConfigsTuning,
            DesktopRoute.EngineConstants,
            DesktopRoute.InjectorConfig,
            DesktopRoute.InputOutputConfig,
            DesktopRoute.SensorsConfig,
            DesktopRoute.SecondarySerial
        )
    )
}

internal fun engineOperationNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionEngineOperation",
        routes = listOf(
            DesktopRoute.EngineOperation,
            DesktopRoute.IdleControl,
            DesktopRoute.ClosedLoopCorrections,
            DesktopRoute.EngineProtection,
            DesktopRoute.RevLimiterConfig
        )
    )
}
