package com.speeduino.manager.model.basemap

import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.EngineStroke
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.InjectorPortType
import com.speeduino.manager.model.InjectorStaging
import com.speeduino.manager.model.MapSampleMethod
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.model.Algorithm
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Entrada principal para o gerador determinístico de mapas base.
 */
data class EngineProfile(
    val cylinders: Int,
    val displacementCc: Double,
    val maxRpm: Int,
    val compressionRatio: Double,
    val fuelType: FuelType,
    val injectorFlowLbsPerHour: Double,
    val mapMaxKpa: Int
) {
    val idleRpm: Int = when (fuelType) {
        FuelType.GASOLINE -> 850
        FuelType.ETHANOL -> 900
        FuelType.METHANOL -> 950
    }

    val compressionClass: CompressionClass = CompressionClass.from(compressionRatio)
}

enum class FuelType(val stoichAfr: Double, val densityGPerCc: Double) {
    GASOLINE(stoichAfr = 14.7, densityGPerCc = 0.745),
    ETHANOL(stoichAfr = 9.8, densityGPerCc = 0.789),
    METHANOL(stoichAfr = 6.4, densityGPerCc = 0.792)
}

enum class CompressionClass {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun from(ratio: Double): CompressionClass = when {
            ratio >= 12.0 -> HIGH
            ratio < 10.0 -> LOW
            else -> MEDIUM
        }
    }
}

data class BaseMapAdjustments(
    val richness: Double = 0.0, // -0.15..+0.15 => multiplica VE/ReqFuel
    val advanceOffset: Double = 0.0, // -5..+5 graus
    val highLoadAggressiveness: Double = 0.0 // -1 conservador, +1 agressivo
)

data class GeneratedBaseMap(
    val veTable: VeTable,
    val ignitionTable: IgnitionTable,
    val afrTable: AfrTable,
    val engineConstants: EngineConstants,
    val profile: EngineProfile
)

class BaseMapGenerator(
    private val axisGenerator: AxisGenerator = AxisGenerator()
) {
    fun injectorFlowLbsPerHourToCcPerMin(flowLbsPerHour: Double, fuelType: FuelType = FuelType.GASOLINE): Double {
        // 1 lb/hr gasolina ≈ 10.5 cc/min. Ajuste leve por densidade do combustível.
        val ccPerMinForGasoline = flowLbsPerHour * 10.5
        val densityScale = FuelType.GASOLINE.densityGPerCc / fuelType.densityGPerCc
        return ccPerMinForGasoline * densityScale
    }

    fun calculateRequiredFuel(profile: EngineProfile, stoichAfr: Double, injectorFlowCcPerMin: Double): Double {
        val cylinders = max(profile.cylinders, 1)
        val displacementPerCylinderCc = profile.displacementCc / cylinders
        val airDensityKgPerM3 = 1.184 // ar seco a 25C
        val displacementM3 = displacementPerCylinderCc * 1e-6
        val airMassG = displacementM3 * airDensityKgPerM3 * 1000.0
        val fuelMassG = airMassG / stoichAfr
        val injectorFlowGPerMs = (injectorFlowCcPerMin * profile.fuelType.densityGPerCc) / 60_000.0

        if (injectorFlowGPerMs <= 0) return 6.0

        val rawMs = fuelMassG / injectorFlowGPerMs
        // Valor típico 6-12 ms para motores comuns aspirados
        return rawMs.coerceIn(3.0, 18.0)
    }

    fun generate(
        profile: EngineProfile,
        existingConstants: EngineConstants? = null,
        adjustments: BaseMapAdjustments = BaseMapAdjustments()
    ): GeneratedBaseMap {
        val stoichAfr = profile.fuelType.stoichAfr
        val rpmBins = axisGenerator.generateRpmAxis(profile.idleRpm, profile.maxRpm)
        val mapBins = axisGenerator.generateMapAxis(profile.mapMaxKpa)

        val veValues = generateVeSurface(profile, rpmBins, mapBins, adjustments)
        val afrValues = generateAfrSurface(profile, rpmBins, mapBins, adjustments)
        val ignitionValues = generateIgnitionSurface(profile, rpmBins, mapBins, adjustments)

        val veTable = VeTable(rpmBins = rpmBins, loadBins = mapBins, values = veValues)
        val afrTable = AfrTable(rpmBins = rpmBins, loadBins = mapBins, values = afrValues)
        val ignitionTable = IgnitionTable(rpmBins = rpmBins, loadBins = mapBins, values = ignitionValues)

        val injectorFlowCcPerMin = injectorFlowLbsPerHourToCcPerMin(profile.injectorFlowLbsPerHour, profile.fuelType)
        val requiredFuelMs = calculateRequiredFuel(profile, stoichAfr, injectorFlowCcPerMin) * (1 + adjustments.richness)

        val engineConstants = buildEngineConstants(
            profile = profile,
            stoichAfr = stoichAfr,
            requiredFuelMs = requiredFuelMs,
            existing = existingConstants,
            injectorFlowCcPerMin = injectorFlowCcPerMin
        )

        return GeneratedBaseMap(
            veTable = veTable,
            ignitionTable = ignitionTable,
            afrTable = afrTable,
            engineConstants = engineConstants,
            profile = profile
        )
    }

    private fun generateVeSurface(
        profile: EngineProfile,
        rpmBins: List<Int>,
        mapBins: List<Int>,
        adjustments: BaseMapAdjustments
    ): List<List<Int>> {
        val peakRpm = (profile.maxRpm * 0.55).coerceAtLeast(profile.idleRpm.toDouble())
        val taperRpm = (profile.maxRpm * 0.9)
        val aspirated = profile.mapMaxKpa <= 105
        val baseIdleVe = when (profile.compressionClass) {
            CompressionClass.HIGH -> 48
            CompressionClass.MEDIUM -> 45
            CompressionClass.LOW -> 42
        }
        val peakVe = when {
            aspirated -> 92
            else -> 105
        }
        val boostCeiling = if (aspirated) 105 else 120

        val raw = mapBins.map { map ->
            val loadNorm = ((map - 20).coerceAtLeast(0).toDouble() / (profile.mapMaxKpa - 20).coerceAtLeast(1)).pow(1.2)
            val boostBonus = if (map > 100) {
                val extra = (map - 100).toDouble() / (profile.mapMaxKpa - 100).coerceAtLeast(1)
                (extra * 12.0)
            } else 0.0

            rpmBins.map { rpm ->
                val rise = (rpm.toDouble() / peakRpm).coerceIn(0.0, 1.25)
                val rpmShape = if (rpm <= peakRpm) {
                    rise.pow(0.9)
                } else {
                    val decay = ((rpm - peakRpm) / (taperRpm - peakRpm).coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
                    1.0 - (0.25 * decay)
                }

                val ve = baseIdleVe + (peakVe - baseIdleVe) * rpmShape * loadNorm + boostBonus
                val corrected = ve * (1 + adjustments.richness)
                corrected.coerceIn(25.0, boostCeiling.toDouble())
            }
        }

        val smoothed = TableSmoother.smooth(raw, passes = 2)
        return smoothed.map { row -> row.map { it.roundToInt().coerceIn(0, 255) } }
    }

    private fun generateAfrSurface(
        profile: EngineProfile,
        rpmBins: List<Int>,
        mapBins: List<Int>,
        adjustments: BaseMapAdjustments
    ): List<List<Int>> {
        val stoich = profile.fuelType.stoichAfr
        val wotTarget = when (profile.fuelType) {
            FuelType.GASOLINE -> 12.8
            FuelType.ETHANOL -> 8.3
            FuelType.METHANOL -> 5.0
        }
        val boostTarget = when (profile.fuelType) {
            FuelType.GASOLINE -> 12.0
            FuelType.ETHANOL -> 7.7
            FuelType.METHANOL -> 4.7
        }
        val leanCruise = when (profile.fuelType) {
            FuelType.GASOLINE -> 15.2
            FuelType.ETHANOL -> 10.2
            FuelType.METHANOL -> 6.8
        }

        val enrichedSurface = mapBins.map { map ->
            val mapAfr = when {
                map <= 50 -> leanCruise
                map <= 70 -> stoich
                map <= 100 -> lerp(stoich, wotTarget, (map - 70) / 30.0)
                else -> {
                    val factor = ((map - 100).toDouble() / (profile.mapMaxKpa - 100).coerceAtLeast(1)).coerceIn(0.0, 1.0)
                    lerp(wotTarget, boostTarget, factor)
                }
            }

            rpmBins.map { rpm ->
                val rpmHighFactor = ((rpm - profile.maxRpm * 0.8).toDouble() / (profile.maxRpm * 0.2)).coerceIn(0.0, 1.0)
                val rpmEnrichment = rpmHighFactor * 0.4
                val richnessOffset = -adjustments.richness * 1.2
                val highLoadBias = if (map > 100) adjustments.highLoadAggressiveness * 0.3 else 0.0
                val afr = mapAfr - rpmEnrichment + richnessOffset + highLoadBias
                afr.coerceIn(boostTarget - 0.3, leanCruise + 1.2)
            }
        }

        val smoothed = TableSmoother.smooth(enrichedSurface, passes = 2)
        return smoothed.map { row ->
            row.map {
                (it * 10).roundToInt().coerceIn(100, 200)
            }
        }
    }

    private fun generateIgnitionSurface(
        profile: EngineProfile,
        rpmBins: List<Int>,
        mapBins: List<Int>,
        adjustments: BaseMapAdjustments
    ): List<List<Int>> {
        val baseRetardPerKpa = 0.065
        val fuelRetardAdjust = when (profile.fuelType) {
            FuelType.GASOLINE -> 0.0
            FuelType.ETHANOL -> -0.01
            FuelType.METHANOL -> -0.015
        }
        val compressionAdjust = when (profile.compressionClass) {
            CompressionClass.HIGH -> 0.018
            CompressionClass.LOW -> -0.01
            CompressionClass.MEDIUM -> 0.0
        }
        val highLoadScale = 1 - (adjustments.highLoadAggressiveness * 0.35)
        val retardPerKpa = (baseRetardPerKpa + fuelRetardAdjust + compressionAdjust) * highLoadScale

        val raw = mapBins.map { map ->
            val loadPenalty = when {
                map < 60 -> 0.0
                map < 80 -> 1.5
                map < 100 -> 3.0
                else -> 5.5
            }

            val boostRetard = if (map > 100) {
                (map - 100) * retardPerKpa
            } else 0.0

            val compressionPenalty = when (profile.compressionClass) {
                CompressionClass.HIGH -> if (map >= 80) 3.5 else 0.0
                CompressionClass.LOW -> if (map >= 80) -2.0 else 0.5
                CompressionClass.MEDIUM -> 0.0
            }

            val fuelAdvanceBonus = when (profile.fuelType) {
                FuelType.GASOLINE -> 0.0
                FuelType.ETHANOL -> 1.5
                FuelType.METHANOL -> 2.0
            }

            rpmBins.map { rpm ->
                val base = baseRpmAdvance(rpm, profile.maxRpm, profile.idleRpm)
                val vacuumAdvance = ((100 - map).coerceAtLeast(0) / 80.0) * 10.0

                val advance = base + vacuumAdvance - boostRetard - loadPenalty + fuelAdvanceBonus + compressionPenalty + adjustments.advanceOffset
                advance.coerceIn(0.0, 45.0)
            }
        }

        val smoothed = TableSmoother.smooth(raw, passes = 3)
        return smoothed.map { row ->
            row.map { it.roundToInt().coerceIn(-40, 70) }
        }
    }

    private fun baseRpmAdvance(rpm: Int, maxRpm: Int, idleRpm: Int): Double {
        return when {
            rpm <= idleRpm + 200 -> 12.0
            rpm <= 1500 -> 18.0
            rpm <= 2500 -> 26.0
            rpm <= 3500 -> 32.0
            rpm <= 4500 -> 35.0
            else -> {
                val drop = ((rpm - 4500).toDouble() / (maxRpm - 4500).coerceAtLeast(1)).coerceIn(0.0, 1.0)
                35.0 - drop * 3.0
            }
        }
    }

    private fun buildEngineConstants(
        profile: EngineProfile,
        stoichAfr: Double,
        requiredFuelMs: Double,
        existing: EngineConstants?,
        injectorFlowCcPerMin: Double
    ): EngineConstants {
        val base = existing ?: EngineConstants(
            reqFuel = requiredFuelMs.toFloat(),
            batteryVoltage = 12.0f,
            algorithm = Algorithm.SPEED_DENSITY,
            squirtsPerCycle = 2,
            injectorStaging = InjectorStaging.ALTERNATING,
            engineStroke = EngineStroke.FOUR_STROKE,
            numberOfCylinders = profile.cylinders,
            injectorPortType = InjectorPortType.PORT,
            numberOfInjectors = max(profile.cylinders, 1),
            engineType = com.speeduino.manager.model.EngineType.EVEN_FIRE,
            stoichiometricRatio = stoichAfr.toFloat(),
            injectorLayout = com.speeduino.manager.model.InjectorLayout.SEQUENTIAL,
            mapSampleMethod = MapSampleMethod.CYCLE_AVERAGE,
            mapSwitchPoint = 4000
        )

        val cleanedCylinders = profile.cylinders.coerceIn(1, 12)
        val cleanedInjectors = max(existing?.numberOfInjectors ?: cleanedCylinders, cleanedCylinders)

        return base.copy(
            reqFuel = requiredFuelMs.toFloat().coerceIn(0.1f, 25f),
            numberOfCylinders = cleanedCylinders,
            numberOfInjectors = cleanedInjectors,
            stoichiometricRatio = stoichAfr.toFloat(),
            injectorPortType = base.injectorPortType,
            mapSampleMethod = base.mapSampleMethod,
            squirtsPerCycle = base.squirtsPerCycle.coerceIn(1, 4),
            injectorStaging = base.injectorStaging,
            algorithm = Algorithm.SPEED_DENSITY,
            engineStroke = base.engineStroke
        )
    }

    private fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t.coerceIn(0.0, 1.0)
    }
}

class AxisGenerator {
    fun generateRpmAxis(idleRpm: Int, maxRpm: Int, size: Int = 16): List<Int> {
        val start = idleRpm.coerceAtLeast(600)
        val end = max(maxRpm, start + 500)
        val bins = mutableListOf<Int>()

        for (i in 0 until size) {
            val t = i.toDouble() / (size - 1)
            val curved = t.pow(1.35)
            val rpm = (start + (end - start) * curved).roundToInt()
            bins.add(rpm)
        }

        return enforceMonotonic(bins, minStep = 50, start)
    }

    fun generateMapAxis(mapMaxKpa: Int, size: Int = 16): List<Int> {
        val end = mapMaxKpa.coerceIn(100, 510)
        val start = 20
        val bins = mutableListOf<Int>()

        val lowBinsCount = (size * 0.6).roundToInt().coerceIn(8, size - 3)
        val lowEnd = min(100, end - (size - lowBinsCount))

        for (i in 0 until lowBinsCount) {
            val t = i.toDouble() / (lowBinsCount - 1).coerceAtLeast(1)
            val curved = t.pow(1.6)
            val value = (start + (lowEnd - start) * curved).roundToInt()
            bins.add(value)
        }

        val remaining = size - lowBinsCount
        val highStart = bins.lastOrNull() ?: lowEnd
        for (i in 1..remaining) {
            val t = i.toDouble() / remaining.coerceAtLeast(1)
            val value = (highStart + (end - highStart) * t).roundToInt()
            bins.add(value)
        }

        return enforceMonotonic(bins, minStep = 2, startMin = start, maxEnd = end)
    }

    private fun enforceMonotonic(values: List<Int>, minStep: Int, startMin: Int, maxEnd: Int? = null): List<Int> {
        val fixed = mutableListOf<Int>()
        var last = startMin
        for (value in values) {
            val raw = max(value, last + minStep)
            val next = if (maxEnd != null) min(raw, maxEnd) else raw
            fixed.add(next)
            last = next
        }
        if (maxEnd != null && fixed.isNotEmpty()) {
            fixed[fixed.lastIndex] = maxEnd
            for (i in fixed.lastIndex - 1 downTo 0) {
                if (fixed[i] >= fixed[i + 1]) {
                    fixed[i] = fixed[i + 1] - minStep
                }
            }
        }
        return fixed
    }
}

object TableSmoother {
    fun smooth(values: List<List<Double>>, passes: Int = 1): List<List<Double>> {
        var current = values
        repeat(passes.coerceAtLeast(1)) {
            current = smoothOnce(current)
        }
        return current
    }

    private fun smoothOnce(values: List<List<Double>>): List<List<Double>> {
        val rows = values.size
        val cols = values.firstOrNull()?.size ?: 0
        return List(rows) { r ->
            List(cols) { c ->
                var sum = 0.0
                var count = 0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val rr = r + dr
                        val cc = c + dc
                        if (rr in 0 until rows && cc in 0 until cols) {
                            sum += values[rr][cc]
                            count++
                        }
                    }
                }
                sum / count.coerceAtLeast(1)
            }
        }
    }
}
