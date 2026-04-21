package com.speeduino.manager.desktop.feature.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.ConnectionType
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.SerialPortInfo
import com.speeduino.manager.desktop.ui.DropdownField
import com.speeduino.manager.desktop.ui.InfoRow
import com.speeduino.manager.desktop.ui.StatusPill

@Composable
internal fun DiagnosticScreen(
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
    onOpenConnectionSettings: () -> Unit = {},
    onOpenBluetoothConnection: () -> Unit = {},
    onOpenUsbSerialConnection: () -> Unit = {},
    onOpenLogsEcuTools: () -> Unit = {},
    onOpenInstitutional: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val firmwareInfo by controller.firmwareInfo.collectAsState()
    val productString by controller.productString.collectAsState()
    val connectionInfo by controller.connectionInfo.collectAsState()
    val lastError by controller.lastError.collectAsState()
    val activeIniDefinition by controller.activeIniDefinition.collectAsState()
    val readOnlySafeMode by controller.readOnlySafeMode.collectAsState()
    val appVersion = com.speeduino.manager.desktop.APP_VERSION
    val serialPorts by controller.serialPorts.collectAsState()

    androidx.compose.runtime.LaunchedEffect(connectionType) {
        if (connectionType != ConnectionType.TCP) {
            controller.refreshSerialPorts()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConnectionCard(
            host = host,
            port = port,
            portIsValid = portIsValid,
            connectionType = connectionType,
            serialPort = serialPort,
            baudRate = baudRate,
            serialPorts = serialPorts,
            connectionState = connectionState,
            onHostChange = onHostChange,
            onPortChange = onPortChange,
            onConnectionTypeChange = onConnectionTypeChange,
            onSerialPortChange = onSerialPortChange,
            onBaudRateChange = onBaudRateChange,
            onToggleConnection = onToggleConnection
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Connection shortcuts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onOpenConnectionSettings) {
                        Text("Connection Settings")
                    }
                    FilledTonalButton(onClick = onOpenBluetoothConnection) {
                        Text("Bluetooth")
                    }
                    FilledTonalButton(onClick = onOpenUsbSerialConnection) {
                        Text("USB Serial")
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "More tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onOpenLogsEcuTools) {
                        Text("Logs & ECU Tools")
                    }
                    FilledTonalButton(onClick = onOpenInstitutional) {
                        Text("Institutional")
                    }
                }
            }
        }
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
                    text = strings["label.diagnostics"],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                InfoRow(strings["label.firmware"], firmwareInfo?.signature ?: strings["label.noData"])
                InfoRow(strings["label.product"], productString ?: strings["label.noData"])
                InfoRow(strings["label.connection"], connectionInfo ?: strings["label.noData"])
                InfoRow("INI", activeIniDefinition?.sourceName ?: strings["label.noData"])
                InfoRow("Safe mode", if (readOnlySafeMode) "Read-only" else "Off")
                InfoRow(strings["label.appVersion"], appVersion)
                if (!lastError.isNullOrBlank()) {
                    val errorText = lastError ?: ""
                    Text(
                        text = strings.format("label.errorWithValue", errorText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9A3B2E)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ConnectionCard(
    host: String,
    port: String,
    portIsValid: Boolean,
    connectionType: ConnectionType,
    serialPort: String,
    baudRate: String,
    serialPorts: List<SerialPortInfo>,
    connectionState: ConnectionState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectionTypeChange: (ConnectionType) -> Unit,
    onSerialPortChange: (String) -> Unit,
    onBaudRateChange: (String) -> Unit,
    onToggleConnection: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings["label.connectionScreenTitle"],
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            DropdownField(
                label = strings["label.connectionType"],
                value = connectionType.label(strings),
                options = ConnectionType.values().map { it.label(strings) }
            ) { label ->
                onConnectionTypeChange(ConnectionType.values().first { it.label(strings) == label })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (connectionType) {
                    ConnectionType.TCP -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            label = { Text(strings["label.host"]) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = onPortChange,
                            label = { Text(strings["label.port"]) },
                            singleLine = true,
                            modifier = Modifier.width(140.dp),
                            isError = port.isNotEmpty() && !portIsValid,
                            supportingText = {
                                if (port.isNotEmpty() && !portIsValid) {
                                    Text(strings["label.portInvalid"])
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    ConnectionType.USB,
                    ConnectionType.BLUETOOTH -> {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DropdownField(
                                label = strings["label.serialPort"],
                                value = serialPorts.firstOrNull { it.systemPortName == serialPort }?.displayName
                                    ?: if (serialPort.isBlank()) strings["label.none"] else serialPort,
                                options = serialPorts.map { it.displayName }
                            ) { label ->
                                val selected = serialPorts.firstOrNull { it.displayName == label }
                                if (selected != null) onSerialPortChange(selected.systemPortName)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = baudRate,
                            onValueChange = onBaudRateChange,
                            label = { Text(strings["label.baud"]) },
                            singleLine = true,
                            modifier = Modifier.width(140.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                FilledTonalButton(
                    onClick = onToggleConnection,
                    enabled = connectionState.isConnected || when (connectionType) {
                        ConnectionType.TCP -> portIsValid
                        ConnectionType.USB,
                        ConnectionType.BLUETOOTH -> serialPort.isNotBlank()
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        if (connectionState.isConnected) {
                            strings["action.disconnect"]
                        } else {
                            strings["action.connect"]
                        }
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings["status.label"],
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(connectionState = connectionState)
            }
        }
    }
}
