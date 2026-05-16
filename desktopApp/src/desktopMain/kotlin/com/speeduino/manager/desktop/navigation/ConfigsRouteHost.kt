package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.ConfigsTuningScreenDesktop
import com.speeduino.manager.desktop.ClosedLoopCorrectionsScreenDesktop
import com.speeduino.manager.desktop.EngineOperationScreenDesktop
import com.speeduino.manager.desktop.IgnitionScreenDesktop
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
    onOpenInputOutputConfig: () -> Unit,
    onOpenSensorCalibration: () -> Unit,
    onOpenInjectorConfig: () -> Unit,
    onOpenSecondarySerial: () -> Unit,
    onOpenIgnitionTable: () -> Unit,
    onOpenIgnitionTable2: () -> Unit,
    onOpenDwellTable: () -> Unit,
    onOpenTriggerSettings: () -> Unit,
    onOpenIdleControl: () -> Unit,
    onOpenEngineProtection: () -> Unit,
    onOpenClosedLoopCorrections: () -> Unit,
    onOpenRevLimiterConfig: () -> Unit,
    onOpenBeforeAfter: () -> Unit
) {
    when (route) {
        DesktopRoute.Ignition -> IgnitionScreenDesktop(
            onOpenIgnitionTable = onOpenIgnitionTable,
            onOpenIgnitionTable2 = onOpenIgnitionTable2,
            onOpenDwellTable = onOpenDwellTable,
            onOpenTriggerSettings = onOpenTriggerSettings
        )
        DesktopRoute.EngineSetup,
        DesktopRoute.ConfigsTuning -> ConfigsTuningScreenDesktop(
            onOpenEngineConstants = onOpenEngineConstants,
            onOpenInputOutput = onOpenInputOutputConfig,
            onOpenSensorCalibration = onOpenSensorCalibration,
            onOpenInjectorConfig = onOpenInjectorConfig,
            onOpenSecondarySerial = onOpenSecondarySerial
        )
        DesktopRoute.EngineOperation -> EngineOperationScreenDesktop(
            onOpenIdleControl = onOpenIdleControl,
            onOpenClosedLoopCorrections = onOpenClosedLoopCorrections,
            onOpenEngineProtection = onOpenEngineProtection,
            onOpenRevLimiter = onOpenRevLimiterConfig
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
