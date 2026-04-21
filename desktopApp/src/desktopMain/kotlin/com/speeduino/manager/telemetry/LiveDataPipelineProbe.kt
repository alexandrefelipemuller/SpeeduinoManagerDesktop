package com.speeduino.manager.telemetry

import android.util.Log
import java.util.Locale

object LiveDataPipelineProbe {
    private const val TAG = "LiveDataPipeline"

    private val lock = Any()

    private var currentSeq = 0L
    private var latestReceivedSeq = 0L
    private var latestAppliedSeq = 0L
    private var latestReceivedAtNs = 0L
    private var latestAppliedAtNs = 0L
    private var windowStartedAtNs = 0L

    private var totalReceived = 0
    private var totalApplied = 0
    private var totalConsumed = 0
    private var totalSkippedBeforeDashboard = 0
    private var lastConsumedSeq = 0L

    private val clientToMainMs = mutableListOf<Double>()
    private val mainToDashboardMs = mutableListOf<Double>()
    private val clientToDashboardMs = mutableListOf<Double>()
    private val emittedSummaries = mutableListOf<String>()

    fun reset(reason: String) {
        synchronized(lock) {
            currentSeq = 0L
            latestReceivedSeq = 0L
            latestAppliedSeq = 0L
            latestReceivedAtNs = 0L
            latestAppliedAtNs = 0L
            windowStartedAtNs = System.nanoTime()
            totalReceived = 0
            totalApplied = 0
            totalConsumed = 0
            totalSkippedBeforeDashboard = 0
            lastConsumedSeq = 0L
            clientToMainMs.clear()
            mainToDashboardMs.clear()
            clientToDashboardMs.clear()
            emittedSummaries.clear()
            emit("reset reason=$reason")
        }
    }

    fun onClientDataReceived(rpm: Int) {
        synchronized(lock) {
            val nowNs = System.nanoTime()
            if (windowStartedAtNs == 0L) {
                windowStartedAtNs = nowNs
            }
            currentSeq += 1
            latestReceivedSeq = currentSeq
            latestReceivedAtNs = nowNs
            totalReceived += 1
            Log.v(TAG, "stage=client_received seq=$latestReceivedSeq rpm=$rpm")
        }
    }

    fun onMainStateApplied(rpm: Int) {
        synchronized(lock) {
            val nowNs = System.nanoTime()
            if (latestReceivedSeq == 0L || latestReceivedAtNs == 0L) return
            latestAppliedSeq = latestReceivedSeq
            latestAppliedAtNs = nowNs
            totalApplied += 1
            clientToMainMs += nanosToMs(nowNs - latestReceivedAtNs)
            Log.v(TAG, "stage=main_state_applied seq=$latestAppliedSeq rpm=$rpm")
        }
    }

    fun onDashboardConsumed(rpm: Int) {
        synchronized(lock) {
            val nowNs = System.nanoTime()
            if (latestAppliedSeq == 0L || latestAppliedAtNs == 0L) return
            totalConsumed += 1
            mainToDashboardMs += nanosToMs(nowNs - latestAppliedAtNs)
            if (latestReceivedAtNs != 0L) {
                clientToDashboardMs += nanosToMs(nowNs - latestReceivedAtNs)
            }
            if (lastConsumedSeq != 0L && latestAppliedSeq > lastConsumedSeq + 1) {
                totalSkippedBeforeDashboard += (latestAppliedSeq - lastConsumedSeq - 1).toInt()
            }
            lastConsumedSeq = latestAppliedSeq
            Log.v(TAG, "stage=dashboard_consumed seq=$latestAppliedSeq rpm=$rpm")
        }
    }

    fun logSummary(finalReport: Boolean) {
        synchronized(lock) {
            if (windowStartedAtNs == 0L) return
            val elapsedMs = nanosToMs(System.nanoTime() - windowStartedAtNs)
            val clientToMain = Stats.from(clientToMainMs)
            val mainToDashboard = Stats.from(mainToDashboardMs)
            val clientToDashboard = Stats.from(clientToDashboardMs)

            emit(
                buildString {
                    append("final=").append(finalReport)
                    append(" window_ms=").append(format(elapsedMs))
                    append(" received=").append(totalReceived)
                    append(" applied=").append(totalApplied)
                    append(" consumed=").append(totalConsumed)
                    append(" skipped_before_dashboard=").append(totalSkippedBeforeDashboard)
                    append(" client_to_main_avg_ms=").append(format(clientToMain.avg))
                    append(" client_to_main_p95_ms=").append(format(clientToMain.p95))
                    append(" client_to_main_max_ms=").append(format(clientToMain.max))
                    append(" main_to_dashboard_avg_ms=").append(format(mainToDashboard.avg))
                    append(" main_to_dashboard_p95_ms=").append(format(mainToDashboard.p95))
                    append(" main_to_dashboard_max_ms=").append(format(mainToDashboard.max))
                    append(" client_to_dashboard_avg_ms=").append(format(clientToDashboard.avg))
                    append(" client_to_dashboard_p95_ms=").append(format(clientToDashboard.p95))
                    append(" client_to_dashboard_max_ms=").append(format(clientToDashboard.max))
                }
            )

            if (!finalReport) {
                windowStartedAtNs = System.nanoTime()
                clientToMainMs.clear()
                mainToDashboardMs.clear()
                clientToDashboardMs.clear()
                totalSkippedBeforeDashboard = 0
            }
        }
    }

    fun consumeSummaries(): List<String> {
        synchronized(lock) {
            val snapshot = emittedSummaries.toList()
            emittedSummaries.clear()
            return snapshot
        }
    }

    private fun nanosToMs(durationNs: Long): Double = durationNs / 1_000_000.0

    private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun emit(message: String) {
        emittedSummaries += message
        Log.d(TAG, message)
        println("$TAG: $message")
    }

    private data class Stats(
        val avg: Double,
        val p95: Double,
        val max: Double
    ) {
        companion object {
            fun from(values: List<Double>): Stats {
                if (values.isEmpty()) return Stats(0.0, 0.0, 0.0)
                val sorted = values.sorted()
                return Stats(
                    avg = sorted.average(),
                    p95 = percentile(sorted, 0.95),
                    max = sorted.last()
                )
            }

            private fun percentile(sorted: List<Double>, ratio: Double): Double {
                val index = (sorted.lastIndex * ratio).toInt().coerceIn(0, sorted.lastIndex)
                return sorted[index]
            }
        }
    }
}
