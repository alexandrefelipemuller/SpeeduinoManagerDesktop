package com.speeduino.manager.desktop.feature.logs

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

internal class VirtualDynoModelDesktop {
    private val schema = loadSchema()
    private val encoder = schema.getJSONObject("encoder")
    private val numericFeatures = encoder.getJSONArray("numeric_features").toStringList()
    private val categoricalFeatures = encoder.getJSONArray("categorical_features").toStringList()
    private val featureNames = encoder.getJSONArray("feature_names").toStringList()
    private val numericFills = encoder.getJSONObject("numeric_fills")
    private val categoricalMaps = encoder.getJSONObject("categorical_maps")
    private val mean = encoder.getJSONArray("mean").toDoubleArray()
    private val scale = encoder.getJSONArray("scale").toDoubleArray()
    private val thresholds = schema.getJSONObject("classifier").getJSONObject("thresholds")
    private val lowMid = thresholds.getDouble("low_mid_whp")
    private val midHigh = thresholds.getDouble("mid_high_whp")
    private val linearWeights = loadLinearWeights(featureNames.size, 2)

    fun analyzeCsv(
        csvText: String,
        fileLabel: String? = null,
        vehicleSpecs: VirtualDynoVehicleSpecs = VirtualDynoVehicleSpecs(),
    ): VirtualDynoAnalysisResult {
        val rows = parseCsv(csvText)
        val predictions = rows.mapNotNull { row ->
            val input = row.toDynoInput(vehicleSpecs) ?: return@mapNotNull null
            val output = predict(input)
            VirtualDynoPrediction(
                rpm = input.rpm,
                tpsPct = input.tpsPct,
                speedKmh = input.speedKmh,
                mapKpaAbs = input.mapKpaAbs,
                powerWhp = output.powerWhp,
                torqueNm = output.torqueNm,
                powerBand = output.powerBand,
            )
        }
        val useful = predictions.filter { it.rpm >= 900.0 && it.tpsPct >= 45.0 && it.powerWhp > 0.0 }
        val analysisSet = useful.ifEmpty { predictions }
        val peakPower = analysisSet.maxByOrNull { it.powerWhp }
        val peakTorque = analysisSet.maxByOrNull { it.torqueNm }
        return VirtualDynoAnalysisResult(
            fileLabel = fileLabel,
            totalRows = rows.size,
            inferredRows = predictions.size,
            usefulRows = useful.size,
            peakPowerWhp = peakPower?.powerWhp ?: 0.0,
            peakTorqueNm = peakTorque?.torqueNm ?: 0.0,
            averagePowerWhp = analysisSet.map { it.powerWhp }.averageOrZero(),
            averageTorqueNm = analysisSet.map { it.torqueNm }.averageOrZero(),
            powerBand = bandFor(peakPower?.powerWhp ?: 0.0),
            quality = qualityFor(predictions, useful),
            vehicleSpecs = vehicleSpecs,
            predictions = predictions,
        )
    }

    private fun predict(input: VirtualDynoInput): VirtualDynoOutput {
        val features = buildFeatures(input)
        val outputs = DoubleArray(linearWeights.bias.size)
        for (outputIndex in outputs.indices) {
            var total = linearWeights.bias[outputIndex]
            for (featureIndex in features.indices) {
                total += features[featureIndex] * linearWeights.weights[featureIndex][outputIndex]
            }
            outputs[outputIndex] = max(0.0, total)
        }
        return VirtualDynoOutput(
            powerWhp = outputs.getOrElse(0) { 0.0 },
            torqueNm = outputs.getOrElse(1) { 0.0 },
            powerBand = bandFor(outputs.getOrElse(0) { 0.0 }),
        )
    }

    private fun buildFeatures(input: VirtualDynoInput): DoubleArray {
        val values = mutableMapOf<String, Double>()
        numericFeatures.forEach { name ->
            values[name] = input.numeric[name] ?: numericFills.optDouble(name, 0.0)
        }
        val rpm = values.getValue("rpm")
        val tps = values.getValue("tps_pct")
        val mapAbs = values.getValue("map_kpa_abs")
        val speed = values.getValue("speed_kmh")
        val weight = max(values.getValue("weight_kg"), 1.0)
        val area = values.getValue("corrected_frontal_area_m2")
        val raw = mutableListOf<Double>()
        numericFeatures.forEach { raw += values.getValue(it) }
        raw += rpm * tps
        raw += rpm * mapAbs
        raw += tps * mapAbs
        raw += speed * rpm
        raw += rpm.pow(2.0)
        raw += tps.pow(2.0)
        raw += mapAbs.pow(2.0)
        raw += speed.pow(2.0)
        raw += rpm * max(tps, 0.0) * max(mapAbs, 0.0) / weight
        raw += area * speed * speed
        categoricalFeatures.forEach { name ->
            val categories = categoricalMaps.getJSONArray(name).toStringList()
            val value = input.categorical[name].orEmpty()
            categories.forEach { category -> raw += if (value == category) 1.0 else 0.0 }
        }
        return DoubleArray(featureNames.size) { index ->
            (raw.getOrElse(index) { 0.0 } - mean[index]) / scale[index]
        }
    }

    private fun bandFor(powerWhp: Double): String = when {
        powerWhp < lowMid -> "low"
        powerWhp < midHigh -> "mid"
        else -> "high"
    }

    private fun qualityFor(all: List<VirtualDynoPrediction>, useful: List<VirtualDynoPrediction>): VirtualDynoQuality {
        if (all.isEmpty()) return VirtualDynoQuality.BAD
        val usefulRatio = useful.size.toDouble() / all.size.toDouble()
        val usefulRpmRange = useful.maxOfOrNull { it.rpm }?.minus(useful.minOfOrNull { it.rpm } ?: 0.0) ?: 0.0
        return when {
            useful.size < 12 -> VirtualDynoQuality.BAD
            usefulRatio >= 0.25 && usefulRpmRange >= 1500.0 -> VirtualDynoQuality.GOOD
            usefulRatio >= 0.10 && usefulRpmRange >= 800.0 -> VirtualDynoQuality.USABLE
            else -> VirtualDynoQuality.BAD
        }
    }

    private fun loadSchema(): JSONObject {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("virtual_dyno/schema.json")) {
            "Bundled virtual dyno schema not found."
        }
        return stream.bufferedReader().use { JSONObject(it.readText()) }
    }

    private fun loadLinearWeights(featureCount: Int, outputCount: Int): LinearWeights {
        val modelBytes = checkNotNull(javaClass.classLoader.getResourceAsStream("virtual_dyno/virtual_dyno_linear.tflite")) {
            "Bundled virtual dyno model not found."
        }.use { it.readBytes() }
        val weightVectorLengthBytes = featureCount * outputCount * Float.SIZE_BYTES
        val biasVectorLengthBytes = outputCount * Float.SIZE_BYTES
        val flatWeights = findFloatVector(modelBytes, weightVectorLengthBytes)
            ?: error("Virtual dyno weight vector not found in TFLite model.")
        val bias = findFloatVector(modelBytes, biasVectorLengthBytes)
            ?: error("Virtual dyno bias vector not found in TFLite model.")
        require(flatWeights.size == featureCount * outputCount) { "Unexpected virtual dyno weight vector size." }
        require(bias.size == outputCount) { "Unexpected virtual dyno bias vector size." }
        return LinearWeights(
            weights = Array(featureCount) { featureIndex ->
                DoubleArray(outputCount) { outputIndex ->
                    flatWeights[featureIndex * outputCount + outputIndex].toDouble()
                }
            },
            bias = DoubleArray(outputCount) { bias[it].toDouble() },
        )
    }

    private fun findFloatVector(bytes: ByteArray, byteLength: Int): FloatArray? {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (offset in 0..bytes.size - 4) {
            val length = buffer.getInt(offset)
            if (length != byteLength || offset + 4 + length > bytes.size) continue
            val valueCount = length / Float.SIZE_BYTES
            var hasMeaningfulMagnitude = false
            var valid = true
            val values = FloatArray(valueCount)
            for (index in 0 until valueCount) {
                val value = buffer.getFloat(offset + 4 + index * Float.SIZE_BYTES)
                if (!value.isFinite() || abs(value) > 10_000f) {
                    valid = false
                    break
                }
                if (abs(value) > 0.01f) {
                    hasMeaningfulMagnitude = true
                }
                values[index] = value
            }
            if (valid && hasMeaningfulMagnitude) {
                return values
            }
        }
        return null
    }
}

internal data class VirtualDynoInput(
    val numeric: Map<String, Double>,
    val categorical: Map<String, String>,
) {
    val rpm: Double get() = numeric["rpm"] ?: 0.0
    val tpsPct: Double get() = numeric["tps_pct"] ?: 0.0
    val speedKmh: Double get() = numeric["speed_kmh"] ?: 0.0
    val mapKpaAbs: Double get() = numeric["map_kpa_abs"] ?: 0.0
}

internal data class VirtualDynoOutput(
    val powerWhp: Double,
    val torqueNm: Double,
    val powerBand: String,
)

internal data class VirtualDynoPrediction(
    val rpm: Double,
    val tpsPct: Double,
    val speedKmh: Double,
    val mapKpaAbs: Double,
    val powerWhp: Double,
    val torqueNm: Double,
    val powerBand: String,
)

internal data class VirtualDynoAnalysisResult(
    val fileLabel: String?,
    val totalRows: Int,
    val inferredRows: Int,
    val usefulRows: Int,
    val peakPowerWhp: Double,
    val peakTorqueNm: Double,
    val averagePowerWhp: Double,
    val averageTorqueNm: Double,
    val powerBand: String,
    val quality: VirtualDynoQuality,
    val vehicleSpecs: VirtualDynoVehicleSpecs = VirtualDynoVehicleSpecs(),
    val predictions: List<VirtualDynoPrediction> = emptyList(),
) {
    fun buildCurve(tpsThreshold: Double = 95.0, rpmBinSize: Int = 250): VirtualDynoCurve {
        val grouped = predictions
            .asSequence()
            .filter { it.rpm >= 900.0 && it.tpsPct >= tpsThreshold && it.powerWhp > 0.0 }
            .groupBy { ((it.rpm / rpmBinSize).roundToInt() * rpmBinSize).toInt() }
            .toSortedMap()
        val rawPoints = grouped.mapNotNull { (rpmBin, values) ->
            if (values.size < 2) return@mapNotNull null
            VirtualDynoCurvePoint(
                rpm = rpmBin.toDouble(),
                powerWhp = values.map { it.powerWhp }.average(),
                torqueNm = 0.0,
                samples = values.size,
            )
        }
        val points = rawPoints.smoothPowerAndDeriveTorque()
        return VirtualDynoCurve(
            points = points,
            tpsThreshold = tpsThreshold,
            peakPower = points.maxByOrNull { it.powerWhp },
            peakTorque = points.maxByOrNull { it.torqueNm },
        )
    }

    fun buildBestCurve(): VirtualDynoCurve {
        val strict = buildCurve(tpsThreshold = 95.0)
        if (strict.points.size >= 2) return strict
        val relaxed = buildCurve(tpsThreshold = 90.0)
        if (relaxed.points.size >= 2) return relaxed
        return buildCurve(tpsThreshold = 80.0)
    }
}

internal data class VirtualDynoCurve(
    val points: List<VirtualDynoCurvePoint>,
    val tpsThreshold: Double,
    val peakPower: VirtualDynoCurvePoint?,
    val peakTorque: VirtualDynoCurvePoint?,
)

internal data class VirtualDynoCurvePoint(
    val rpm: Double,
    val powerWhp: Double,
    val torqueNm: Double,
    val samples: Int,
)

private fun List<VirtualDynoCurvePoint>.smoothPowerAndDeriveTorque(): List<VirtualDynoCurvePoint> {
    val anchored = listOf(VirtualDynoCurvePoint(rpm = 500.0, powerWhp = 0.0, torqueNm = 0.0, samples = 0)) +
        filter { it.rpm > 500.0 }
    if (anchored.size < 3) {
        return anchored.map { point -> point.copy(torqueNm = torqueFromWhp(point.powerWhp, point.rpm)) }
    }
    val firstPass = anchored.smoothPower(windowRadius = 3)
    val secondPass = firstPass.smoothPower(windowRadius = 3)
    val thirdPass = secondPass.smoothPower(windowRadius = 2)
    val derived = thirdPass.map { point ->
        point.copy(torqueNm = torqueFromWhp(point.powerWhp, point.rpm))
    }
    return derived.smoothTorque(windowRadius = 2).softenTorqueSpikes()
}

private fun List<VirtualDynoCurvePoint>.smoothPower(windowRadius: Int): List<VirtualDynoCurvePoint> {
    return mapIndexed { index, point ->
        if (point.rpm <= 500.0) return@mapIndexed point.copy(powerWhp = 0.0, torqueNm = 0.0)
        var weightedPower = 0.0
        var totalWeight = 0.0
        for (offset in -windowRadius..windowRadius) {
            val neighbor = getOrNull(index + offset) ?: continue
            val distance = abs(offset)
            val weight = (windowRadius + 1 - distance).toDouble().coerceAtLeast(1.0)
            weightedPower += neighbor.powerWhp * weight
            totalWeight += weight
        }
        point.copy(powerWhp = weightedPower / totalWeight)
    }
}

private fun List<VirtualDynoCurvePoint>.smoothTorque(windowRadius: Int): List<VirtualDynoCurvePoint> {
    return mapIndexed { index, point ->
        if (point.rpm <= 500.0) return@mapIndexed point.copy(torqueNm = 0.0)
        var weightedTorque = 0.0
        var totalWeight = 0.0
        for (offset in -windowRadius..windowRadius) {
            val neighbor = getOrNull(index + offset) ?: continue
            val distance = abs(offset)
            val weight = (windowRadius + 1 - distance).toDouble().coerceAtLeast(1.0)
            weightedTorque += neighbor.torqueNm * weight
            totalWeight += weight
        }
        point.copy(torqueNm = weightedTorque / totalWeight)
    }
}

private fun List<VirtualDynoCurvePoint>.softenTorqueSpikes(): List<VirtualDynoCurvePoint> {
    return mapIndexed { index, point ->
        val previous = getOrNull(index - 1)
        val next = getOrNull(index + 1)
        if (previous != null && next != null && point.torqueNm > previous.torqueNm && point.torqueNm > next.torqueNm) {
            val neighborAverage = (previous.torqueNm + next.torqueNm) / 2.0
            point.copy(torqueNm = point.torqueNm * 0.45 + neighborAverage * 0.55)
        } else {
            point
        }
    }
}

private fun torqueFromWhp(powerWhp: Double, rpm: Double): Double {
    if (rpm <= 0.0) return 0.0
    return powerWhp * 7127.0 / rpm
}

internal data class VirtualDynoVehicleSpecs(
    val vehicleName: String = "",
    val weightKg: Double? = null,
    val dragCoefficient: Double? = null,
    val frontalAreaM2: Double? = null,
    val correctedFrontalAreaM2: Double? = null,
    val drivetrain: String = "",
    val analyzedGearRatio: Double? = null,
    val differentialRatio: Double? = null,
    val wheelTireDiameterCm: Double? = null,
) {
    val effectiveCorrectedFrontalAreaM2: Double?
        get() = correctedFrontalAreaM2 ?: dragCoefficient?.let { cd -> frontalAreaM2?.let { area -> cd * area } }

    fun hasAnyValue(): Boolean =
        vehicleName.isNotBlank() || weightKg != null || dragCoefficient != null ||
            frontalAreaM2 != null || correctedFrontalAreaM2 != null || drivetrain.isNotBlank() ||
            analyzedGearRatio != null || differentialRatio != null || wheelTireDiameterCm != null

    fun toReportText(): String = buildString {
        appendLine("vehicle_name=$vehicleName")
        appendLine("weight_kg=${weightKg?.format1().orEmpty()}")
        appendLine("drag_coefficient_cd=${dragCoefficient?.format3().orEmpty()}")
        appendLine("frontal_area_m2=${frontalAreaM2?.format3().orEmpty()}")
        appendLine("corrected_frontal_area_m2=${effectiveCorrectedFrontalAreaM2?.format3().orEmpty()}")
        appendLine("drivetrain=$drivetrain")
        appendLine("analyzed_gear_ratio=${analyzedGearRatio?.format3().orEmpty()}")
        appendLine("differential_ratio=${differentialRatio?.format3().orEmpty()}")
        appendLine("wheel_tire_diameter_cm=${wheelTireDiameterCm?.format1().orEmpty()}")
    }
}

internal enum class VirtualDynoQuality { GOOD, USABLE, BAD }

internal fun VirtualDynoAnalysisResult.toFeedbackText(expectedPower: String, notes: String): String = buildString {
    appendLine("Virtual Dyno experimental report")
    appendLine("file=${fileLabel.orEmpty()}")
    appendLine("rows_total=$totalRows")
    appendLine("rows_inferred=$inferredRows")
    appendLine("rows_useful=$usefulRows")
    appendLine("peak_power_whp=${peakPowerWhp.format1()}")
    appendLine("peak_torque_nm=${peakTorqueNm.format1()}")
    appendLine("avg_power_whp=${averagePowerWhp.format1()}")
    appendLine("avg_torque_nm=${averageTorqueNm.format1()}")
    appendLine("power_band=$powerBand")
    appendLine("quality=$quality")
    appendLine("vehicle_specs_begin")
    append(vehicleSpecs.toReportText())
    appendLine("vehicle_specs_end")
    appendLine("expected_power=$expectedPower")
    appendLine("user_notes=$notes")
}

internal fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
internal fun Double.format3(): String = String.format(Locale.US, "%.3f", this)
internal fun Double.format0(): String = String.format(Locale.US, "%.0f", this)

internal fun Double.niceAxisMax(): Double = when {
    this <= 50.0 -> 50.0
    this <= 100.0 -> 100.0
    this <= 150.0 -> 150.0
    this <= 200.0 -> 200.0
    this <= 300.0 -> 300.0
    this <= 400.0 -> 400.0
    this <= 600.0 -> 600.0
    this <= 800.0 -> 800.0
    else -> ceil(this / 250.0) * 250.0
}

internal fun String.parseOptionalDouble(): Double? = trim()
    .replace(',', '.')
    .takeIf { it.isNotBlank() }
    ?.toDoubleOrNull()

private fun List<Map<String, String>>.firstNumeric(row: Map<String, String>, aliases: List<String>): Double? {
    aliases.forEach { alias ->
        val match = row.entries.firstOrNull { it.key.normalizeKey() == alias.normalizeKey() }?.value
        val value = match?.toDoubleOrNullCompat()
        if (value != null) return value
    }
    return null
}

private fun Map<String, String>.toDynoInput(vehicleSpecs: VirtualDynoVehicleSpecs): VirtualDynoInput? {
    val rows = listOf(this)
    val rpm = rows.firstNumeric(this, listOf("rpm", "RPM")) ?: return null
    val tps = rows.firstNumeric(this, listOf("tps_pct", "TPS", "throttle", "throttle_pct", "TPS (%)")) ?: 0.0
    val speed = rows.firstNumeric(this, listOf("speed_kmh", "speed", "Vehicle Speed", "VSS", "kph", "kmh")) ?: 0.0
    val mapAbsRaw = rows.firstNumeric(this, listOf("map_kpa_abs", "MAP", "map", "MAP kPa", "map_abs"))
    val mapGaugeBar = rows.firstNumeric(this, listOf("map_bar_gauge", "boost_bar", "boost", "Boost bar"))
    val mapAbs = mapAbsRaw ?: mapGaugeBar?.let { (it + 1.01325) * 100.0 } ?: 101.3
    val mapGauge = mapGaugeBar ?: (mapAbs / 100.0 - 1.01325)
    val numeric = mutableMapOf(
        "rpm" to rpm,
        "tps_pct" to tps,
        "speed_kmh" to speed,
        "map_kpa_abs" to mapAbs,
        "map_bar_gauge" to mapGauge,
        "lambda_wb" to (rows.firstNumeric(this, listOf("lambda_wb", "lambda", "Lambda")) ?: 1.0),
        "battery_v" to (rows.firstNumeric(this, listOf("battery_v", "Battery V", "battery")) ?: 13.8),
        "engine_temp_c" to (rows.firstNumeric(this, listOf("engine_temp_c", "CLT", "coolant", "coolant_c")) ?: 85.0),
        "air_temp_c" to (rows.firstNumeric(this, listOf("air_temp_c", "IAT", "iat_c")) ?: 30.0),
    )
    vehicleSpecs.weightKg?.let { numeric["weight_kg"] = it }
    vehicleSpecs.effectiveCorrectedFrontalAreaM2?.let { numeric["corrected_frontal_area_m2"] = it }
    val categorical = mapOf(
        "source_type" to "real",
        "drivetrain" to vehicleSpecs.drivetrain,
        "synthetic" to "0",
    )
    return VirtualDynoInput(numeric = numeric, categorical = categorical)
}

private fun parseCsv(csvText: String): List<Map<String, String>> {
    val lines = csvText.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.size < 2) return emptyList()
    val separator = if (lines.first().count { it == ';' } > lines.first().count { it == ',' }) ';' else ','
    val headers = splitCsvLine(lines.first(), separator).map { it.trim() }
    return lines.drop(1).mapNotNull { line ->
        val values = splitCsvLine(line, separator)
        if (values.isEmpty()) return@mapNotNull null
        headers.mapIndexedNotNull { index, header ->
            values.getOrNull(index)?.trim()?.let { header to it }
        }.toMap()
    }
}

private fun splitCsvLine(line: String, separator: Char): List<String> {
    val out = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    line.forEach { char ->
        when {
            char == '"' -> inQuotes = !inQuotes
            char == separator && !inQuotes -> {
                out += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
    }
    out += current.toString()
    return out
}

private fun String.toDoubleOrNullCompat(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun String.normalizeKey(): String = lowercase(Locale.US)
    .replace("[", "")
    .replace("]", "")
    .replace("%", "pct")
    .replace("/", "_")
    .replace(" ", "_")

private fun Iterable<Double>.averageOrZero(): Double {
    val values = toList()
    return if (values.isEmpty()) 0.0 else values.average()
}

private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }
private fun JSONArray.toDoubleArray(): DoubleArray = DoubleArray(length()) { getDouble(it) }

private data class LinearWeights(
    val weights: Array<DoubleArray>,
    val bias: DoubleArray,
)
