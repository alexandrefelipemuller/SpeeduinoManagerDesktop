package com.speeduino.manager.desktop.feature.logs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speeduino.manager.desktop.DesktopSpeeduinoController
import com.speeduino.manager.desktop.LocalStrings
import com.speeduino.manager.desktop.ui.chooseOpenFile
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun VirtualDynoScreenDesktop(controller: DesktopSpeeduinoController) {
    val strings = LocalStrings.current
    val lastSavedLogPath by controller.lastSavedLogPath.collectAsState()
    val scope = rememberCoroutineScope()
    var currentLogPath by remember { mutableStateOf(lastSavedLogPath) }
    var currentLogLabel by remember { mutableStateOf(lastSavedLogPath?.substringAfterLast(File.separator)) }
    var result by remember { mutableStateOf<VirtualDynoAnalysisResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var feedbackStatus by remember { mutableStateOf<String?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
    var expectedPower by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var vehicleName by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var dragCoefficient by remember { mutableStateOf("") }
    var frontalAreaM2 by remember { mutableStateOf("") }
    var correctedFrontalAreaM2 by remember { mutableStateOf("") }
    var drivetrain by remember { mutableStateOf("") }
    var analyzedGearRatio by remember { mutableStateOf("") }
    var differentialRatio by remember { mutableStateOf("") }
    var wheelTireDiameterCm by remember { mutableStateOf("") }

    LaunchedEffect(lastSavedLogPath) {
        if (currentLogPath == null && lastSavedLogPath != null) {
            currentLogPath = lastSavedLogPath
            currentLogLabel = lastSavedLogPath?.substringAfterLast(File.separator)
        }
    }

    fun specs() = VirtualDynoVehicleSpecs(
        vehicleName = vehicleName.trim(),
        weightKg = weightKg.parseOptionalDouble(),
        dragCoefficient = dragCoefficient.parseOptionalDouble(),
        frontalAreaM2 = frontalAreaM2.parseOptionalDouble(),
        correctedFrontalAreaM2 = correctedFrontalAreaM2.parseOptionalDouble(),
        drivetrain = drivetrain.trim(),
        analyzedGearRatio = analyzedGearRatio.parseOptionalDouble(),
        differentialRatio = differentialRatio.parseOptionalDouble(),
        wheelTireDiameterCm = wheelTireDiameterCm.parseOptionalDouble(),
    )

    fun analyzeSelected(path: String?) {
        scope.launch {
            isLoading = true
            error = null
            feedbackStatus = null
            val analysis = runCatching {
                withContext(Dispatchers.IO) {
                    val file = path?.let(::File)?.takeIf { it.exists() }
                        ?: error(strings["label.virtualDynoSelectLogFirst"])
                    VirtualDynoModelDesktop().analyzeCsv(
                        csvText = file.readText(),
                        fileLabel = file.name,
                        vehicleSpecs = specs(),
                    )
                }
            }
            analysis.onSuccess { result = it }
            analysis.onFailure {
                result = null
                error = it.message ?: strings["label.virtualDynoAnalyzeFailed"]
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F2EC))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(strings["route.virtualDyno"], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    strings["label.virtualDynoHeroDesc"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings["label.virtualDynoExperiment"], style = MaterialTheme.typography.titleMedium)
                Text(
                    strings["label.virtualDynoExperimentDesc"],
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(strings["label.virtualDynoCurrentLog"], style = MaterialTheme.typography.titleMedium)
                Text(
                    currentLogLabel ?: strings["label.virtualDynoNoCsvSelected"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            currentLogPath = lastSavedLogPath
                            currentLogLabel = lastSavedLogPath?.substringAfterLast(File.separator)
                            analyzeSelected(lastSavedLogPath)
                        },
                        enabled = lastSavedLogPath != null && !isLoading,
                    ) {
                        Text(strings["label.virtualDynoUseLastLog"])
                    }
                    OutlinedButton(
                        onClick = {
                            chooseOpenFile(strings["label.logViewerOpenCsvTitle"])?.let { file ->
                                currentLogPath = file.absolutePath
                                currentLogLabel = file.name
                                analyzeSelected(file.absolutePath)
                            }
                        },
                        enabled = !isLoading,
                    ) {
                        Text(strings["label.virtualDynoOpenCsv"])
                    }
                    Button(
                        onClick = { analyzeSelected(currentLogPath) },
                        enabled = currentLogPath != null && !isLoading,
                    ) {
                        Text(strings["label.virtualDynoAnalyze"])
                    }
                }
                if (lastSavedLogPath != null) {
                    Text(
                        strings.format("label.virtualDynoLastSavedLog", lastSavedLogPath.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        OutlinedButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) strings["label.virtualDynoAdvancedToggleClose"] else strings["label.virtualDynoAdvancedToggleOpen"])
        }

        if (showAdvanced) {
            VehicleSpecsCardDesktop(
                vehicleName = vehicleName,
                onVehicleNameChange = { vehicleName = it },
                weightKg = weightKg,
                onWeightKgChange = { weightKg = it },
                dragCoefficient = dragCoefficient,
                onDragCoefficientChange = { dragCoefficient = it },
                frontalAreaM2 = frontalAreaM2,
                onFrontalAreaM2Change = { frontalAreaM2 = it },
                correctedFrontalAreaM2 = correctedFrontalAreaM2,
                onCorrectedFrontalAreaM2Change = { correctedFrontalAreaM2 = it },
                drivetrain = drivetrain,
                onDrivetrainChange = { drivetrain = it },
                analyzedGearRatio = analyzedGearRatio,
                onAnalyzedGearRatioChange = { analyzedGearRatio = it },
                differentialRatio = differentialRatio,
                onDifferentialRatioChange = { differentialRatio = it },
                wheelTireDiameterCm = wheelTireDiameterCm,
                onWheelTireDiameterCmChange = { wheelTireDiameterCm = it },
            )
        }

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text(strings["label.virtualDynoAnalyzing"])
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        result?.let { analysis ->
            val curve = analysis.buildBestCurve()
            ResultCardDesktop(analysis)
            CurveSummaryCardDesktop(curve)
            PowerTorqueChartDesktop(curve)
            FeedbackCardDesktop(
                expectedPower = expectedPower,
                onExpectedPowerChange = { expectedPower = it },
                notes = notes,
                onNotesChange = { notes = it },
                consent = consent,
                onConsentChange = { consent = it },
                onExportReport = {
                    val sourceFile = currentLogPath?.let(::File)
                    val reportFile = exportVirtualDynoReport(
                        analysis = analysis,
                        expectedPower = expectedPower,
                        notes = notes,
                        sourceFile = sourceFile,
                    )
                    feedbackStatus = strings.format("label.virtualDynoLogSaved", reportFile.absolutePath)
                },
                onOpenMailDraft = {
                    openVirtualDynoMailDraft(
                        analysis = analysis,
                        expectedPower = expectedPower,
                        notes = notes,
                    )
                    feedbackStatus = strings["label.virtualDynoMailDraftOpened"]
                },
            )
        }

        feedbackStatus?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun VehicleSpecsCardDesktop(
    vehicleName: String,
    onVehicleNameChange: (String) -> Unit,
    weightKg: String,
    onWeightKgChange: (String) -> Unit,
    dragCoefficient: String,
    onDragCoefficientChange: (String) -> Unit,
    frontalAreaM2: String,
    onFrontalAreaM2Change: (String) -> Unit,
    correctedFrontalAreaM2: String,
    onCorrectedFrontalAreaM2Change: (String) -> Unit,
    drivetrain: String,
    onDrivetrainChange: (String) -> Unit,
    analyzedGearRatio: String,
    onAnalyzedGearRatioChange: (String) -> Unit,
    differentialRatio: String,
    onDifferentialRatioChange: (String) -> Unit,
    wheelTireDiameterCm: String,
    onWheelTireDiameterCmChange: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val corrected = correctedFrontalAreaM2.parseOptionalDouble()
        ?: dragCoefficient.parseOptionalDouble()?.let { cd -> frontalAreaM2.parseOptionalDouble()?.let { area -> cd * area } }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(strings["label.virtualDynoAdvancedData"], style = MaterialTheme.typography.titleMedium)
            Text(
                "Se o CdA estiver vazio, o desktop calcula Cx x área frontal. Relações de marcha e roda/pneu ainda entram só no relatório.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(vehicleName, onVehicleNameChange, label = { Text(strings["label.virtualDynoVehicle"]) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(weightKg, onWeightKgChange, label = { Text(strings["label.virtualDynoWeightKg"]) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(dragCoefficient, onDragCoefficientChange, label = { Text(strings["label.virtualDynoDragCoeff"]) }, modifier = Modifier.weight(1f))
                OutlinedTextField(frontalAreaM2, onFrontalAreaM2Change, label = { Text(strings["label.virtualDynoFrontalArea"]) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                correctedFrontalAreaM2,
                onCorrectedFrontalAreaM2Change,
                label = { Text(strings["label.virtualDynoCorrectedCda"]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(strings.format("label.virtualDynoCdAUsed", corrected?.format3() ?: strings["label.virtualDynoNoCdA"]), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(drivetrain, onDrivetrainChange, label = { Text(strings["label.virtualDynoTraction"]) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(analyzedGearRatio, onAnalyzedGearRatioChange, label = { Text(strings["label.virtualDynoGearRatio"]) }, modifier = Modifier.weight(1f))
                OutlinedTextField(differentialRatio, onDifferentialRatioChange, label = { Text(strings["label.virtualDynoDiffRatio"]) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                wheelTireDiameterCm,
                onWheelTireDiameterCmChange,
                label = { Text(strings["label.virtualDynoWheelDiameter"]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResultCardDesktop(result: VirtualDynoAnalysisResult) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings["label.virtualDynoResult"], style = MaterialTheme.typography.titleLarge)
            Text(strings.format("label.virtualDynoPeakWhp", result.peakPowerWhp), style = MaterialTheme.typography.headlineSmall)
            Text(strings.format("label.virtualDynoPeakTorque", result.peakTorqueNm), style = MaterialTheme.typography.headlineSmall)
            Text(strings.format("label.virtualDynoAvg", result.averagePowerWhp, result.averageTorqueNm))
            Text(strings.format("label.virtualDynoBand", result.powerBand))
            Text(strings.format("label.virtualDynoQuality", qualityLabel(result.quality)))
            Text(strings.format("label.virtualDynoSamples", result.inferredRows, result.usefulRows, result.totalRows))
            if (result.vehicleSpecs.hasAnyValue()) {
                Text(strings.format("label.virtualDynoSpecsApplied", result.vehicleSpecs.vehicleName.ifBlank { "veículo informado" }))
                result.vehicleSpecs.weightKg?.let { Text(strings.format("label.virtualDynoWeightKg", it)) }
                result.vehicleSpecs.effectiveCorrectedFrontalAreaM2?.let { Text(strings.format("label.virtualDynoCorrectedCda", it)) }
            }
        }
    }
}

@Composable
private fun CurveSummaryCardDesktop(curve: VirtualDynoCurve) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings["label.virtualDynoCurve"], style = MaterialTheme.typography.titleLarge)
            Text(
                "Média por faixas de RPM usando pontos com TPS >= ${curve.tpsThreshold.format1()}%. A curva é sintetizada a partir da log, não de uma única puxada recortada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (curve.points.size < 2) {
                Text(strings["label.virtualDynoCurveInsufficient"], color = MaterialTheme.colorScheme.error)
            } else {
                Text(strings.format("label.virtualDynoCurvePeakPower", curve.peakPower?.powerWhp ?: 0.0, curve.peakPower?.rpm ?: 0.0))
                Text(strings.format("label.virtualDynoCurvePeakTorque", curve.peakTorque?.torqueNm ?: 0.0, curve.peakTorque?.rpm ?: 0.0))
                Text(strings.format("label.virtualDynoCurvePoints", curve.points.size))
            }
        }
    }
}

@Composable
private fun PowerTorqueChartDesktop(curve: VirtualDynoCurve) {
    if (curve.points.size < 2) return
    val strings = LocalStrings.current
    val powerColor = MaterialTheme.colorScheme.primary
    val torqueColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val points = curve.points
    val minRpm = 500.0
    val maxRpm = points.maxOf { it.rpm }.coerceAtLeast(5500.0)
    val maxPower = points.maxOf { it.powerWhp }.coerceAtLeast(1.0).niceAxisMax()
    val maxTorque = points.maxOf { it.torqueNm }.coerceAtLeast(1.0).niceAxisMax()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(strings["label.virtualDynoPowerAxis"], color = powerColor, style = MaterialTheme.typography.labelLarge)
                Text(strings["label.virtualDynoTorqueAxis"], color = torqueColor, style = MaterialTheme.typography.labelLarge)
            }
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val left = 28f
                    val right = size.width - 28f
                    val top = 20f
                    val bottom = size.height - 24f
                    fun xFor(rpm: Double): Float {
                        val span = (maxRpm - minRpm).coerceAtLeast(1.0)
                        return left + ((rpm - minRpm) / span).toFloat() * (right - left)
                    }
                    fun yForPower(value: Double): Float = bottom - (value / maxPower).toFloat() * (bottom - top)
                    fun yForTorque(value: Double): Float = bottom - (value / maxTorque).toFloat() * (bottom - top)

                    repeat(5) { step ->
                        val fraction = step / 4f
                        val y = bottom - fraction * (bottom - top)
                        drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
                    }
                    drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 2f)
                    drawLine(axisColor, Offset(right, top), Offset(right, bottom), strokeWidth = 2f)
                    drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2f)

                    val powerPath = Path()
                    val torquePath = Path()
                    points.forEachIndexed { index, point ->
                        val x = xFor(point.rpm)
                        val powerY = yForPower(point.powerWhp)
                        val torqueY = yForTorque(point.torqueNm)
                        if (index == 0) {
                            powerPath.moveTo(x, powerY)
                            torquePath.moveTo(x, torqueY)
                        } else {
                            powerPath.lineTo(x, powerY)
                            torquePath.lineTo(x, torqueY)
                        }
                    }
                    drawPath(powerPath, powerColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    drawPath(torquePath, torqueColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings["label.virtualDynoRpmStart"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.format("label.virtualDynoRpmEnd", maxRpm.format0()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.format("label.virtualDynoPowerAxisRange", maxPower.format0()), style = MaterialTheme.typography.bodySmall, color = powerColor)
                Text(strings.format("label.virtualDynoTorqueAxisRange", maxTorque.format0()), style = MaterialTheme.typography.bodySmall, color = torqueColor)
            }
        }
    }
}

@Composable
private fun FeedbackCardDesktop(
    expectedPower: String,
    onExpectedPowerChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    consent: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onExportReport: () -> Unit,
    onOpenMailDraft: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(strings["label.virtualDynoFeedbackBeta"], style = MaterialTheme.typography.titleMedium)
            Text(
                "No desktop o relatório é exportado para .txt. Depois disso, você pode abrir um rascunho de e-mail e anexar manualmente o CSV e o relatório.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = expectedPower,
                onValueChange = onExpectedPowerChange,
                label = { Text(strings["label.virtualDynoExpectedPower"]) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(strings["label.virtualDynoNotes"]) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = onConsentChange)
                Text(strings["label.virtualDynoFeedbackAccept"])
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onExportReport,
                    enabled = consent && expectedPower.isNotBlank(),
                ) {
                    Text(strings["label.virtualDynoExportReport"])
                }
                OutlinedButton(
                    onClick = onOpenMailDraft,
                    enabled = consent && expectedPower.isNotBlank(),
                ) {
                    Text(strings["label.virtualDynoOpenMail"])
                }
            }
        }
    }
}

private fun exportVirtualDynoReport(
    analysis: VirtualDynoAnalysisResult,
    expectedPower: String,
    notes: String,
    sourceFile: File?,
): File {
    val reportDir = File(System.getProperty("user.home"), "SpeeduinoManagerDesktop/virtual_dyno_reports").also { it.mkdirs() }
    val safeName = (sourceFile?.name ?: analysis.fileLabel ?: "log").replace(Regex("[^A-Za-z0-9._-]"), "_")
    val reportFile = File(reportDir, "virtual_dyno_${safeName}_${System.currentTimeMillis()}.txt")
    reportFile.writeText(analysis.toFeedbackText(expectedPower, notes))
    return reportFile
}

private fun openVirtualDynoMailDraft(
    analysis: VirtualDynoAnalysisResult,
    expectedPower: String,
    notes: String,
) {
    if (!Desktop.isDesktopSupported()) return
    val subject = "Virtual Dyno feedback - ${analysis.fileLabel ?: "log"}"
    val body = analysis.toFeedbackText(expectedPower, notes)
    val mailto = URI(
        "mailto:alexandrefelipemuller@gmail.com?subject=${URLEncoder.encode(subject, StandardCharsets.UTF_8.name())}&body=${URLEncoder.encode(body, StandardCharsets.UTF_8.name())}"
    )
    runCatching { Desktop.getDesktop().mail(mailto) }
}

private fun qualityLabel(quality: VirtualDynoQuality): String = when (quality) {
    VirtualDynoQuality.GOOD -> "boa"
    VirtualDynoQuality.USABLE -> "utilizável"
    VirtualDynoQuality.BAD -> "fraca"
}
