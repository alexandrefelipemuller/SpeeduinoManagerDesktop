package com.speeduino.manager.desktop.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun NavigationSidebar(
    currentRoute: DesktopRoute,
    onRouteSelected: (DesktopRoute) -> Unit,
    compact: Boolean = false
) {
    val strings = com.speeduino.manager.desktop.LocalStrings.current
    val selectedTopLevelRoute = selectedNavRoute(currentRoute)

    Surface(
        modifier = Modifier.width(if (compact) 92.dp else 296.dp).fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings["app.sidebarTitlePrimary"],
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = strings["app.sidebarTitleSecondary"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    navSections().forEach { section ->
                        if (!compact) {
                            Text(
                                text = strings[section.titleKey].uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        section.routes.forEach { route ->
                            val selected = route == currentRoute ||
                                (route.topLevel && route == selectedTopLevelRoute && currentRoute !in section.routes)
                            val contentColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val background = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                            val borderColor = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = background,
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                val clickableModifier = if (selected) Modifier else Modifier.clickable { onRouteSelected(route) }
                                Row(
                                    modifier = clickableModifier
                                        .fillMaxWidth()
                                        .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = route.icon,
                                        contentDescription = strings[route.labelKey],
                                        tint = contentColor
                                    )
                                    if (!compact) {
                                        Text(
                                            text = strings[route.labelKey],
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(if (compact) 2.dp else 8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}
