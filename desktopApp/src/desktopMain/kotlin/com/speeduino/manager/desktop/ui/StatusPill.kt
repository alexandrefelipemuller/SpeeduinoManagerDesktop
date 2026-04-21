package com.speeduino.manager.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.ConnectionStatus
import com.speeduino.manager.desktop.LocalStrings

@Composable
internal fun StatusPill(connectionState: ConnectionState) {
    val strings = LocalStrings.current
    val isConnected = connectionState.isConnected
    val background = if (isConnected) Color(0xFFE0F2E9) else Color(0xFFFDE9E4)
    val content = if (isConnected) Color(0xFF1F5F3D) else Color(0xFF7A3626)
    val message = when (connectionState.status) {
        ConnectionStatus.Connected -> strings["status.connected"]
        ConnectionStatus.Disconnected -> strings["status.disconnected"]
        ConnectionStatus.Connecting -> strings["status.connecting"]
        ConnectionStatus.Failed -> strings.format("status.failed", connectionState.detail ?: "")
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, content.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(8.dp)
                    .background(content, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
        }
    }
}
