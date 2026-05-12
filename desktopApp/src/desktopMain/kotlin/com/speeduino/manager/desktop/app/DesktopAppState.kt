package com.speeduino.manager.desktop.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.speeduino.manager.desktop.ConnectionType
import com.speeduino.manager.desktop.DesktopSettingsState
import com.speeduino.manager.desktop.navigation.DesktopRoute

@Stable
internal class DesktopAppState(
    initialRoute: DesktopRoute,
    initialHost: String,
    initialPort: String,
    initialConnectionType: ConnectionType,
    initialSerialPort: String,
    initialBaudRate: String
) {
    var currentRoute by mutableStateOf(initialRoute)
    var host by mutableStateOf(initialHost)
    var port by mutableStateOf(initialPort)
    var connectionType by mutableStateOf(initialConnectionType)
    var serialPort by mutableStateOf(initialSerialPort)
    var baudRate by mutableStateOf(initialBaudRate)
}

@Composable
internal fun rememberDesktopAppState(desktopSettings: DesktopSettingsState): DesktopAppState {
    return remember {
        DesktopAppState(
            initialRoute = DesktopRoute.Home,
            initialHost = desktopSettings.lastTcpHost ?: "127.0.0.1",
            initialPort = desktopSettings.lastTcpPort?.toString() ?: "5555",
            initialConnectionType = desktopSettings.lastConnectionType ?: ConnectionType.TCP,
            initialSerialPort = desktopSettings.lastSerialPort.orEmpty(),
            initialBaudRate = desktopSettings.lastSerialBaudRate?.toString() ?: "115200"
        )
    }
}
