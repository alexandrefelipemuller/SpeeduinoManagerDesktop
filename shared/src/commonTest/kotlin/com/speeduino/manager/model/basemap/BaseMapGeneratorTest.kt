package com.speeduino.manager.model.basemap

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaseMapGeneratorTest {
    private val generator = BaseMapGenerator()

    @Test
    fun injectorFlowConversionMatchesExpected() {
        val ccPerMin = generator.injectorFlowLbsPerHourToCcPerMin(40.0, FuelType.GASOLINE)
        assertEquals(420.0, ccPerMin, 2.0)
    }

    @Test
    fun requiredFuelFallsInReasonableRange() {
        val profile = EngineProfile(
            cylinders = 4,
            displacementCc = 2000.0,
            maxRpm = 6500,
            compressionRatio = 10.5,
            fuelType = FuelType.GASOLINE,
            injectorFlowLbsPerHour = 40.0,
            mapMaxKpa = 100
        )

        val stoich = profile.fuelType.stoichAfr
        val injectorFlowCcPerMin = generator.injectorFlowLbsPerHourToCcPerMin(profile.injectorFlowLbsPerHour, profile.fuelType)
        val reqFuel = generator.calculateRequiredFuel(profile, stoich, injectorFlowCcPerMin)

        assertTrue(reqFuel in 3.0..18.0)
    }

    @Test
    fun axisGenerationStartsAtIdleAndEndsAtMax() {
        val rpmBins = generator.generate(
            profile = EngineProfile(
                cylinders = 4,
                displacementCc = 1800.0,
                maxRpm = 7000,
                compressionRatio = 10.0,
                fuelType = FuelType.ETHANOL,
                injectorFlowLbsPerHour = 36.0,
                mapMaxKpa = 250
            )
        ).veTable.rpmBins

        assertTrue(rpmBins.first() >= 850)
        assertTrue(rpmBins.last() >= 7000)
        assertTrue(rpmBins.zipWithNext().all { it.second > it.first })

        val mapBins = generator.generate(
            profile = EngineProfile(
                cylinders = 4,
                displacementCc = 1800.0,
                maxRpm = 6500,
                compressionRatio = 10.0,
                fuelType = FuelType.GASOLINE,
                injectorFlowLbsPerHour = 36.0,
                mapMaxKpa = 220
            )
        ).veTable.loadBins
        assertEquals(20, mapBins.first())
        assertEquals(220, mapBins.last())
        assertTrue(mapBins.zipWithNext().all { it.second > it.first })
    }

    @Test
    fun ignitionTableRespectsTypicalRanges() {
        val profile = EngineProfile(
            cylinders = 4,
            displacementCc = 2000.0,
            maxRpm = 6800,
            compressionRatio = 11.0,
            fuelType = FuelType.GASOLINE,
            injectorFlowLbsPerHour = 40.0,
            mapMaxKpa = 200
        )

        val generated = generator.generate(profile)
        val table = generated.ignitionTable

        val idleCol = closestIndex(table.rpmBins, profile.idleRpm + 150)
        val idleRow = closestIndex(table.loadBins, 40)
        val idleValue = table.values[idleRow][idleCol]
        assertTrue(idleValue in 10..18)

        val cruiseRow = closestIndex(table.loadBins, 55)
        val cruiseCol = closestIndex(table.rpmBins, 2600)
        val cruiseValue = table.values[cruiseRow][cruiseCol]
        assertTrue(cruiseValue in 28..40)

        val wotRow = closestIndex(table.loadBins, 100)
        val wotCol = closestIndex(table.rpmBins, 4200)
        val wotValue = table.values[wotRow][wotCol]
        assertTrue(wotValue in 18..32)
    }

    @Test
    fun afrTableLeansOnLightLoadAndRichensOnBoost() {
        val profile = EngineProfile(
            cylinders = 4,
            displacementCc = 2000.0,
            maxRpm = 6800,
            compressionRatio = 9.8,
            fuelType = FuelType.ETHANOL,
            injectorFlowLbsPerHour = 44.0,
            mapMaxKpa = 220
        )
        val afrTable = generator.generate(profile).afrTable

        val lightRow = closestIndex(afrTable.loadBins, 35)
        val lightCol = closestIndex(afrTable.rpmBins, profile.idleRpm + 400)
        val lightAfr = afrTable.values[lightRow][lightCol] / 10.0

        val boostRow = closestIndex(afrTable.loadBins, 180)
        val boostCol = closestIndex(afrTable.rpmBins, 5000)
        val boostAfr = afrTable.values[boostRow][boostCol] / 10.0

        assertTrue(lightAfr > boostAfr)
        assertTrue(lightAfr >= profile.fuelType.stoichAfr)
    }

    private fun closestIndex(values: List<Int>, target: Int): Int {
        return values.withIndex().minByOrNull { (_, value) -> abs(value - target) }?.index ?: 0
    }
}
