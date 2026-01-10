package com.speeduino.manager.model.logging

import com.speeduino.manager.SpeeduinoLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque
import java.util.UUID

/**
 * Buffer leve em memoria para armazenar logs de live data no Android.
 * Mantem um deque com capacidade fixa para evitar pressao de RAM e expos
 * um StateFlow com estado agregado para a UI.
 */
class LiveLogRecorder(
    private val maxSamples: Int = DEFAULT_MAX_SAMPLES
) {

    private val lock = Any()
    private var currentSession: LiveLogSession? = null

    private val _state = MutableStateFlow(LiveLogRecorderState())
    val state: StateFlow<LiveLogRecorderState> = _state.asStateFlow()

    /**
     * Inicia nova captura, descartando buffer anterior.
     */
    fun start(sampleIntervalMs: Long) {
        synchronized(lock) {
            currentSession = LiveLogSession(
                id = UUID.randomUUID().toString(),
                sampleIntervalMs = sampleIntervalMs
            )
            _state.value = LiveLogRecorderState(
                isRecording = true,
                startedAtMs = System.currentTimeMillis(),
                sampleIntervalMs = sampleIntervalMs,
                samplesCaptured = 0,
                lastSample = null
            )
        }
    }

    /**
     * Finaliza captura atual mantendo buffer em memoria.
     */
    fun stop() {
        synchronized(lock) {
            val session = currentSession ?: return
            session.stoppedAtMs = System.currentTimeMillis()
            _state.update {
                it.copy(
                    isRecording = false,
                    stoppedAtMs = session.stoppedAtMs
                )
            }
        }
    }

    /**
     * Registra um pacote de live data no buffer atual.
     */
    fun record(liveData: SpeeduinoLiveData) {
        val session = synchronized(lock) { currentSession } ?: return

        val entry = LiveLogEntry.fromLiveData(
            liveData = liveData,
            timestampMs = System.currentTimeMillis()
        )

        synchronized(lock) {
            session.append(entry, maxSamples)
            _state.update {
                it.copy(
                    samplesCaptured = session.size(),
                    lastSample = entry,
                    sampleIntervalMs = session.sampleIntervalMs
                )
            }
        }
    }

    /**
     * Snapshot imutavel da sessao atual, usado posteriormente pelo Log Viewer.
     */
    fun snapshot(): LiveLogSnapshot? = synchronized(lock) {
        currentSession?.toSnapshot()
    }

    companion object {
        const val DEFAULT_MAX_SAMPLES = 12_000 // ~20 min a 10 Hz
    }
}

/**
 * Representa um ponto amostral de live data em formato compacto.
 * Valores sao normalizados para inteiros para reduzir uso de memoria.
 */
data class LiveLogEntry(
    val timestampMs: Long,
    val rpm: Int,
    val mapKpa: Int,
    val tps: Int,
    val coolantTempC: Int,
    val intakeTempC: Int,
    val batteryDeciVolt: Int,
    val advanceDeg: Int,
    val o2: Int
) {
    companion object {
        fun fromLiveData(liveData: SpeeduinoLiveData, timestampMs: Long): LiveLogEntry {
            return LiveLogEntry(
                timestampMs = timestampMs,
                rpm = liveData.rpm,
                mapKpa = liveData.mapPressure,
                tps = liveData.tps,
                coolantTempC = liveData.coolantTemp,
                intakeTempC = liveData.intakeTemp,
                batteryDeciVolt = (liveData.batteryVoltage * 10).toInt(),
                advanceDeg = liveData.advance,
                o2 = liveData.o2
            )
        }
    }
}

data class LiveLogRecorderState(
    val isRecording: Boolean = false,
    val startedAtMs: Long? = null,
    val stoppedAtMs: Long? = null,
    val sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS,
    val samplesCaptured: Int = 0,
    val lastSample: LiveLogEntry? = null
) {
    companion object {
        const val DEFAULT_SAMPLE_INTERVAL_MS = 100L
    }
}

data class LiveLogSnapshot(
    val id: String,
    val startedAtMs: Long,
    val stoppedAtMs: Long?,
    val sampleIntervalMs: Long,
    val entries: List<LiveLogEntry>
)

private data class LiveLogSession(
    val id: String,
    val sampleIntervalMs: Long,
    private val entries: ArrayDeque<LiveLogEntry> = ArrayDeque(),
    var stoppedAtMs: Long? = null,
    val startedAtMs: Long = System.currentTimeMillis()
) {
    fun append(entry: LiveLogEntry, maxSamples: Int) {
        if (entries.size >= maxSamples) {
            entries.removeFirst()
        }
        entries.addLast(entry)
    }

    fun size(): Int = entries.size

    fun toSnapshot(): LiveLogSnapshot {
        return LiveLogSnapshot(
            id = id,
            startedAtMs = startedAtMs,
            stoppedAtMs = stoppedAtMs,
            sampleIntervalMs = sampleIntervalMs,
            entries = entries.toList()
        )
    }
}
