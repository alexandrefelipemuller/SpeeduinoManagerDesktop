package com.speeduino.manager.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ui.DropdownField
import com.speeduino.manager.desktop.ui.InfoRow

@Composable
internal fun ConnectionSettingsScreenDesktop(
    controller: DesktopSpeeduinoController,
    onOpenBluetoothConnection: () -> Unit,
    onOpenUsbSerialConnection: () -> Unit
) {
    val strings = LocalStrings.current
    val settings by controller.desktopSettings.collectAsState()
    var host by remember(settings.lastTcpHost) { mutableStateOf(settings.lastTcpHost ?: "127.0.0.1") }
    var port by remember(settings.lastTcpPort) { mutableStateOf(settings.lastTcpPort?.toString() ?: "5555") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings["label.connectionSettings"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings["label.toolsConnectionSettingsDesc"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ConnectionProfileCard(
            title = strings["label.connectionTypeTcp"],
            subtitle = strings["label.toolsConnectionSettingsDesc"],
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(strings["label.host"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit) },
                label = { Text(strings["label.port"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        val parsedPort = port.toIntOrNull()
                        if (!host.isBlank() && parsedPort != null) {
                            controller.saveConnectionProfile(
                                connectionType = ConnectionType.TCP,
                                tcpHost = host,
                                tcpPort = parsedPort
                            )
                        }
                    }
                ) {
                    Text(strings["label.saveTcpProfile"])
                }
                FilledTonalButton(onClick = onOpenBluetoothConnection) {
                    Text(strings["label.bluetooth"])
                }
                FilledTonalButton(onClick = onOpenUsbSerialConnection) {
                    Text(strings["label.usbSerial"])
                }
            }
        }
        ConnectionSettingsSummary(settings)
    }
}

@Composable
internal fun BluetoothConnectionScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    ConnectionEndpointScreenDesktop(
        controller = controller,
        title = strings["label.bluetoothConnectionTitle"],
        subtitle = strings["label.bluetoothConnectionSubtitle"],
        connectionType = ConnectionType.BLUETOOTH
    )
}

@Composable
internal fun UsbSerialConnectionScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    ConnectionEndpointScreenDesktop(
        controller = controller,
        title = strings["label.usbSerialConnectionTitle"],
        subtitle = strings["label.usbSerialConnectionSubtitle"],
        connectionType = ConnectionType.USB
    )
}

@Composable
private fun ConnectionEndpointScreenDesktop(
    controller: DesktopSpeeduinoController,
    title: String,
    subtitle: String,
    connectionType: ConnectionType
) {
    val strings = LocalStrings.current
    val serialPorts by controller.serialPorts.collectAsState()
    val settings by controller.desktopSettings.collectAsState()
    var selectedPort by remember(settings.lastSerialPort, serialPorts) {
        mutableStateOf(settings.lastSerialPort.orEmpty())
    }
    var manualPort by remember(settings.lastSerialPort) { mutableStateOf(settings.lastSerialPort.orEmpty()) }
    var baudRate by remember(settings.lastSerialBaudRate) { mutableStateOf(settings.lastSerialBaudRate?.toString() ?: "115200") }

    LaunchedEffect(connectionType) {
        controller.refreshSerialPorts()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ConnectionProfileCard(
            title = if (connectionType == ConnectionType.BLUETOOTH) {
                strings["label.bluetoothDevice"]
            } else {
                strings["label.usbSerialPort"]
            },
            subtitle = if (connectionType == ConnectionType.BLUETOOTH) {
                strings["label.bluetoothDeviceHelp"]
            } else {
                strings["label.usbSerialPortHelp"]
            }
        ) {
            if (serialPorts.isNotEmpty()) {
                DropdownField(
                    label = strings["label.discoveredPorts"],
                    value = serialPorts.firstOrNull { it.systemPortName == selectedPort }?.displayName
                        ?: selectedPort.ifBlank { strings["label.noSelection"] },
                    options = serialPorts.map { it.displayName }
                ) { label ->
                    selectedPort = serialPorts.firstOrNull { it.displayName == label }?.systemPortName.orEmpty()
                    manualPort = selectedPort
                }
            }
            OutlinedTextField(
                value = manualPort,
                onValueChange = {
                    manualPort = it
                    selectedPort = it
                },
                label = { Text(strings["label.portPathDevice"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = baudRate,
                onValueChange = { baudRate = it.filter(Char::isDigit) },
                label = { Text(strings["label.baudRateLabel"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { controller.refreshSerialPorts() }) {
                    Text(strings["label.refreshPorts"])
                }
                FilledTonalButton(
                    onClick = {
                        val parsedBaudRate = baudRate.toIntOrNull() ?: 115200
                        if (selectedPort.isNotBlank()) {
                            controller.connectSerial(selectedPort, parsedBaudRate, connectionType)
                        }
                    }
                ) {
                    Text(strings["action.connect"])
                }
                FilledTonalButton(
                    onClick = {
                        val parsedBaudRate = baudRate.toIntOrNull() ?: 115200
                        if (selectedPort.isNotBlank()) {
                            controller.saveConnectionProfile(
                                connectionType = connectionType,
                                serialPort = selectedPort,
                                serialBaudRate = parsedBaudRate
                            )
                        }
                    }
                ) {
                    Text(strings["label.saveProfile"])
                }
            }
        }
        ConnectionSettingsSummary(settings)
    }
}

@Composable
private fun ConnectionProfileCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun ConnectionSettingsSummary(settings: DesktopSettingsState) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings["label.savedProfile"],
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            InfoRow(strings["label.autoConnectOnStart"], if (settings.autoConnectOnStart) strings["label.enabled"] else strings["label.disabled"])
            InfoRow(
                strings["label.lastConnection"],
                when (settings.lastConnectionType) {
                    ConnectionType.TCP -> strings.format("label.connectionProfileTcp", settings.lastTcpHost ?: "-", settings.lastTcpPort ?: "-")
                    ConnectionType.USB,
                    ConnectionType.BLUETOOTH -> strings.format("label.connectionProfileSerial", settings.lastConnectionType.name, settings.lastSerialPort ?: "-", settings.lastSerialBaudRate ?: "-")
                    null -> strings["label.noSavedConnection"]
                }
            )
            InfoRow(strings["label.unitSystem"], settings.unitSystem.storageValue)
            InfoRow(strings["label.shiftLightRpm"], settings.shiftLightRpm.toString())
        }
    }
}
