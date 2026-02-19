package com.speeduino.manager.tuning

import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.VeTable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object TuningAssistantAnalyzer {

    fun analyzeLines(
        logName: String,
        lines: Sequence<String>,
        veTable: VeTable,
        afrTable: AfrTable,
        strategy: TuningStrategy,
        settings: AnalyzerSettings = AnalyzerSettings()
    ): AnalyzerResult {
        val iterator = lines.iterator()
        if (!iterator.hasNext()) {
            return emptyResult(logName, veTable)
        }

        val headerLine = iterator.next()
        val headers = headerLine.split(",").map { it.trim() }
        val columns = resolveColumns(headers)
        val loadType = veTable.loadType

        val hasRpm = columns.rpmIndex != null
        val hasAfr = columns.afrIndex != null
        val hasLoad = if (loadType == VeTable.LoadType.MAP) columns.mapIndex != null else columns.tpsIndex != null
        val hasAfrTarget = afrTable.loadType.name == loadType.name

        val signalStatus = AnalyzerSignalStatus(
            hasRpm = hasRpm,
            hasLoad = hasLoad,
            hasAfr = hasAfr,
            hasAfrTarget = hasAfrTarget
        )

        val rows = veTable.loadBins.size
        val cols = veTable.rpmBins.size
        val accumulators = List(rows) { List(cols) { CellAccumulator() } }

        var firstTimestamp: Long? = null
        var lastTimestamp: Long? = null
        var totalSamples = 0
        var usedSamples = 0
        var minRpm: Int? = null
        var maxRpm: Int? = null
        var minLoad: Int? = null
        var maxLoad: Int? = null

        var previousSample: SamplePoint? = null
        val timestampIsSeconds = columns.timestampHeader?.contains("sec") == true &&
            columns.timestampHeader.contains("ms").not()

        while (iterator.hasNext()) {
            val line = iterator.next()
            if (line.isBlank()) continue
            totalSamples++
            val colsData = line.split(",")
            val timestamp = parseTimestamp(
                colsData.getOrNull(columns.timestampIndex ?: -1),
                timestampIsSeconds
            )
            val rpm = columns.rpmIndex?.let { colsData.getOrNull(it)?.trim()?.toDoubleOrNull() }
            val map = columns.mapIndex?.let { colsData.getOrNull(it)?.trim()?.toDoubleOrNull() }
            val tps = columns.tpsIndex?.let { colsData.getOrNull(it)?.trim()?.toDoubleOrNull() }
            val afr = columns.afrIndex?.let { colsData.getOrNull(it)?.trim()?.toDoubleOrNull() }
            val clt = columns.cltIndex?.let { colsData.getOrNull(it)?.trim()?.toDoubleOrNull() }

            if (!signalStatus.isReady || timestamp == null || rpm == null || afr == null) continue

            val loadValue = if (loadType == VeTable.LoadType.MAP) map else tps
            if (loadValue == null) continue

            if (clt != null && clt < settings.minCoolantC) continue
            if (afr < settings.minAfr || afr > settings.maxAfr) continue
            if (rpm <= 0.0) continue

            if (firstTimestamp == null) firstTimestamp = timestamp
            lastTimestamp = timestamp

            val dtSec = previousSample?.let { (timestamp - it.timestampMs) / 1000.0 } ?: 0.0
            val dRpm = if (previousSample != null && dtSec > 0) (rpm - previousSample.rpm) / dtSec else 0.0
            val dMap = if (previousSample != null && dtSec > 0 && map != null) (map - previousSample.map) / dtSec else 0.0
            val dTps = if (previousSample != null && dtSec > 0 && tps != null) (tps - previousSample.tps) / dtSec else 0.0

            if (abs(dRpm) > settings.maxRpmRate) {
                previousSample = SamplePoint(timestamp, rpm, map ?: 0.0, tps ?: 0.0)
                continue
            }
            if (map != null && abs(dMap) > settings.maxMapRate) {
                previousSample = SamplePoint(timestamp, rpm, map, tps ?: 0.0)
                continue
            }
            if (tps != null && abs(dTps) > settings.maxTpsRate) {
                previousSample = SamplePoint(timestamp, rpm, map ?: 0.0, tps)
                continue
            }

            val rpmIndex = findNearestIndex(veTable.rpmBins, rpm)
            val loadIndex = findNearestIndex(veTable.loadBins, loadValue)
            val afrTarget = lookupAfrTarget(afrTable, rpm, loadValue, settings)
            if (afrTarget == null) {
                previousSample = SamplePoint(timestamp, rpm, map ?: 0.0, tps ?: 0.0)
                continue
            }

            val errorRatio = clamp(
                afr / afrTarget,
                settings.errorRatioMin,
                settings.errorRatioMax
            )
            val weight = computeWeight(dRpm, dMap, dTps, settings)
            val accumulator = accumulators[loadIndex][rpmIndex]
            accumulator.hitCount++
            accumulator.weightSum += weight
            accumulator.errorSum += errorRatio * weight
            accumulator.afrTargetSum += afrTarget * weight
            accumulator.afrMeasuredSum += afr * weight

            usedSamples++
            minRpm = minRpm?.let { min(it, rpm.toInt()) } ?: rpm.toInt()
            maxRpm = maxRpm?.let { max(it, rpm.toInt()) } ?: rpm.toInt()
            minLoad = minLoad?.let { min(it, loadValue.toInt()) } ?: loadValue.toInt()
            maxLoad = maxLoad?.let { max(it, loadValue.toInt()) } ?: loadValue.toInt()

            previousSample = SamplePoint(timestamp, rpm, map ?: 0.0, tps ?: 0.0)
        }

        val clampedDeltas = List(rows) { MutableList<Double?>(cols) { null } }
        val rawDeltas = List(rows) { MutableList<Double?>(cols) { null } }
        val meanTargets = List(rows) { MutableList<Double?>(cols) { null } }
        val meanMeasured = List(rows) { MutableList<Double?>(cols) { null } }
        val hits = List(rows) { MutableList<Int>(cols) { 0 } }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val acc = accumulators[row][col]
                if (acc.hitCount < strategy.minHits || acc.weightSum <= 0.0) continue
                val meanError = acc.errorSum / acc.weightSum
                val rawDelta = meanError - 1.0
                val clamped = clamp(rawDelta, -strategy.maxChangePct, strategy.maxChangePct)
                rawDeltas[row][col] = rawDelta
                clampedDeltas[row][col] = clamped
                meanTargets[row][col] = acc.afrTargetSum / acc.weightSum
                meanMeasured[row][col] = acc.afrMeasuredSum / acc.weightSum
                hits[row][col] = acc.hitCount
            }
        }

        val smoothedDeltas = smoothDeltas(clampedDeltas, strategy.maxChangePct)

        val cellSuggestions = List(rows) { row ->
            List<CellSuggestion?>(cols) cell@{ col ->
                val delta = smoothedDeltas[row][col] ?: return@cell null
                val rawDelta = rawDeltas[row][col] ?: return@cell null
                val meanTarget = meanTargets[row][col] ?: return@cell null
                val meanMeas = meanMeasured[row][col] ?: return@cell null
                val value = veTable.getValue(row, col)
                val suggestedValue = (value * (1.0 + delta)).roundToInt().coerceIn(0, 255)
                CellSuggestion(
                    row = row,
                    col = col,
                    hitCount = hits[row][col],
                    rawDeltaPct = rawDelta,
                    deltaPct = delta,
                    meanAfrTarget = meanTarget,
                    meanAfrMeasured = meanMeas,
                    suggestedValue = suggestedValue
                )
            }
        }

        val suggestedVe = applySuggestions(veTable, cellSuggestions)
        val clusters = buildClusters(cellSuggestions, veTable, settings)

        val durationSeconds = if (firstTimestamp != null && lastTimestamp != null) {
            ((lastTimestamp - firstTimestamp) / 1000f).coerceAtLeast(0f)
        } else {
            0f
        }

        val loadLabel = if (loadType == VeTable.LoadType.MAP) "kPa" else "%"
        val summary = AnalyzerSummary(
            logName = logName,
            durationSeconds = durationSeconds,
            rpmRange = if (minRpm != null && maxRpm != null) IntRange(minRpm, maxRpm) else null,
            loadRange = if (minLoad != null && maxLoad != null) IntRange(minLoad, maxLoad) else null,
            totalSamples = totalSamples,
            usedSamples = usedSamples,
            loadLabel = loadLabel
        )

        return AnalyzerResult(
            summary = summary,
            signalStatus = signalStatus,
            cellSuggestions = cellSuggestions,
            clusters = clusters,
            suggestedVeTable = suggestedVe
        )
    }

    fun applyClustersToVe(
        veTable: VeTable,
        suggestions: List<List<CellSuggestion?>>,
        includedClusterIds: Set<String>,
        clusters: List<SuggestionCluster>
    ): VeTable {
        val includedCells = clusters
            .filter { includedClusterIds.contains(it.id) }
            .flatMap { it.cells }
            .toSet()

        val newValues = veTable.values.mapIndexed { row, rowValues ->
            rowValues.mapIndexed cellValue@{ col, value ->
                val cellRef = CellRef(row, col)
                if (!includedCells.contains(cellRef)) return@cellValue value
                val suggestion = suggestions.getOrNull(row)?.getOrNull(col) ?: return@cellValue value
                suggestion.suggestedValue
            }
        }
        return veTable.copy(values = newValues)
    }

    private fun applySuggestions(veTable: VeTable, suggestions: List<List<CellSuggestion?>>): VeTable {
        val newValues = veTable.values.mapIndexed { row, rowValues ->
            rowValues.mapIndexed { col, value ->
                val suggestion = suggestions.getOrNull(row)?.getOrNull(col)
                suggestion?.suggestedValue ?: value
            }
        }
        return veTable.copy(values = newValues)
    }

    private fun buildClusters(
        suggestions: List<List<CellSuggestion?>>,
        veTable: VeTable,
        settings: AnalyzerSettings
    ): List<SuggestionCluster> {
        val rows = suggestions.size
        val cols = suggestions.firstOrNull()?.size ?: 0
        val visited = Array(rows) { BooleanArray(cols) }
        val clusters = mutableListOf<SuggestionCluster>()
        val tolerance = settings.clusterDeltaTolerancePct

        fun neighbors(row: Int, col: Int): List<CellRef> {
            val list = mutableListOf<CellRef>()
            if (row > 0) list.add(CellRef(row - 1, col))
            if (row < rows - 1) list.add(CellRef(row + 1, col))
            if (col > 0) list.add(CellRef(row, col - 1))
            if (col < cols - 1) list.add(CellRef(row, col + 1))
            return list
        }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val suggestion = suggestions[row][col] ?: continue
                if (visited[row][col]) continue

                val sign = if (suggestion.deltaPct >= 0) 1 else -1
                val seedDelta = suggestion.deltaPct
                val queue = ArrayDeque<CellRef>()
                val cells = mutableListOf<CellRef>()
                queue.add(CellRef(row, col))
                visited[row][col] = true

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    cells.add(current)
                    for (next in neighbors(current.row, current.col)) {
                        if (visited[next.row][next.col]) continue
                        val nextSuggestion = suggestions[next.row][next.col] ?: continue
                        val nextSign = if (nextSuggestion.deltaPct >= 0) 1 else -1
                        if (nextSign != sign) continue
                        if (abs(nextSuggestion.deltaPct - seedDelta) > tolerance) continue
                        visited[next.row][next.col] = true
                        queue.add(next)
                    }
                }

                if (cells.isEmpty()) continue

                val deltas = cells.mapNotNull { suggestions[it.row][it.col]?.deltaPct }
                val avgDelta = deltas.average()
                val avgHits = cells.mapNotNull { suggestions[it.row][it.col]?.hitCount }.average().roundToInt()
                val rpmValues = cells.map { veTable.rpmBins[it.col] }
                val loadValues = cells.map { veTable.loadBins[it.row] }
                val rpmRange = IntRange(rpmValues.minOrNull() ?: 0, rpmValues.maxOrNull() ?: 0)
                val loadRange = IntRange(loadValues.minOrNull() ?: 0, loadValues.maxOrNull() ?: 0)

                clusters.add(
                    SuggestionCluster(
                        id = "cluster_${clusters.size}",
                        cells = cells,
                        avgDeltaPct = avgDelta,
                        avgHits = avgHits,
                        rpmRange = rpmRange,
                        loadRange = loadRange,
                        label = buildRegionLabel(rpmRange, loadRange, veTable.loadType),
                        reason = if (avgDelta >= 0) "Lean average" else "Rich average"
                    )
                )
            }
        }

        return clusters
            .sortedByDescending { abs(it.avgDeltaPct) }
            .take(8)
    }

    private fun buildRegionLabel(
        rpmRange: IntRange,
        loadRange: IntRange,
        loadType: VeTable.LoadType
    ): String {
        val loadUnit = if (loadType == VeTable.LoadType.MAP) "kPa" else "%"
        val loadMid = (loadRange.first + loadRange.last) / 2
        val rpmMid = (rpmRange.first + rpmRange.last) / 2
        val prefix = when {
            loadMid >= 85 -> "High load"
            loadMid <= 40 && rpmMid <= 2000 -> "Cruise"
            rpmMid <= 1200 -> "Idle"
            else -> "Mid load"
        }
        return "$prefix (${rpmRange.first}-${rpmRange.last} rpm / ${loadRange.first}-${loadRange.last} $loadUnit)"
    }

    private fun smoothDeltas(
        clampedDeltas: List<List<Double?>>,
        maxChange: Double
    ): List<List<Double?>> {
        val rows = clampedDeltas.size
        val cols = clampedDeltas.firstOrNull()?.size ?: 0
        return List(rows) { row ->
            List<Double?>(cols) value@{ col ->
                val center = clampedDeltas[row][col] ?: return@value null
                val neighbors = mutableListOf<Double>()
                for (r in max(0, row - 1)..min(rows - 1, row + 1)) {
                    for (c in max(0, col - 1)..min(cols - 1, col + 1)) {
                        val value = clampedDeltas[r][c] ?: continue
                        neighbors.add(value)
                    }
                }
                val avg = if (neighbors.isEmpty()) center else neighbors.average()
                clamp(avg, -maxChange, maxChange)
            }
        }
    }

    private fun lookupAfrTarget(
        afrTable: AfrTable,
        rpm: Double,
        load: Double,
        settings: AnalyzerSettings
    ): Double? {
        val rpmIndex = findNearestIndex(afrTable.rpmBins, rpm)
        val loadIndex = findNearestIndex(afrTable.loadBins, load)
        val raw = afrTable.values.getOrNull(loadIndex)?.getOrNull(rpmIndex) ?: return null
        val afrTarget = raw / 10.0
        if (afrTarget < settings.minAfr || afrTarget > settings.maxAfr) {
            return null
        }
        return afrTarget
    }

    private fun computeWeight(
        dRpm: Double,
        dMap: Double,
        dTps: Double,
        settings: AnalyzerSettings
    ): Double {
        val rpmTerm = abs(dRpm) / settings.maxRpmRate
        val mapTerm = abs(dMap) / settings.maxMapRate
        val tpsTerm = abs(dTps) / settings.maxTpsRate
        val sum = rpmTerm + mapTerm + tpsTerm
        return 1.0 / (1.0 + sum)
    }

    private fun clamp(value: Double, min: Double, max: Double): Double {
        return value.coerceIn(min, max)
    }

    private fun findNearestIndex(bins: List<Int>, value: Double): Int {
        if (bins.isEmpty()) return 0
        var bestIndex = 0
        var bestDistance = abs(bins[0] - value)
        for (i in bins.indices) {
            val distance = abs(bins[i] - value)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
            }
        }
        return bestIndex
    }

    private fun resolveColumns(headers: List<String>): LogColumns {
        val headerLower = headers.map { it.lowercase() }
        val timestampIndex = headerLower.indexOfFirst { it.contains("timestamp") }.takeIf { it >= 0 }
        val timestampHeader = timestampIndex?.let { headerLower[it] }

        val rpmIndex = findHeaderIndex(headerLower, listOf("rpm"))
        val mapIndex = findHeaderIndex(headerLower, listOf("map", "map_kpa", "mapkpa"))
        val tpsIndex = findHeaderIndex(headerLower, listOf("tps"))
        val afrIndex = findHeaderIndex(headerLower, listOf("afr", "o2", "ego"))
        val cltIndex = findHeaderIndex(headerLower, listOf("coolantraw", "coolant", "clt", "coolant_c", "clt_c"))

        return LogColumns(
            timestampIndex = timestampIndex ?: 0,
            timestampHeader = timestampHeader,
            rpmIndex = rpmIndex,
            mapIndex = mapIndex,
            tpsIndex = tpsIndex,
            afrIndex = afrIndex,
            cltIndex = cltIndex
        )
    }

    private fun findHeaderIndex(headersLower: List<String>, candidates: List<String>): Int? {
        candidates.forEach { candidate ->
            val idx = headersLower.indexOfFirst { it == candidate }
            if (idx >= 0) return idx
        }
        return null
    }

    private fun parseTimestamp(value: String?, isSeconds: Boolean): Long? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val asLong = raw.toLongOrNull()
        if (asLong != null) {
            return if (isSeconds) asLong * 1000L else asLong
        }
        val asDouble = raw.toDoubleOrNull() ?: return null
        return if (isSeconds) (asDouble * 1000.0).toLong() else asDouble.toLong()
    }

    private fun emptyResult(
        logName: String,
        veTable: VeTable
    ): AnalyzerResult {
        val rows = veTable.loadBins.size
        val cols = veTable.rpmBins.size
        val summary = AnalyzerSummary(
            logName = logName,
            durationSeconds = 0f,
            rpmRange = null,
            loadRange = null,
            totalSamples = 0,
            usedSamples = 0,
            loadLabel = if (veTable.loadType == VeTable.LoadType.MAP) "kPa" else "%"
        )
        val signalStatus = AnalyzerSignalStatus(
            hasRpm = false,
            hasLoad = false,
            hasAfr = false,
            hasAfrTarget = false
        )
        val emptySuggestions = List(rows) { List<CellSuggestion?>(cols) { null } }
        return AnalyzerResult(
            summary = summary,
            signalStatus = signalStatus,
            cellSuggestions = emptySuggestions,
            clusters = emptyList(),
            suggestedVeTable = veTable
        )
    }

    private data class SamplePoint(
        val timestampMs: Long,
        val rpm: Double,
        val map: Double,
        val tps: Double
    )

    private data class LogColumns(
        val timestampIndex: Int?,
        val timestampHeader: String?,
        val rpmIndex: Int?,
        val mapIndex: Int?,
        val tpsIndex: Int?,
        val afrIndex: Int?,
        val cltIndex: Int?
    )

    private class CellAccumulator {
        var hitCount: Int = 0
        var weightSum: Double = 0.0
        var errorSum: Double = 0.0
        var afrTargetSum: Double = 0.0
        var afrMeasuredSum: Double = 0.0
    }
}
