package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.InstitutionalScreenDesktop
import com.speeduino.manager.desktop.feature.app.DashboardScreen
import com.speeduino.manager.desktop.feature.app.SettingsScreen

@Composable
internal fun AppRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    liveData: SpeeduinoLiveData?,
    onOpenSettings: () -> Unit
) {
    when (route) {
        DesktopRoute.Settings -> SettingsScreen(controller)
        DesktopRoute.Institutional -> InstitutionalScreenDesktop()
        DesktopRoute.Dashboard -> DashboardScreen(liveData, onOpenSettings = onOpenSettings)
        else -> Unit
    }
}
