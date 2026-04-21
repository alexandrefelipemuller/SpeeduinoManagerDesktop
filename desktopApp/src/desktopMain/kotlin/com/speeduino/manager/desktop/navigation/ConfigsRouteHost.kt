package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.ConfigsTuningScreenDesktop
import com.speeduino.manager.desktop.ClosedLoopCorrectionsScreenDesktop
import com.speeduino.manager.desktop.IdleControlScreenDesktop
import com.speeduino.manager.desktop.InputOutputConfigScreenDesktop
import com.speeduino.manager.desktop.InjectorConfigScreenDesktop
import com.speeduino.manager.desktop.RevLimiterConfigScreenDesktop
import com.speeduino.manager.desktop.SecondarySerialScreenDesktop
import com.speeduino.manager.desktop.TuningAssistantScreenDesktop
import com.speeduino.manager.desktop.feature.configs.EngineConstantsScreenDesktop
import com.speeduino.manager.desktop.feature.configs.EngineProtectionScreenDesktop
import com.speeduino.manager.desktop.feature.configs.SensorsConfigScreenDesktop
import com.speeduino.manager.desktop.feature.configs.TriggerSettingsScreenDesktop

@Composable
internal fun ConfigsRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    onOpenEngineConstants: () -> Unit,
    onOpenTriggerSettings: () -> Unit,
    onOpenIdleControl: () -> Unit,
    onOpenInputOutputConfig: () -> Unit,
    onOpenSensorCalibration: () -> Unit,
    onOpenEngineProtection: () -> Unit,
    onOpenClosedLoopCorrections: () -> Unit,
    onOpenInjectorConfig: () -> Unit,
    onOpenRevLimiterConfig: () -> Unit,
    onOpenSecondarySerial: () -> Unit,
    onOpenTuningAssistant: () -> Unit,
    onOpenBeforeAfter: () -> Unit
) {
    when (route) {
        DesktopRoute.ConfigsTuning -> ConfigsTuningScreenDesktop(
            onOpenEngineConstants = onOpenEngineConstants,
            onOpenTriggerSettings = onOpenTriggerSettings,
            onOpenIdleControl = onOpenIdleControl,
            onOpenInputOutput = onOpenInputOutputConfig,
            onOpenSensorCalibration = onOpenSensorCalibration,
            onOpenEngineProtection = onOpenEngineProtection,
            onOpenClosedLoopCorrections = onOpenClosedLoopCorrections,
            onOpenInjectorConfig = onOpenInjectorConfig,
            onOpenRevLimiter = onOpenRevLimiterConfig,
            onOpenSecondarySerial = onOpenSecondarySerial,
            onOpenTuningAssistant = onOpenTuningAssistant
        )
        DesktopRoute.TuningAssistant -> TuningAssistantScreenDesktop(
            controller = controller,
            onOpenBeforeAfter = onOpenBeforeAfter
        )
        DesktopRoute.InjectorConfig -> InjectorConfigScreenDesktop(controller)
        DesktopRoute.InputOutputConfig -> InputOutputConfigScreenDesktop(
            controller = controller,
            onOpenSecondarySerial = onOpenSecondarySerial
        )
        DesktopRoute.RevLimiterConfig -> RevLimiterConfigScreenDesktop()
        DesktopRoute.SecondarySerial -> SecondarySerialScreenDesktop(controller)
        DesktopRoute.EngineConstants -> EngineConstantsScreenDesktop(controller)
        DesktopRoute.TriggerSettings -> TriggerSettingsScreenDesktop(controller)
        DesktopRoute.IdleControl -> IdleControlScreenDesktop(controller)
        DesktopRoute.ClosedLoopCorrections -> ClosedLoopCorrectionsScreenDesktop(controller)
        DesktopRoute.SensorsConfig -> SensorsConfigScreenDesktop()
        DesktopRoute.EngineProtection -> EngineProtectionScreenDesktop()
        else -> Unit
    }
}
