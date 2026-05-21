package com.speeduino.manager

import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.EcuFamily
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.protocol.SerialCapability
import com.speeduino.manager.transport.EcuTransport
import com.speeduino.manager.sync.SessionTableParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Gerenciador de configurações do Speeduino.
 *
 * Responsável por baixar, salvar e carregar configurações e mapas da ECU.
 */
class ConfigManager(baseDir: File = defaultBaseDir()) {

    companion object {
        // Tamanhos de páginas padrão (podem variar por firmware)
        val PAGE_SIZES = linkedMapOf(
            0.toByte() to 128,   // Page 0 - Configurações gerais / status
            1.toByte() to 128,   // Page 1 - Engine constants (algoritmo, req fuel, etc)
            2.toByte() to 288,   // Page 2 - VE Table (16x16 + eixos compactados)
            3.toByte() to 288,   // Page 3 - Ignition Table (16x16 + eixos)
            4.toByte() to 192,   // Page 4 - Ign settings / trigger
            5.toByte() to 288,   // Page 5 - AFR Target Table
            6.toByte() to 192,   // Page 6 - Boost/VVT (varia por firmware)
            7.toByte() to 192,   // Page 7 - Trim tables
            8.toByte() to 192,   // Page 8 - CAN config
            9.toByte() to 192,   // Page 9 - Warmup enrichment
            10.toByte() to 192   // Page 10 - Linear/misc config
        )

        const val CONFIG_DIR = "speeduino_configs"

        fun defaultBaseDir(): File {
            return File(System.getProperty("user.home"), "SpeeduinoManagerDesktop")
        }
    }

    private val configDir: File = File(baseDir, CONFIG_DIR).apply {
        if (!exists()) mkdirs()
    }

    /**
     * Baixa todas as configurações do Speeduino.
     */
    suspend fun downloadAllConfigs(
        client: EcuTransport,
        onProgress: (Int, Int, String) -> Unit
    ): ConfigDownloadResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val sessionDir = File(configDir, timestamp).apply { mkdirs() }

            // 1. Obter capacidades
            onProgress(0, PAGE_SIZES.size + 1, "Obtendo capacidades...")
            val capability = client.getSerialCapability()

            // Salvar informações gerais
            val infoFile = File(sessionDir, "info.txt")
            infoFile.writeText(
                """
                Speeduino Configuration Download
                Timestamp: $timestamp
                Protocol Version: ${capability.protocolVersion}
                Blocking Factor: ${capability.blockingFactor}
                Table Blocking Factor: ${capability.tableBlockingFactor}
                """.trimIndent()
            )

            val downloadedPages = mutableMapOf<Byte, PageData>()
            val failedPages = mutableListOf<Byte>()
            var currentPage = 0

            // 2. Baixar cada página
            for ((pageNum, pageSize) in PAGE_SIZES) {
                currentPage++
                onProgress(currentPage, PAGE_SIZES.size + 1, "Baixando página $pageNum ($pageSize bytes)...")

                try {
                    // CRC desabilitado - comando 'd' pode causar timeout em alguns firmwares
                    val crc = 0L

                    val blockSize = if (capability.blockingFactor > 0) {
                        capability.blockingFactor
                    } else {
                        64
                    }

                    val pageData = client.readFullPage(pageNum.toInt() and 0xFF, pageSize, blockSize)

                    val pageFile = File(sessionDir, "page_$pageNum.bin")
                    pageFile.writeBytes(pageData)

                    downloadedPages[pageNum] = PageData(pageNum, pageSize, crc, pageData)

                    val crcInfo = if (crc != 0L) " CRC: 0x${crc.toString(16)}" else ""
                    onProgress(
                        currentPage,
                        PAGE_SIZES.size + 1,
                        "Página $pageNum baixada (${pageData.size} bytes$crcInfo)"
                    )
                } catch (e: Exception) {
                    failedPages += pageNum
                    onProgress(
                        currentPage,
                        PAGE_SIZES.size + 1,
                        "Erro na página $pageNum: ${e.message}"
                    )
                }
            }

            onProgress(
                PAGE_SIZES.size + 1,
                PAGE_SIZES.size + 1,
                if (failedPages.isEmpty()) {
                    "Download concluído! ${downloadedPages.size} páginas salvas"
                } else {
                    "Download concluído com falhas: ${failedPages.joinToString(", ")}"
                }
            )

            ConfigDownloadResult(
                success = failedPages.isEmpty(),
                timestamp = timestamp,
                pagesDownloaded = downloadedPages.size,
                totalPages = PAGE_SIZES.size,
                sessionDir = sessionDir,
                capability = capability,
                pages = downloadedPages,
                error = if (failedPages.isEmpty()) null else "Páginas com falha: ${failedPages.joinToString(", ")}"
            )
        } catch (e: Exception) {
            ConfigDownloadResult(
                success = false,
                timestamp = "",
                pagesDownloaded = 0,
                totalPages = PAGE_SIZES.size,
                sessionDir = null,
                capability = null,
                pages = emptyMap(),
                error = e.message
            )
        }
    }

    fun listSavedConfigs(): List<File> {
        return configDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun latestSavedConfig(): File? {
        return listSavedConfigs().firstOrNull()
    }

    suspend fun loadConfig(sessionDir: File): Map<Byte, ByteArray> = withContext(Dispatchers.IO) {
        val pages = mutableMapOf<Byte, ByteArray>()

        for ((pageNum, _) in PAGE_SIZES) {
            val pageFile = File(sessionDir, "page_$pageNum.bin")
            if (pageFile.exists()) {
                pages[pageNum] = pageFile.readBytes()
            }
        }

        pages
    }

    private fun findPageFile(sessionDir: File, candidates: List<Int>): File? {
        for (page in candidates) {
            val file = File(sessionDir, "page_${page}.bin")
            if (file.exists()) {
                return file
            }
        }
        return null
    }

    private fun resolveEngineConstantsModel(sessionDir: File): EngineConstants? {
        val pageFile = findPageFile(sessionDir, listOf(1)) ?: return null
        return try {
            EngineConstants.fromPage1(pageFile.readBytes())
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun readSessionPagesInt(sessionDir: File): Map<Int, ByteArray> {
        return loadConfig(sessionDir).mapKeys { (page, _) -> page.toInt() and 0xFF }
    }

    private fun resolveSessionEcuFamily(sessionDir: File): EcuFamily {
        val infoFile = File(sessionDir, "info.txt")
        if (!infoFile.exists()) return EcuFamily.SPEEDUINO

        val familyLine = infoFile.readLines().firstOrNull { it.startsWith("ECU Family:", ignoreCase = true) }
            ?: return EcuFamily.SPEEDUINO
        val familyValue = familyLine.substringAfter(':').trim()
        return runCatching { EcuFamily.valueOf(familyValue) }.getOrDefault(EcuFamily.SPEEDUINO)
    }

    fun deleteConfig(sessionDir: File): Boolean {
        return sessionDir.deleteRecursively()
    }

    fun exportSessionToZip(sessionDir: File, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zip ->
            sessionDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                val entry = ZipEntry(file.name)
                zip.putNextEntry(entry)
                file.inputStream().use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
    }

    fun importSessionFromZip(inputStream: InputStream): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sessionDir = File(configDir, "${timestamp}_imported").apply { mkdirs() }
        val rootPath = sessionDir.canonicalPath

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(sessionDir, entry.name)
                val outPath = outFile.canonicalPath
                if (!outPath.startsWith(rootPath)) {
                    throw IllegalArgumentException("Entrada ZIP inválida: ${entry.name}")
                }
                if (!entry.isDirectory) {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return sessionDir
    }

    suspend fun loadVeTable(mapIndex: Int = 1): VeTable? = withContext(Dispatchers.IO) {
        val latestSession = listSavedConfigs().firstOrNull() ?: return@withContext null
        return@withContext loadVeTable(latestSession, mapIndex)
    }

    suspend fun loadVeTable(sessionDir: File, mapIndex: Int = 1): VeTable? = withContext(Dispatchers.IO) {
        val ecuFamily = resolveSessionEcuFamily(sessionDir)
        if (mapIndex == 1) {
            val pages = readSessionPagesInt(sessionDir)
            return@withContext SessionTableParser.parseVeTable(ecuFamily, pages)
        }

        val candidates = when (mapIndex) {
            2 -> listOf(7)
            3 -> listOf(8)
            4 -> listOf(9)
            else -> listOf(2, 1)
        }
        val pageFile = findPageFile(sessionDir, candidates) ?: return@withContext null
        val data = pageFile.readBytes()
        val constants = resolveEngineConstantsModel(sessionDir)
        return@withContext VeTable.fromPageData(
            data,
            loadType = com.speeduino.manager.tables.TableDomainFacade.resolveVeLoadType(constants),
        )
    }

    suspend fun loadIgnitionTable(mapIndex: Int = 1): IgnitionTable? = withContext(Dispatchers.IO) {
        val latestSession = listSavedConfigs().firstOrNull() ?: return@withContext null
        return@withContext loadIgnitionTable(latestSession, mapIndex)
    }

    suspend fun loadIgnitionTable(sessionDir: File, mapIndex: Int = 1): IgnitionTable? = withContext(Dispatchers.IO) {
        val ecuFamily = resolveSessionEcuFamily(sessionDir)
        if (mapIndex == 1) {
            val pages = readSessionPagesInt(sessionDir)
            return@withContext SessionTableParser.parseIgnitionTable(ecuFamily, pages)
        }

        val candidates = when (mapIndex) {
            2 -> listOf(10)
            3 -> listOf(11)
            4 -> listOf(12)
            else -> listOf(3)
        }
        val pageFile = findPageFile(sessionDir, candidates) ?: return@withContext null
        val data = pageFile.readBytes()
        val constants = resolveEngineConstantsModel(sessionDir)
        return@withContext IgnitionTable.fromPageData(
            data,
            loadType = com.speeduino.manager.tables.TableDomainFacade.resolveIgnitionLoadType(constants),
        )
    }

    suspend fun loadAfrTable(): AfrTable? = withContext(Dispatchers.IO) {
        val latestSession = listSavedConfigs().firstOrNull() ?: return@withContext null
        return@withContext loadAfrTable(latestSession)
    }

    suspend fun loadAfrTable(sessionDir: File): AfrTable? = withContext(Dispatchers.IO) {
        val ecuFamily = resolveSessionEcuFamily(sessionDir)
        val pages = readSessionPagesInt(sessionDir)
        SessionTableParser.parseAfrTable(ecuFamily, pages)
    }

    suspend fun loadEngineConstants(sessionDir: File): EngineConstants? = withContext(Dispatchers.IO) {
        val pageFile = findPageFile(sessionDir, listOf(1)) ?: return@withContext null
        return@withContext try {
            EngineConstants.fromPage1(pageFile.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadTriggerSettings(sessionDir: File): TriggerSettings? = withContext(Dispatchers.IO) {
        val pageFile = findPageFile(sessionDir, listOf(TriggerSettings.PAGE_NUMBER)) ?: return@withContext null
        return@withContext try {
            TriggerSettings.fromPageData(pageFile.readBytes())
        } catch (_: Exception) {
            null
        }
    }
}

data class PageData(
    val pageNum: Byte,
    val size: Int,
    val crc: Long,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageData

        if (pageNum != other.pageNum) return false
        if (size != other.size) return false
        if (crc != other.crc) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageNum.toInt()
        result = 31 * result + size
        result = 31 * result + crc.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class ConfigDownloadResult(
    val success: Boolean,
    val timestamp: String,
    val pagesDownloaded: Int,
    val totalPages: Int,
    val sessionDir: File?,
    val capability: SerialCapability?,
    val pages: Map<Byte, PageData>,
    val error: String? = null
)
