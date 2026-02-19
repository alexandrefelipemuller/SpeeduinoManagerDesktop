package com.speeduino.manager.compare

import com.speeduino.manager.model.VeTable
import java.io.File
import kotlin.math.abs

enum class LogHeatCellState {
    IMPROVED,
    WORSE,
    NOT_ENOUGH,
    UNCHANGED,
}

data class LogCellComparison(
    val row: Int,
    val col: Int,
    val beforeAvgAbsError: Double?,
    val afterAvgAbsError: Double?,
    val beforeSamples: Int,
    val afterSamples: Int,
    val state: LogHeatCellState,
)

data class LogSummaryMetrics(
    val avgAbsError: Double,
    val leanTimeRatio: Double,
    val richTimeRatio: Double,
    val coverageCells: Int,
    val totalCells: Int,
    val validSamples: Int,
)

data class LogCompareResult(
    val loadLabel: String,
    val rpmBins: List<Int>,
    val loadBins: List<Int>,
    val beforeSummary: LogSummaryMetrics,
    val afterSummary: LogSummaryMetrics,
    val cells: List<List<LogCellComparison>>,
)

class LogCompareException(
    val reason: LogCompareReason,
) : IllegalArgumentException(reason.name)

enum class LogCompareReason {
    EMPTY_LOG,
    MISSING_RPM,
    MISSING_AFR_MEASURED,
    MISSING_AFR_TARGET,
    MISSING_LOAD_CHANNEL,
    NO_VALID_SAMPLES,
}

class BeforeAfterLogComparator {
    fun compareLogs(
        beforePath: String,
        afterPath: String,
        rpmBins: List<Int>,
        loadBins: List<Int>,
        preferredLoadType: VeTable.LoadType,
        minHitsPerCell: Int = DEFAULT_MIN_HITS,
        afrTolerance: Double = DEFAULT_AFR_TOLERANCE,
        minAfr: Double = PLAUSIBLE_AFR_MIN,
        maxAfr: Double = PLAUSIBLE_AFR_MAX,
    ): LogCompareResult {
        require(rpmBins.isNotEmpty() && loadBins.isNotEmpty()) { "No axes available" }

        val beforeLog = parseComparableLog(beforePath, preferredLoadType, afrTolerance, minAfr, maxAfr)
        val afterLog = parseComparableLog(afterPath, preferredLoadType, afrTolerance, minAfr, maxAfr)
        if (beforeLog.samples.isEmpty() || afterLog.samples.isEmpty()) {
            throw LogCompareException(LogCompareReason.NO_VALID_SAMPLES)
        }

        val rows = loadBins.size
        val cols = rpmBins.size
        val beforeBuckets = List(rows) { List(cols) { CellBucket() } }
        val afterBuckets = List(rows) { List(cols) { CellBucket() } }

        fillBuckets(beforeLog.samples, beforeBuckets, rpmBins, loadBins)
        fillBuckets(afterLog.samples, afterBuckets, rpmBins, loadBins)

        val beforeSummary = buildSummary(beforeLog.samples, beforeBuckets, minHitsPerCell)
        val afterSummary = buildSummary(afterLog.samples, afterBuckets, minHitsPerCell)

        val cells = List(rows) { row ->
            List(cols) { col ->
                val beforeBucket = beforeBuckets[row][col]
                val afterBucket = afterBuckets[row][col]
                val beforeMean = beforeBucket.meanAbsError()
                val afterMean = afterBucket.meanAbsError()
                val state = when {
                    beforeBucket.samples < minHitsPerCell || afterBucket.samples < minHitsPerCell -> LogHeatCellState.NOT_ENOUGH
                    beforeMean == null || afterMean == null -> LogHeatCellState.NOT_ENOUGH
                    beforeMean - afterMean > IMPROVEMENT_EPSILON -> LogHeatCellState.IMPROVED
                    afterMean - beforeMean > IMPROVEMENT_EPSILON -> LogHeatCellState.WORSE
                    else -> LogHeatCellState.UNCHANGED
                }
                LogCellComparison(
                    row = row,
                    col = col,
                    beforeAvgAbsError = beforeMean,
                    afterAvgAbsError = afterMean,
                    beforeSamples = beforeBucket.samples,
                    afterSamples = afterBucket.samples,
                    state = state,
                )
            }
        }

        return LogCompareResult(
            loadLabel = if (beforeLog.loadType == VeTable.LoadType.MAP) "kPa" else "%",
            rpmBins = rpmBins,
            loadBins = loadBins,
            beforeSummary = beforeSummary,
            afterSummary = afterSummary,
            cells = cells,
        )
    }

    private fun parseComparableLog(
        path: String,
        preferredLoadType: VeTable.LoadType,
        afrTolerance: Double,
        minAfr: Double,
        maxAfr: Double,
    ): ParsedComparableLog {
        File(path).bufferedReader().use { reader ->
            val headerLine = reader.readLine() ?: throw LogCompareException(LogCompareReason.EMPTY_LOG)
            val headers = headerLine.split(",").map { it.trim() }
            val columns = resolveColumns(headers)

            val rpmIndex = columns.rpmIndex ?: throw LogCompareException(LogCompareReason.MISSING_RPM)
            val afrIndex = columns.afrIndex ?: throw LogCompareException(LogCompareReason.MISSING_AFR_MEASURED)
            val afrTargetIndex = columns.afrTargetIndex ?: throw LogCompareException(LogCompareReason.MISSING_AFR_TARGET)
            val loadType = when {
                preferredLoadType == VeTable.LoadType.MAP && columns.mapIndex != null -> VeTable.LoadType.MAP
                preferredLoadType == VeTable.LoadType.TPS && columns.tpsIndex != null -> VeTable.LoadType.TPS
                columns.mapIndex != null -> VeTable.LoadType.MAP
                columns.tpsIndex != null -> VeTable.LoadType.TPS
                else -> throw LogCompareException(LogCompareReason.MISSING_LOAD_CHANNEL)
            }
            val loadIndex = (if (loadType == VeTable.LoadType.MAP) columns.mapIndex else columns.tpsIndex)
                ?: throw LogCompareException(LogCompareReason.MISSING_LOAD_CHANNEL)

            val samples = mutableListOf<CompareLogSample>()
            reader.lineSequence().forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = line.split(",")
                val rpm = cols.getOrNull(rpmIndex)?.trim()?.toDoubleOrNull() ?: return@forEach
                val load = cols.getOrNull(loadIndex)?.trim()?.toDoubleOrNull() ?: return@forEach
                val afrMeasured = cols.getOrNull(afrIndex)?.trim()?.toDoubleOrNull() ?: return@forEach
                val afrTarget = cols.getOrNull(afrTargetIndex)?.trim()?.toDoubleOrNull() ?: return@forEach
                if (rpm <= 0.0 || afrMeasured !in minAfr..maxAfr || afrTarget !in minAfr..maxAfr) return@forEach
                samples += CompareLogSample(
                    rpm = rpm,
                    load = load,
                    absError = abs(afrMeasured - afrTarget),
                    isLean = afrMeasured > (afrTarget + afrTolerance),
                    isRich = afrMeasured < (afrTarget - afrTolerance),
                )
            }
            if (samples.isEmpty()) throw LogCompareException(LogCompareReason.NO_VALID_SAMPLES)
            return ParsedComparableLog(loadType, samples)
        }
    }

    private fun fillBuckets(
        samples: List<CompareLogSample>,
        buckets: List<List<CellBucket>>,
        rpmBins: List<Int>,
        loadBins: List<Int>,
    ) {
        samples.forEach { sample ->
            val row = nearestIndex(loadBins, sample.load)
            val col = nearestIndex(rpmBins, sample.rpm)
            val bucket = buckets[row][col]
            bucket.samples += 1
            bucket.sumAbsError += sample.absError
        }
    }

    private fun buildSummary(
        samples: List<CompareLogSample>,
        buckets: List<List<CellBucket>>,
        minHitsPerCell: Int,
    ): LogSummaryMetrics {
        val coverage = buckets.sumOf { row -> row.count { it.samples >= minHitsPerCell } }
        val totalCells = buckets.sumOf { it.size }
        return LogSummaryMetrics(
            avgAbsError = samples.map { it.absError }.average(),
            leanTimeRatio = samples.count { it.isLean }.toDouble() / samples.size,
            richTimeRatio = samples.count { it.isRich }.toDouble() / samples.size,
            coverageCells = coverage,
            totalCells = totalCells,
            validSamples = samples.size,
        )
    }

    private fun resolveColumns(headers: List<String>): LogColumns {
        val normalized = headers.map { it.trim().lowercase().replace(" ", "_") }
        val rpmIndex = findHeaderIndex(normalized, listOf("rpm"))
        val mapIndex = findHeaderIndex(normalized, listOf("map", "mapkpa", "map_kpa"))
        val tpsIndex = findHeaderIndex(normalized, listOf("tps", "throttle"))
        val afrTargetIndex = findHeaderIndex(
            normalized,
            listOf("afr_target", "afrtarget", "targetafr", "target_afr", "ego_target", "o2target", "lambda_target")
        )
        val afrIndex = findAfrMeasuredIndex(normalized, afrTargetIndex)
        return LogColumns(rpmIndex, mapIndex, tpsIndex, afrIndex, afrTargetIndex)
    }

    private fun findHeaderIndex(headers: List<String>, candidates: List<String>): Int? {
        candidates.forEach { candidate ->
            val exact = headers.indexOfFirst { it == candidate }
            if (exact >= 0) return exact
        }
        candidates.forEach { candidate ->
            val contains = headers.indexOfFirst { it.contains(candidate) }
            if (contains >= 0) return contains
        }
        return null
    }

    private fun findAfrMeasuredIndex(headers: List<String>, afrTargetIndex: Int?): Int? {
        val candidates = listOf("afr", "ego", "o2", "lambda")
        headers.forEachIndexed { index, header ->
            if (index == afrTargetIndex) return@forEachIndexed
            if (header.contains("target") || header.contains("tgt")) return@forEachIndexed
            if (candidates.any { header == it || header.contains(it) }) return index
        }
        return null
    }

    private fun nearestIndex(bins: List<Int>, value: Double): Int {
        var bestIndex = 0
        var bestDistance = abs(value - bins[0])
        bins.indices.forEach { index ->
            val distance = abs(value - bins[index])
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private data class LogColumns(
        val rpmIndex: Int?,
        val mapIndex: Int?,
        val tpsIndex: Int?,
        val afrIndex: Int?,
        val afrTargetIndex: Int?,
    )

    private data class ParsedComparableLog(
        val loadType: VeTable.LoadType,
        val samples: List<CompareLogSample>,
    )

    private data class CompareLogSample(
        val rpm: Double,
        val load: Double,
        val absError: Double,
        val isLean: Boolean,
        val isRich: Boolean,
    )

    private class CellBucket {
        var samples: Int = 0
        var sumAbsError: Double = 0.0
        fun meanAbsError(): Double? = if (samples <= 0) null else sumAbsError / samples
    }

    companion object {
        const val DEFAULT_AFR_TOLERANCE = 0.2
        const val DEFAULT_MIN_HITS = 5
        const val PLAUSIBLE_AFR_MIN = 8.0
        const val PLAUSIBLE_AFR_MAX = 22.0
        private const val IMPROVEMENT_EPSILON = 0.01
    }
}
