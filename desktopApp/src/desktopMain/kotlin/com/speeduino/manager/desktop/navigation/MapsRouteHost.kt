package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.desktop.AfrTableScreenDesktop
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.MapsTablesScreenDesktop
import com.speeduino.manager.desktop.VeTableScreenDesktop

@Composable
internal fun MapsRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    onOpenVeTable: () -> Unit,
    onOpenVeTable2: () -> Unit,
    onOpenAfrTable: () -> Unit,
    onOpenInjectorConfig: () -> Unit,
) {
    when (route) {
        DesktopRoute.Fuel,
        DesktopRoute.MapsTables -> MapsTablesScreenDesktop(
            controller = controller,
            onOpenVeTable = onOpenVeTable,
            onOpenVeTable2 = onOpenVeTable2,
            onOpenAfrTable = onOpenAfrTable,
            onOpenInjectorConfig = onOpenInjectorConfig,
        )
        DesktopRoute.VeTable -> VeTableScreenDesktop(controller, mapIndex = 1)
        DesktopRoute.VeTable2 -> VeTableScreenDesktop(controller, mapIndex = 2)
        DesktopRoute.AfrTable -> AfrTableScreenDesktop(controller)
        else -> Unit
    }
}
