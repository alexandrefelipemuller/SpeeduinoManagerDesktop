package com.speeduino.manager.sync

import com.speeduino.manager.ConfigDownloadResult
import com.speeduino.manager.ConfigManager
import com.speeduino.manager.SpeeduinoClient
import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.Page6Validator
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.model.VeTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.CRC32

data class SessionTablesSnapshot(
    val veTable: VeTable?,
    val veTable2: VeTable?,
    val ignitionTable: IgnitionTable?,
    val ignitionTable2: IgnitionTable?,
    val afrTable: AfrTable?,
    val engineConstants: EngineConstants?,
    val triggerSettings: TriggerSettings?,
)

data class SyncDecision(
    val sessionDir: File?,
    val prompt: SessionSyncPrompt?,
)

data class SessionSyncPrompt(
    val localSessionDir: File,
    val ecuSessionDir: File,
)

data class RestoreOutcome(
    val warnings: List<String>,
    val inconsistentPages: List<Byte>,
    val completed: Boolean,
)

class ConfigSyncService(
    private val configManager: ConfigManager,
) {
    suspend fun downloadAndResolveSync(
        client: SpeeduinoClient,
        localSessionDir: File?,
        onProgress: (Int, Int, String) -> Unit,
    ): Pair<ConfigDownloadResult, SyncDecision> = withContext(Dispatchers.IO) {
        val result = configManager.downloadAllConfigs(client, onProgress)
        if (!result.success || result.sessionDir == null) {
            return@withContext result to SyncDecision(sessionDir = null, prompt = null)
        }

        val ecuSessionDir = result.sessionDir
        if (localSessionDir == null) {
            return@withContext result to SyncDecision(sessionDir = ecuSessionDir, prompt = null)
        }

        val ecuSignature = sessionSignature(ecuSessionDir)
        val localSignature = sessionSignature(localSessionDir)
        if (ecuSignature != localSignature) {
            result to SyncDecision(
                sessionDir = null,
                prompt = SessionSyncPrompt(localSessionDir, ecuSessionDir),
            )
        } else {
            result to SyncDecision(sessionDir = ecuSessionDir, prompt = null)
        }
    }

    suspend fun loadTablesFromSession(sessionDir: File): SessionTablesSnapshot = withContext(Dispatchers.IO) {
        SessionTablesSnapshot(
            veTable = configManager.loadVeTable(sessionDir, 1),
            veTable2 = configManager.loadVeTable(sessionDir, 2),
            ignitionTable = configManager.loadIgnitionTable(sessionDir, 1),
            ignitionTable2 = configManager.loadIgnitionTable(sessionDir, 2),
            afrTable = configManager.loadAfrTable(sessionDir),
            engineConstants = configManager.loadEngineConstants(sessionDir),
            triggerSettings = configManager.loadTriggerSettings(sessionDir),
        )
    }

    suspend fun restoreConfigToEcu(
        client: SpeeduinoClient,
        sessionDir: File,
        skipPages: Set<Byte> = emptySet(),
        stopOnRangeErr: Boolean = false,
        applyTriggerFix: Boolean = false,
        restartStreamIntervalMs: Long? = null,
    ): RestoreOutcome = withContext(Dispatchers.IO) {
        val wasStreaming = client.isStreaming()
        if (wasStreaming) {
            client.pauseLiveDataStream()
        }

        val pages = configManager.loadConfig(sessionDir)
        if (pages.isEmpty()) {
            throw IllegalStateException("Backup sem páginas válidas")
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val inconsistentPages = mutableListOf<Byte>()
        var wroteAnyPage = false
        val restoreSkipPages = skipPages + 0.toByte()
        var stopDueToRangeErr = false

        pageLoop@ for ((pageNum, pageSize) in ConfigManager.PAGE_SIZES) {
            if (restoreSkipPages.contains(pageNum)) {
                val reason = when (pageNum.toInt()) {
                    0 -> "read-only/status"
                    else -> "compatibilidade"
                }
                warnings.add("Página $pageNum ignorada no restore ($reason)")
                continue
            }
            val data = pages[pageNum]
            if (data == null) {
                warnings.add("Página $pageNum ausente no backup")
                continue
            }
            if (data.size != pageSize) {
                errors.add("Página $pageNum com tamanho inesperado (${data.size} != $pageSize)")
                continue
            }

            var attempt = 0
            var success = false
            while (attempt < 3 && !success) {
                attempt++
                try {
                    client.writeRawPageWithoutBurn(pageNum, data)
                    wroteAnyPage = true
                    success = true
                } catch (e: Exception) {
                    val errorText = e.message ?: "Erro desconhecido"
                    if (errorText.contains("RANGE_ERR")) {
                        if (pageNum.toInt() == 6) {
                            val sanitized = Page6Validator.sanitize(data)
                            if (sanitized.changed) {
                                try {
                                    client.writeRawPageWithoutBurn(pageNum, sanitized.data)
                                    wroteAnyPage = true
                                    warnings.add("Página 6 corrigida (sanitização)")
                                    success = true
                                    break
                                } catch (sanitizeError: Exception) {
                                    warnings.add("Falha ao corrigir página 6: ${sanitizeError.message}")
                                }
                            }
                        }

                        warnings.add("Página $pageNum rejeitada (RANGE_ERR)")
                        if (stopOnRangeErr) {
                            inconsistentPages.add(pageNum)
                            stopDueToRangeErr = true
                            break
                        }
                        success = true
                        break
                    }
                    val message = "Falha ao gravar página $pageNum (tentativa $attempt): $errorText"
                    if (attempt >= 3) {
                        errors.add(message)
                    } else {
                        delay(400)
                    }
                }
            }
            if (stopDueToRangeErr) {
                break@pageLoop
            }
            delay(150)
        }

        if (!stopDueToRangeErr && applyTriggerFix) {
            val triggerPage = pages[TriggerSettings.PAGE_NUMBER.toByte()]
            if (triggerPage != null) {
                try {
                    val triggerSettings = TriggerSettings.fromPageData(triggerPage)
                    client.writeTriggerSettings(triggerSettings, burn = false)
                    wroteAnyPage = true
                    warnings.add("Página ${TriggerSettings.PAGE_NUMBER} corrigida (Trigger Settings)")
                } catch (e: Exception) {
                    warnings.add("Falha ao corrigir página ${TriggerSettings.PAGE_NUMBER}: ${e.message}")
                }
            } else {
                warnings.add("Página ${TriggerSettings.PAGE_NUMBER} ausente no backup")
            }
        }

        if (!stopDueToRangeErr && wroteAnyPage) {
            try {
                client.burnConfigs()
            } catch (e: Exception) {
                errors.add("Falha ao executar burn: ${e.message}")
            }
        }

        if (errors.isNotEmpty()) {
            throw IllegalStateException(errors.joinToString(" | "))
        }

        if (wasStreaming && restartStreamIntervalMs != null) {
            client.startLiveDataStream(restartStreamIntervalMs)
        }

        return@withContext RestoreOutcome(
            warnings = warnings,
            inconsistentPages = inconsistentPages,
            completed = !stopDueToRangeErr,
        )
    }

    suspend fun sessionSignature(sessionDir: File): Map<Byte, Long> = withContext(Dispatchers.IO) {
        val pages = runCatching { configManager.loadConfig(sessionDir) }.getOrDefault(emptyMap())
        pages.mapValues { (_, data) ->
            val crc = CRC32()
            crc.update(data)
            crc.value
        }
    }
}
