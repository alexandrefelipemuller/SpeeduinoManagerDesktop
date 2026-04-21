package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.ConnectionType
import com.speeduino.manager.desktop.DesktopSpeeduinoController

@Composable
internal fun ScreenHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    connectionState: ConnectionState,
    liveData: SpeeduinoLiveData?,
    host: String,
    port: String,
    portIsValid: Boolean,
    connectionType: ConnectionType,
    serialPort: String,
    baudRate: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectionTypeChange: (ConnectionType) -> Unit,
    onSerialPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit,
    onToggleConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInstitutional: () -> Unit,
    onOpenConnectionSettings: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenBluetoothConnection: () -> Unit,
    onOpenUsbSerialConnection: () -> Unit,
    onOpenVeTable: () -> Unit,
    onOpenVeTable2: () -> Unit,
    onOpenIgnitionTable: () -> Unit,
    onOpenIgnitionTable2: () -> Unit,
    onOpenAfrTable: () -> Unit,
    onOpenBaseMapWizard: () -> Unit,
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
    onOpenLogsEcuTools: () -> Unit,
    onOpenLogViewer: () -> Unit,
    onOpenRealTimeMonitor: () -> Unit,
    onOpenBeforeAfter: () -> Unit
) {
    when (route) {
        DesktopRoute.Settings,
        DesktopRoute.Institutional,
        DesktopRoute.Dashboard -> AppRouteHost(
            route = route,
            controller = controller,
            liveData = liveData,
            onOpenSettings = onOpenSettings
        )
        DesktopRoute.Connection,
        DesktopRoute.ConnectionSettings,
        DesktopRoute.BluetoothConnection,
        DesktopRoute.UsbSerialConnection -> ConnectionRouteHost(
            route = route,
            controller = controller,
            connectionState = connectionState,
            host = host,
            port = port,
            portIsValid = portIsValid,
            connectionType = connectionType,
            serialPort = serialPort,
            baudRate = baudRate,
            onHostChange = onHostChange,
            onPortChange = onPortChange,
            onConnectionTypeChange = onConnectionTypeChange,
            onSerialPortChange = onSerialPortChange,
            onBaudRateChange = onBaudRateChange,
            onToggleConnection = onToggleConnection,
            onOpenConnectionSettings = onOpenConnectionSettings,
            onOpenBluetoothConnection = onOpenBluetoothConnection,
            onOpenUsbSerialConnection = onOpenUsbSerialConnection,
            onOpenLogsEcuTools = onOpenLogsEcuTools,
            onOpenInstitutional = onOpenInstitutional
        )
        DesktopRoute.MapsTables,
        DesktopRoute.VeTable,
        DesktopRoute.VeTable2,
        DesktopRoute.IgnitionTable,
        DesktopRoute.IgnitionTable2,
        DesktopRoute.AfrTable,
        DesktopRoute.BaseMapWizard -> MapsRouteHost(
            route = route,
            controller = controller,
            onOpenVeTable = onOpenVeTable,
            onOpenVeTable2 = onOpenVeTable2,
            onOpenIgnitionTable = onOpenIgnitionTable,
            onOpenIgnitionTable2 = onOpenIgnitionTable2,
            onOpenAfrTable = onOpenAfrTable,
            onOpenBaseMapWizard = onOpenBaseMapWizard
        )
        DesktopRoute.ConfigsTuning,
        DesktopRoute.TuningAssistant,
        DesktopRoute.InjectorConfig,
        DesktopRoute.InputOutputConfig,
        DesktopRoute.RevLimiterConfig,
        DesktopRoute.SecondarySerial,
        DesktopRoute.EngineConstants,
        DesktopRoute.TriggerSettings,
        DesktopRoute.IdleControl,
        DesktopRoute.ClosedLoopCorrections,
        DesktopRoute.SensorsConfig,
        DesktopRoute.EngineProtection -> ConfigsRouteHost(
            route = route,
            controller = controller,
            onOpenEngineConstants = onOpenEngineConstants,
            onOpenTriggerSettings = onOpenTriggerSettings,
            onOpenIdleControl = onOpenIdleControl,
            onOpenInputOutputConfig = onOpenInputOutputConfig,
            onOpenSensorCalibration = onOpenSensorCalibration,
            onOpenEngineProtection = onOpenEngineProtection,
            onOpenClosedLoopCorrections = onOpenClosedLoopCorrections,
            onOpenInjectorConfig = onOpenInjectorConfig,
            onOpenRevLimiterConfig = onOpenRevLimiterConfig,
            onOpenSecondarySerial = onOpenSecondarySerial,
            onOpenTuningAssistant = onOpenTuningAssistant,
            onOpenBeforeAfter = onOpenBeforeAfter
        )
        DesktopRoute.LogsEcuTools,
        DesktopRoute.LogViewer,
        DesktopRoute.RealTimeMonitor,
        DesktopRoute.LogAnalyzer,
        DesktopRoute.BeforeAfter -> LogsRouteHost(
            route = route,
            controller = controller,
            liveData = liveData,
            onOpenConnection = onOpenConnection,
            onOpenLogViewer = onOpenLogViewer,
            onOpenRealTimeMonitor = onOpenRealTimeMonitor,
            onOpenBeforeAfter = onOpenBeforeAfter
        )
    }
}
