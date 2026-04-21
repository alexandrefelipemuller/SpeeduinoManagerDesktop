package com.speeduino.manager.desktop.navigation

internal fun configsNavSection(): NavSection {
    return NavSection(
        titleKey = "nav.sectionConfigs",
        route = DesktopRoute.ConfigsTuning,
        activeRoutes = setOf(
            DesktopRoute.ConfigsTuning,
            DesktopRoute.InjectorConfig,
            DesktopRoute.InputOutputConfig,
            DesktopRoute.RevLimiterConfig,
            DesktopRoute.SecondarySerial,
            DesktopRoute.TuningAssistant,
            DesktopRoute.EngineConstants,
            DesktopRoute.TriggerSettings,
            DesktopRoute.IdleControl,
            DesktopRoute.ClosedLoopCorrections,
            DesktopRoute.SensorsConfig,
            DesktopRoute.EngineProtection
        )
    )
}
