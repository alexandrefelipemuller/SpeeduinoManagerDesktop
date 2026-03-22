package com.speeduino.manager.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeeduinoLoadAxisTest {

    @Test
    fun `ve table parses alpha n load bins using half scale`() {
        val page = ByteArray(288)
        page[272] = 60
        page[287] = 200.toByte()

        val table = VeTable.fromPageData(page, loadType = VeTable.LoadType.TPS)

        assertEquals(30, table.loadBins.first())
        assertEquals(100, table.loadBins.last())
    }

    @Test
    fun `changing ve table load type rescales existing bins from raw data`() {
        val page = ByteArray(288)
        page[272] = 60
        page[287] = 100.toByte()

        val mapTable = VeTable.fromPageData(page, loadType = VeTable.LoadType.MAP)
        val alphaNTable = mapTable.withLoadType(VeTable.LoadType.TPS)

        assertEquals(120, mapTable.loadBins.first())
        assertEquals(30, alphaNTable.loadBins.first())
        assertEquals(50, alphaNTable.loadBins.last())
    }

    @Test
    fun `engine constants parse separate ignition algorithm from page 1`() {
        val page = ByteArray(128)
        page[26] = 0x11
        page[37] = 0x00

        val constants = EngineConstants.fromPage1(page)

        assertEquals(Algorithm.SPEED_DENSITY, constants.algorithm)
        assertEquals(Algorithm.ALPHA_N, constants.ignitionAlgorithm)
        assertEquals(IgnitionTable.LoadType.TPS, constants.ignitionTableLoadType())
        assertEquals(VeTable.LoadType.MAP, constants.fuelTableLoadType())
    }

    @Test
    fun `afr table stays map on modern speeduino even with alpha n`() {
        val constants = EngineConstants(
            reqFuel = 8.0f,
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

        assertEquals(AfrTable.LoadType.MAP, constants.afrTableLoadType())
        assertEquals(AfrTable.LoadType.TPS, constants.afrTableLoadType(isLegacyFormat = true))
    }
}
