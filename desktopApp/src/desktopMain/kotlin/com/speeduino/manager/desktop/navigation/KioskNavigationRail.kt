package com.speeduino.manager.desktop.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RailDestinations = listOf(
    DesktopRoute.Home,
    DesktopRoute.Dashboard,
    DesktopRoute.Ecu,
    DesktopRoute.Tools
)

/**
 * Always-visible left-side navigation rail: the kiosk's equivalent of the mobile app's
 * bottom tab bar, rotated to the side since screen height (480px) is the scarce dimension
 * on this display, not width.
 */
@Composable
internal fun KioskNavigationRail(
    currentRoute: DesktopRoute,
    onRouteSelected: (DesktopRoute) -> Unit
) {
    val strings = com.speeduino.manager.desktop.LocalStrings.current
    val selectedTopLevelRoute = selectedNavRoute(currentRoute)

    Surface(
        modifier = Modifier.width(58.dp).fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            RailDestinations.forEach { route ->
                RailButton(
                    route = route,
                    selected = route == selectedTopLevelRoute,
                    label = strings[route.labelKey],
                    onClick = { onRouteSelected(route) }
                )
            }
        }
    }
}

@Composable
private fun RailButton(
    route: DesktopRoute,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = background
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = route.icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
