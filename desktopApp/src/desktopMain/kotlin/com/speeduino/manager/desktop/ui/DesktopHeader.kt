package com.speeduino.manager.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.ConnectionState
import com.speeduino.manager.desktop.LocalStrings

/** Single compact row: back arrow (if any) + screen title + status pill. No app-name repetition. */
@Composable
internal fun HeaderBar(
    title: String,
    connectionState: ConnectionState,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    val strings = LocalStrings.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (onBackClick != null) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings["action.back"],
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        StatusPill(connectionState = connectionState)
    }
}
