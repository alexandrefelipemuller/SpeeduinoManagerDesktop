package com.speeduino.manager.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.ConnectionStatus
import com.speeduino.manager.desktop.LocalStrings

@Composable
internal fun DisconnectedBanner(connectionState: ConnectionState) {
    AnimatedVisibility(
        visible = !connectionState.isConnected,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val strings = LocalStrings.current
        val background = Color(0xFF7A3626)
        val content = Color.White
        val message = when (connectionState.status) {
            ConnectionStatus.Connected -> strings["status.connected"]
            ConnectionStatus.Disconnected -> strings["status.disconnected"]
            ConnectionStatus.Connecting -> strings["status.connecting"]
            ConnectionStatus.Failed -> strings.format("status.failed", connectionState.detail ?: "")
        }
        Surface(color = background, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
