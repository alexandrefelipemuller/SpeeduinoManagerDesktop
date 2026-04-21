package com.speeduino.manager.transport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Obd2OptimizationFeature(
    val bitIndex: Int,
    val displayName: String,
) {
    DISABLE_SPACES(bitIndex = 0, displayName = "Disable spaces"),
    FIXED_PROTOCOL(bitIndex = 1, displayName = "Fixed protocol"),
    PRIORITY_SCHEDULER(bitIndex = 2, displayName = "Priority scheduler"),
    FAST_TIMEOUT(bitIndex = 3, displayName = "Fast timeout"),
    TIGHT_READ_LOOP(bitIndex = 4, displayName = "Tight read loop"),
    FAST_POLL_INTERVAL(bitIndex = 5, displayName = "Fast poll interval"),
    MULTI_PID_EXPERIMENTAL(bitIndex = 6, displayName = "Multi-PID experimental"),
}

data class Obd2OptimizationStatus(
    val appliedMask: Int,
    val testedCount: Int,
    val totalCount: Int,
    val currentFeature: Obd2OptimizationFeature?,
    val message: String,
    val testing: Boolean,
    val fallbackOccurred: Boolean = false,
) {
    val progressFraction: Float
        get() = if (totalCount <= 0) 0f else testedCount.toFloat() / totalCount.toFloat()
}

object Obd2OptimizationStatusBus {
    private val _state = MutableStateFlow<Obd2OptimizationStatus?>(null)
    val state: StateFlow<Obd2OptimizationStatus?> = _state.asStateFlow()

    fun publish(status: Obd2OptimizationStatus) {
        _state.value = status
    }

    fun clear() {
        _state.value = null
    }
}

enum class VehicleBrandHint {
    UNKNOWN,
    PSA,
    RENAULT,
    VAG,
}

data class Obd2PersistedProfile(
    val mask: Int,
    val supportedPids: Set<Int>,
    val preferredO2Pid: Int?,
)

interface Obd2ProfileStore {
    fun load(profileKey: String): Obd2PersistedProfile?

    fun save(profileKey: String, profile: Obd2PersistedProfile)
}

data class PsaPersistedSession(
    val protocol: String,
    val txId: String,
    val rxId: String,
    val hintsCsv: String,
    val oemProfileId: String,
    val functionalHeader: String,
    val c4LiveModeEnabled: Boolean,
    val isFunctional: Boolean,
    val timestampMs: Long,
)

interface PsaSessionStore {
    fun load(): PsaPersistedSession?

    fun save(session: PsaPersistedSession)

    fun clear()
}

fun Set<Obd2OptimizationFeature>.toMask(): Int {
    var mask = 0
    for (feature in this) {
        mask = mask or (1 shl feature.bitIndex)
    }
    return mask
}

fun featuresFromMask(mask: Int): Set<Obd2OptimizationFeature> {
    return Obd2OptimizationFeature.entries
        .filter { feature -> (mask and (1 shl feature.bitIndex)) != 0 }
        .toSet()
}

fun resolveObd2ProfileKey(
    connectionInfo: String,
    vin: String?,
    calibrationId: String?,
): String {
    val trimmedConnection = connectionInfo.trim().ifBlank { "unknown" }
    if (!vin.isNullOrBlank()) {
        return if (calibrationId != null) {
            "vin:$vin|cal:$calibrationId"
        } else {
            "vin:$vin"
        }
    }
    if (!calibrationId.isNullOrBlank()) {
        return "cal:$calibrationId|conn:$trimmedConnection"
    }
    return "conn:$trimmedConnection"
}

fun classifyVehicleBrandHint(vin: String?, calibrationId: String?): VehicleBrandHint {
    val vinUpper = vin.orEmpty().uppercase()
    val calibrationUpper = calibrationId.orEmpty().uppercase()
    return when {
        vinUpper.startsWith("VF7") ||
            vinUpper.startsWith("VF3") ||
            vinUpper.startsWith("VR7") ||
            vinUpper.startsWith("VR3") ||
            calibrationUpper.contains("PSA") ||
            calibrationUpper.contains("PEUGEOT") ||
            calibrationUpper.contains("CITROEN") ||
            calibrationUpper.contains("DS") -> VehicleBrandHint.PSA
        vinUpper.startsWith("VF1") ||
            calibrationUpper.contains("RENAULT") ||
            calibrationUpper.contains("DACIA") -> VehicleBrandHint.RENAULT
        vinUpper.startsWith("WVW") ||
            vinUpper.startsWith("WAU") ||
            vinUpper.startsWith("VSS") ||
            vinUpper.startsWith("SKZ") ||
            calibrationUpper.contains("VAG") ||
            calibrationUpper.contains("VOLKSWAGEN") ||
            calibrationUpper.contains("AUDI") ||
            calibrationUpper.contains("SEAT") ||
            calibrationUpper.contains("SKODA") -> VehicleBrandHint.VAG
        else -> VehicleBrandHint.UNKNOWN
    }
}

fun shouldPromoteToPsa(vehicleBrandHint: VehicleBrandHint): Boolean = vehicleBrandHint == VehicleBrandHint.PSA
