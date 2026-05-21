package com.speeduino.manager.ecu

import com.speeduino.manager.model.EcuCapabilities
import com.speeduino.manager.model.EcuFamily
import com.speeduino.manager.model.FirmwareEra
import com.speeduino.manager.model.UnsupportedFirmwareException
import java.util.Locale

data class FirmwareConsensus(
    val signature: String?,
    val consensusHits: Int,
)

object FirmwareHandshakeDomain {
    fun sanitizeSignature(raw: String): String {
        val asciiOnly = raw.map { ch ->
            when {
                ch == '\t' || ch == '\n' || ch == '\r' -> ' '
                ch in ' '..'~' -> ch
                else -> ' '
            }
        }.joinToString("")

        return asciiOnly
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalizeSignature(signature: String): String? {
        val sanitized = sanitizeSignature(signature)
        if (sanitized.isBlank()) return null

        Regex("""(?i)^speeduino\s+(\d{6})(?:\.\d+)?$""")
            .find(sanitized)
            ?.let { return "speeduino ${it.groupValues[1]}" }

        Regex("""(?i)^speeduino\s+(\d{4})\.(\d{2})$""")
            .find(sanitized)
            ?.let { return "speeduino ${it.groupValues[1]}${it.groupValues[2]}" }

        Regex("""(?i)^ms3\s*format\s*([0-9]{4}\.[0-9]{2}[a-z]?)$""")
            .find(sanitized)
            ?.let { return "MS3 Format ${it.groupValues[1].uppercase(Locale.US)}" }

        Regex("""(?i)^ms2extra\s+comms([0-9a-z]+)$""")
            .find(sanitized)
            ?.let { return "MS2Extra comms${it.groupValues[1].lowercase(Locale.US)}" }

        Regex("""(?i)^ms2extra\s+megaspeed(?:\s+.*)?$""")
            .find(sanitized)
            ?.let { return "MS2Extra MegaSpeed" }

        Regex("""(?i)^ms2extra\s+hr[_\s-]?10(?:\s+.*)?$""")
            .find(sanitized)
            ?.let { return "MS2Extra hr_10" }

        Regex("""(?i)^ms2extra\s+hr[_\s-]?11d(?:\s+.*)?$""")
            .find(sanitized)
            ?.let { return "MS2Extra hr_11d" }

        if (sanitized.startsWith("rusEFI", ignoreCase = true)) {
            return sanitized
        }

        val hasDuinoToken = Regex("""(?i)\b[a-z]*duino\b""").containsMatchIn(sanitized)
        val v6 = Regex("""\b(20\d{4})\b""").find(sanitized)?.groupValues?.getOrNull(1)
        if (hasDuinoToken && !v6.isNullOrBlank()) {
            return "speeduino $v6"
        }

        val dotted = Regex("""\b(20\d{2})\.(\d{2})\b""").find(sanitized)
        if (hasDuinoToken && dotted != null) {
            return "speeduino ${dotted.groupValues[1]}${dotted.groupValues[2]}"
        }

        return null
    }

    fun resolveConsensus(samples: List<String>): FirmwareConsensus {
        val normalized = samples.mapNotNull(::normalizeSignature)
        if (normalized.isEmpty()) {
            return FirmwareConsensus(signature = null, consensusHits = 0)
        }

        val grouped = normalized.groupingBy { it }.eachCount()
        val best = grouped.maxByOrNull { it.value }
        return FirmwareConsensus(
            signature = best?.key,
            consensusHits = best?.value ?: 0,
        )
    }

    fun selectBestCandidate(candidates: List<String>): String? {
        val sanitizedCandidates = candidates
            .map(::sanitizeSignature)
            .filter(String::isNotBlank)

        sanitizedCandidates.firstNotNullOfOrNull(::normalizeSignature)?.let { return it }
        return sanitizedCandidates.firstOrNull(::looksLikeFirmwareSample)
    }

    fun validateConsensus(consensus: FirmwareConsensus, samples: List<String>) {
        val signature = consensus.signature
        if (signature != null && consensus.consensusHits >= 1) {
            return
        }

        val errorMessage = buildString {
            appendLine("❌ Assinatura de firmware inválida/ilegível")
            appendLine()
            appendLine("Amostras recebidas:")
            samples.forEach { sample ->
                appendLine("- $sample")
            }
            appendLine()
            appendLine("Isso costuma indicar problema no canal (ruído, baud incorreto, cabo/adaptador, timeout).")
            append("Tente reconectar e verificar a conexão.")
        }
        throw UnsupportedFirmwareException(errorMessage)
    }

    fun normalizeManualProfile(signature: String): String {
        return normalizeSignature(signature)
            ?: throw UnsupportedFirmwareException("Invalid manual firmware profile: $signature")
    }

    fun approximateSpeeduinoSignature(raw: String): String? {
        val sanitized = sanitizeSignature(raw)
        if (sanitized.isBlank()) return null
        val lower = sanitized.lowercase(Locale.US)
        if (!lower.contains("speeduino")) return null
        if (normalizeSignature(sanitized) != null) return normalizeSignature(sanitized)
        val version = Regex("""\b(20\d{4})\b""").find(sanitized)?.groupValues?.getOrNull(1)
        return version?.let { "speeduino $it" }
    }

    fun containsApproximateToken(raw: String): Boolean {
        val lower = sanitizeSignature(raw).lowercase(Locale.US)
        return lower.contains("speeduino") || lower.contains("ms2extra") || lower.contains("rusefi")
    }

    fun isRusEfiFallbackAllowed(signature: String?): Boolean {
        val sanitized = signature?.trim().orEmpty()
        return sanitized.isNotBlank() && sanitized.startsWith("rusEFI", ignoreCase = true)
    }

    fun shouldUseLegacyHandshakeCore(supportsModernProtocol: Boolean): Boolean = !supportsModernProtocol

    private fun looksLikeFirmwareSample(signature: String): Boolean {
        val lower = signature.lowercase(Locale.US)
        val hasVersionDigits = signature.count(Char::isDigit) >= 4
        return (lower.contains("speeduino") && hasVersionDigits) ||
            lower.startsWith("ms2extra ") ||
            lower.contains("ms3format") ||
            lower.startsWith("ms3 format ") ||
            lower.startsWith("rusefi ")
    }
}

data class FirmwareInfo(
    val signature: String,
    val productString: String,
    val era: FirmwareEra,
    val family: EcuFamily = EcuFamily.SPEEDUINO,
    val capabilities: EcuCapabilities = EcuCapabilities(
        supportsModernProtocol = true,
        supportsLegacyProtocol = true,
        supportsPageRead = true,
        supportsPageWrite = true,
        supportsBurn = true,
        supportsLiveData = true,
    ),
)
