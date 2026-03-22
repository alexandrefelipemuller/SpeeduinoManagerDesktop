package com.speeduino.manager.sync

import com.speeduino.manager.model.Algorithm
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.EngineStroke
import com.speeduino.manager.model.EcuFamily
import com.speeduino.manager.model.InjectorPortType
import com.speeduino.manager.model.InjectorStaging
import com.speeduino.manager.model.MapSampleMethod
import com.speeduino.manager.model.VeTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SessionTableParserTest {

    @Test
    fun `parses speeduino ve table from generic session pages`() {
        val expected = VeTable.createDefault().withLoadType(VeTable.LoadType.TPS)
        val engineConstants = EngineConstants(
            reqFuel = 8.5f,
            algorithm = Algorithm.ALPHA_N,
            squirtsPerCycle = 2,
            injectorStaging = InjectorStaging.ALTERNATING,
            engineStroke = EngineStroke.FOUR_STROKE,
            numberOfCylinders = 4,
            injectorPortType = InjectorPortType.PORT,
            numberOfInjectors = 4,
            stoichiometricRatio = 14.7f,
            mapSampleMethod = MapSampleMethod.CYCLE_AVERAGE,
        )

        val parsed = SessionTableParser.parseVeTable(
            ecuFamily = EcuFamily.SPEEDUINO,
            pages = mapOf(2 to expected.toByteArray()),
            engineConstants = engineConstants,
        )

        assertNotNull(parsed)
        assertEquals(VeTable.LoadType.TPS, parsed.loadType)
        assertEquals(expected.values, parsed.values)
        assertEquals(expected.rpmBins, parsed.rpmBins)
        assertEquals(expected.loadBins, parsed.loadBins)
    }
}
