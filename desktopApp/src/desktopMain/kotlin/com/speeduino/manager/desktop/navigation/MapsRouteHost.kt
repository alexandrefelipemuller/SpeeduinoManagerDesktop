package com.speeduino.manager.desktop.navigation

import androidx.compose.runtime.Composable
import com.speeduino.manager.desktop.AfrTableScreenDesktop
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.IgnitionTableScreenDesktop
import com.speeduino.manager.desktop.MapsTablesScreenDesktop
import com.speeduino.manager.desktop.VeTableScreenDesktop
import com.speeduino.manager.desktop.feature.maps.BaseMapWizardScreenDesktop

@Composable
internal fun MapsRouteHost(
    route: DesktopRoute,
    controller: DesktopSpeeduinoController,
    onOpenVeTable: () -> Unit,
    onOpenVeTable2: () -> Unit,
    onOpenIgnitionTable: () -> Unit,
    onOpenIgnitionTable2: () -> Unit,
    onOpenAfrTable: () -> Unit,
    onOpenBaseMapWizard: () -> Unit
) {
    when (route) {
        DesktopRoute.MapsTables -> MapsTablesScreenDesktop(
            onOpenVeTable = onOpenVeTable,
            onOpenVeTable2 = onOpenVeTable2,
            onOpenIgnitionTable = onOpenIgnitionTable,
            onOpenIgnitionTable2 = onOpenIgnitionTable2,
            onOpenAfrTable = onOpenAfrTable,
            onOpenBaseMapWizard = onOpenBaseMapWizard
        )
        DesktopRoute.VeTable -> VeTableScreenDesktop(controller, mapIndex = 1)
        DesktopRoute.VeTable2 -> VeTableScreenDesktop(controller, mapIndex = 2)
        DesktopRoute.IgnitionTable -> IgnitionTableScreenDesktop(controller, mapIndex = 1)
        DesktopRoute.IgnitionTable2 -> IgnitionTableScreenDesktop(controller, mapIndex = 2)
        DesktopRoute.AfrTable -> AfrTableScreenDesktop(controller)
        DesktopRoute.BaseMapWizard -> BaseMapWizardScreenDesktop(controller)
        else -> Unit
    }
}
