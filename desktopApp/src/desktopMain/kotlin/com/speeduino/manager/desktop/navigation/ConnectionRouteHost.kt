package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.desktop.BluetoothConnectionScreenDesktop
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.ConnectionType
import com.speeduino.manager.desktop.ConnectionSettingsScreenDesktop
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.UsbSerialConnectionScreenDesktop
import com.speeduino.manager.desktop.feature.connection.DiagnosticScreen

@Composable
internal fun ConnectionRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    connectionState: ConnectionState,
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
    onOpenConnectionSettings: () -> Unit,
    onOpenBluetoothConnection: () -> Unit,
    onOpenUsbSerialConnection: () -> Unit,
    onOpenLogsEcuTools: () -> Unit,
    onOpenInstitutional: () -> Unit
) {
    when (route) {
        DesktopRoute.Connection -> DiagnosticScreen(
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
        DesktopRoute.ConnectionSettings -> ConnectionSettingsScreenDesktop(
            controller = controller,
            onOpenBluetoothConnection = onOpenBluetoothConnection,
            onOpenUsbSerialConnection = onOpenUsbSerialConnection
        )
        DesktopRoute.BluetoothConnection -> BluetoothConnectionScreenDesktop(controller)
        DesktopRoute.UsbSerialConnection -> UsbSerialConnectionScreenDesktop(controller)
        else -> Unit
    }
}
