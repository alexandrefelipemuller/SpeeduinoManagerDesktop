package com.speeduino.manager.desktop

import com.speeduino.manager.definition.IniCatalogEntry
import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.definition.IniParser
import com.speeduino.manager.units.UnitSystem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties

internal data class ImportedIniDefinition(
    val fileName: String,
    val signature: String,
    val family: String,
)

internal enum class IniSelectionMode(val storageValue: String) {
    AUTOMATIC("automatic"),
    MANUAL("manual");

    companion object {
        fun fromStorage(value: String?): IniSelectionMode =
            entries.firstOrNull { it.storageValue == value } ?: AUTOMATIC
    }
}

internal enum class IniSelectionSource(val storageValue: String) {
    CATALOG("catalog"),
    IMPORTED("imported");

    companion object {
        fun fromStorage(value: String?): IniSelectionSource =
            entries.firstOrNull { it.storageValue == value } ?: CATALOG
    }
}

internal enum class AppProtocol(val storageValue: String) {
    MS_PROTOCOL("ms_protocol"),
    ELM327_OBD2("elm327_obd2");

    companion object {
        fun fromStorage(value: String?): AppProtocol =
            entries.firstOrNull { it.storageValue == value } ?: MS_PROTOCOL
    }
}

internal const val SHIFT_LIGHT_RPM_DEFAULT = 6000
internal const val SHIFT_LIGHT_RPM_MIN = 3000
internal const val SHIFT_LIGHT_RPM_MAX = 8000

internal data class DesktopSettingsState(
    val unitSystem: UnitSystem = UnitSystem.AUTO,
    val autoConnectOnStart: Boolean = false,
    val shiftLightRpm: Int = SHIFT_LIGHT_RPM_DEFAULT,
    val protocol: AppProtocol = AppProtocol.MS_PROTOCOL,
    val iniSelectionMode: IniSelectionMode = IniSelectionMode.AUTOMATIC,
    val iniSelectionSource: IniSelectionSource = IniSelectionSource.CATALOG,
    val iniDefinitionId: String? = null,
    val manualFirmwareProfile: String? = null,
    val lastConnectionType: ConnectionType? = null,
    val lastTcpHost: String? = null,
    val lastTcpPort: Int? = null,
    val lastSerialPort: String? = null,
    val lastSerialBaudRate: Int? = null,
)

internal data class ManualFirmwareProfileOption(
    val signature: String,
    val label: String,
)

internal class DesktopDefinitionRepository(
    private val baseDir: File = DesktopSettingsStore.settingsDir().resolve("ecu_definitions"),
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    companion object {
        private const val DEFAULT_BASE_URL = "https://speeduinomanager.web.app/ecu-definitions/"
        private const val MANIFEST_FILE = "manifest.json"
    }

    private val catalogDir = baseDir.resolve("catalog").apply { mkdirs() }
    private val definitionsDir = baseDir.resolve("definitions").apply { mkdirs() }
    private val cachedManifestFile = catalogDir.resolve(MANIFEST_FILE)

    fun loadCatalog(forceRefresh: Boolean = false): List<IniCatalogEntry> {
        if (forceRefresh || !cachedManifestFile.exists()) {
            refreshCatalog()
        }
        if (!cachedManifestFile.exists()) return emptyList()
        return parseCatalog(cachedManifestFile.readText())
    }

    fun refreshCatalog(): List<IniCatalogEntry> {
        val content = httpGet("${baseUrl.trimEnd('/')}/$MANIFEST_FILE")
        cachedManifestFile.writeText(content)
        return parseCatalog(content)
    }

    fun findMatchingEntry(signature: String, forceCatalogRefresh: Boolean = false): IniCatalogEntry? {
        val entries = loadCatalog(forceRefresh = forceCatalogRefresh)
        return entries
            .filter { matchesSignature(signature, it.signaturePattern) }
            .maxByOrNull { it.priority }
    }

    fun downloadDefinition(entry: IniCatalogEntry, forceRefresh: Boolean = false): File {
        val target = definitionsDir.resolve("${entry.id}.ini")
        if (target.exists() && !forceRefresh) {
            val currentHash = sha256(target)
            if (currentHash.equals(entry.sha256, ignoreCase = true)) {
                return target
            }
        }

        val content = httpGet(entry.url)
        target.writeText(content)
        val downloadedHash = sha256(target)
        require(downloadedHash.equals(entry.sha256, ignoreCase = true)) {
            "Hash invalido para ${entry.id}: esperado ${entry.sha256}, recebido $downloadedHash"
        }
        return target
    }

    fun loadDefinition(entry: IniCatalogEntry, forceRefresh: Boolean = false): IniDefinition {
        val file = downloadDefinition(entry, forceRefresh)
        return IniParser.parse(file.name, file.readText())
    }

    fun importDefinition(source: File): IniDefinition {
        require(source.exists()) { "Arquivo nao encontrado: ${source.absolutePath}" }
        val target = definitionsDir.resolve(source.name)
        source.copyTo(target, overwrite = true)
        return IniParser.parse(target.name, target.readText())
    }

    fun listImportedDefinitions(): List<ImportedIniDefinition> {
        return definitionsDir.listFiles { file ->
            file.isFile && file.extension.equals("ini", ignoreCase = true)
        }?.sortedBy { it.name.lowercase(Locale.US) }
            ?.mapNotNull { file ->
                runCatching {
                    val definition = IniParser.parse(file.name, file.readText())
                    ImportedIniDefinition(
                        fileName = file.name,
                        signature = definition.signature,
                        family = definition.family,
                    )
                }.getOrNull()
            }
            ?: emptyList()
    }

    fun loadImportedDefinition(fileName: String): IniDefinition {
        val target = definitionsDir.resolve(fileName)
        require(target.exists()) { "Arquivo .ini importado nao encontrado: $fileName" }
        return IniParser.parse(target.name, target.readText())
    }

    fun loadCachedDefinitionById(id: String): IniDefinition {
        val target = definitionsDir.resolve("$id.ini")
        require(target.exists()) { "Definicao .ini em cache nao encontrada: $id" }
        return IniParser.parse(target.name, target.readText())
    }

    fun hasCachedDefinitionById(id: String): Boolean = definitionsDir.resolve("$id.ini").exists()

    fun isDefinitionCached(entry: IniCatalogEntry): Boolean {
        val target = definitionsDir.resolve("${entry.id}.ini")
        return target.exists() && sha256(target).equals(entry.sha256, ignoreCase = true)
    }

    private fun parseCatalog(content: String): List<IniCatalogEntry> {
        val json = JSONObject(content)
        val array = json.optJSONArray("definitions") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    IniCatalogEntry(
                        id = item.getString("id"),
                        family = item.getString("family"),
                        board = item.optString("board", "generic"),
                        signaturePattern = item.getString("signature_pattern"),
                        version = item.optString("version", "unknown"),
                        url = item.getString("url"),
                        sha256 = item.getString("sha256"),
                        priority = item.optInt("priority", 100),
                        bundled = item.optBoolean("bundled", false),
                        minAppVersion = if (item.has("min_app_version")) item.getInt("min_app_version") else null,
                    )
                )
            }
        }
    }

    private fun matchesSignature(signature: String, pattern: String): Boolean {
        val wildcardRegex = pattern.trim().split('*').joinToString(".*") { Regex.escape(it) }
        return Regex("^$wildcardRegex$", RegexOption.IGNORE_CASE).matches(signature.trim())
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json, text/plain, */*")
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

internal object DesktopSettingsStore {
    private const val SETTINGS_FILE = "settings.properties"
    private const val INI_CACHE_FILE = "ini_cache.properties"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_UNIT_SYSTEM = "unit_system"
    private const val KEY_AUTO_CONNECT_ON_START = "auto_connect_on_start"
    private const val KEY_SHIFT_LIGHT_RPM = "shift_light_rpm"
    private const val KEY_INI_SELECTION_MODE = "ini_selection_mode"
    private const val KEY_INI_SELECTION_SOURCE = "ini_selection_source"
    private const val KEY_INI_DEFINITION_ID = "ini_definition_id"
    private const val KEY_MANUAL_FIRMWARE_PROFILE = "manual_firmware_profile"
    private const val KEY_PROTOCOL = "protocol"
    private const val KEY_LAST_CONNECTION_TYPE = "last_connection_type"
    private const val KEY_LAST_TCP_HOST = "last_tcp_host"
    private const val KEY_LAST_TCP_PORT = "last_tcp_port"
    private const val KEY_LAST_SERIAL_PORT = "last_serial_port"
    private const val KEY_LAST_SERIAL_BAUD_RATE = "last_serial_baud_rate"

    fun settingsDir(): File = File(System.getProperty("user.home"), ".speeduino-manager-desktop").apply { mkdirs() }

    private fun settingsFile(): File = settingsDir().resolve(SETTINGS_FILE)
    private fun iniCacheFile(): File = settingsDir().resolve(INI_CACHE_FILE)

    private fun loadProperties(file: File): Properties {
        val properties = Properties()
        if (file.exists()) {
            runCatching {
                file.inputStream().use(properties::load)
            }
        }
        return properties
    }

    private fun storeProperties(file: File, properties: Properties) {
        file.outputStream().use { output ->
            properties.store(output, "SpeeduinoManager Desktop settings")
        }
    }

    fun loadLanguage(): AppLanguage {
        return AppLanguage.fromCode(loadProperties(settingsFile()).getProperty(KEY_LANGUAGE))
    }

    fun saveLanguage(language: AppLanguage) {
        val properties = loadProperties(settingsFile())
        properties.setProperty(KEY_LANGUAGE, language.code)
        storeProperties(settingsFile(), properties)
    }

    fun loadSettings(): DesktopSettingsState {
        val properties = loadProperties(settingsFile())
        val unitSystem = UnitSystem.fromStorage(properties.getProperty(KEY_UNIT_SYSTEM))
        val autoConnectOnStart = properties.getProperty(KEY_AUTO_CONNECT_ON_START)?.equals("true", ignoreCase = true) ?: false
        val shiftLightRpm = properties.getProperty(KEY_SHIFT_LIGHT_RPM)?.toIntOrNull()
            ?.coerceIn(SHIFT_LIGHT_RPM_MIN, SHIFT_LIGHT_RPM_MAX)
            ?: SHIFT_LIGHT_RPM_DEFAULT
        val protocol = AppProtocol.fromStorage(properties.getProperty(KEY_PROTOCOL))
        val mode = IniSelectionMode.fromStorage(properties.getProperty(KEY_INI_SELECTION_MODE))
        val source = IniSelectionSource.fromStorage(properties.getProperty(KEY_INI_SELECTION_SOURCE))
        val definitionId = properties.getProperty(KEY_INI_DEFINITION_ID)?.takeIf { it.isNotBlank() }
        val manualFirmwareProfile = properties.getProperty(KEY_MANUAL_FIRMWARE_PROFILE)?.takeIf { it.isNotBlank() }
        val lastConnectionType = properties.getProperty(KEY_LAST_CONNECTION_TYPE)?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ConnectionType.valueOf(it) }.getOrNull() }
        val lastTcpHost = properties.getProperty(KEY_LAST_TCP_HOST)?.takeIf { it.isNotBlank() }
        val lastTcpPort = properties.getProperty(KEY_LAST_TCP_PORT)?.toIntOrNull()
        val lastSerialPort = properties.getProperty(KEY_LAST_SERIAL_PORT)?.takeIf { it.isNotBlank() }
        val lastSerialBaudRate = properties.getProperty(KEY_LAST_SERIAL_BAUD_RATE)?.toIntOrNull()
        return DesktopSettingsState(
            unitSystem = unitSystem,
            autoConnectOnStart = autoConnectOnStart,
            shiftLightRpm = shiftLightRpm,
            protocol = protocol,
            iniSelectionMode = mode,
            iniSelectionSource = if (mode == IniSelectionMode.MANUAL) source else IniSelectionSource.CATALOG,
            iniDefinitionId = if (mode == IniSelectionMode.MANUAL) definitionId else null,
            manualFirmwareProfile = manualFirmwareProfile,
            lastConnectionType = lastConnectionType,
            lastTcpHost = lastTcpHost,
            lastTcpPort = lastTcpPort,
            lastSerialPort = lastSerialPort,
            lastSerialBaudRate = lastSerialBaudRate,
        )
    }

    fun saveSettings(settings: DesktopSettingsState) {
        val properties = loadProperties(settingsFile())
        properties.setProperty(KEY_UNIT_SYSTEM, settings.unitSystem.storageValue)
        properties.setProperty(KEY_AUTO_CONNECT_ON_START, settings.autoConnectOnStart.toString())
        properties.setProperty(KEY_SHIFT_LIGHT_RPM, settings.shiftLightRpm.coerceIn(SHIFT_LIGHT_RPM_MIN, SHIFT_LIGHT_RPM_MAX).toString())
        properties.setProperty(KEY_PROTOCOL, settings.protocol.storageValue)
        properties.setProperty(KEY_INI_SELECTION_MODE, settings.iniSelectionMode.storageValue)
        properties.setProperty(KEY_INI_SELECTION_SOURCE, settings.iniSelectionSource.storageValue)
        if (settings.iniSelectionMode == IniSelectionMode.MANUAL && !settings.iniDefinitionId.isNullOrBlank()) {
            properties.setProperty(KEY_INI_DEFINITION_ID, settings.iniDefinitionId)
        } else {
            properties.remove(KEY_INI_DEFINITION_ID)
        }
        if (!settings.manualFirmwareProfile.isNullOrBlank()) {
            properties.setProperty(KEY_MANUAL_FIRMWARE_PROFILE, settings.manualFirmwareProfile)
        } else {
            properties.remove(KEY_MANUAL_FIRMWARE_PROFILE)
        }
        if (settings.lastConnectionType != null) {
            properties.setProperty(KEY_LAST_CONNECTION_TYPE, settings.lastConnectionType.name)
        } else {
            properties.remove(KEY_LAST_CONNECTION_TYPE)
        }
        if (!settings.lastTcpHost.isNullOrBlank()) {
            properties.setProperty(KEY_LAST_TCP_HOST, settings.lastTcpHost)
        } else {
            properties.remove(KEY_LAST_TCP_HOST)
        }
        if (settings.lastTcpPort != null) {
            properties.setProperty(KEY_LAST_TCP_PORT, settings.lastTcpPort.toString())
        } else {
            properties.remove(KEY_LAST_TCP_PORT)
        }
        if (!settings.lastSerialPort.isNullOrBlank()) {
            properties.setProperty(KEY_LAST_SERIAL_PORT, settings.lastSerialPort)
        } else {
            properties.remove(KEY_LAST_SERIAL_PORT)
        }
        if (settings.lastSerialBaudRate != null) {
            properties.setProperty(KEY_LAST_SERIAL_BAUD_RATE, settings.lastSerialBaudRate.toString())
        } else {
            properties.remove(KEY_LAST_SERIAL_BAUD_RATE)
        }
        storeProperties(settingsFile(), properties)
    }

    fun loadCachedRemoteIniId(signature: String): String? {
        val key = signature.trim().lowercase(Locale.US)
        return loadProperties(iniCacheFile()).getProperty(key)?.takeIf { it.isNotBlank() }
    }

    fun persistCachedRemoteIniId(signature: String, definitionId: String) {
        val properties = loadProperties(iniCacheFile())
        properties.setProperty(signature.trim().lowercase(Locale.US), definitionId)
        storeProperties(iniCacheFile(), properties)
    }
}

internal val MANUAL_FIRMWARE_PROFILES = listOf(
    ManualFirmwareProfileOption("speeduino 202501", "speeduino 202501 (latest / och 130)"),
    ManualFirmwareProfileOption("speeduino 202402", "speeduino 202402 (LTS / och 127)"),
    ManualFirmwareProfileOption("speeduino 202310", "speeduino 202310 (och 125)"),
    ManualFirmwareProfileOption("speeduino 202305", "speeduino 202305 (och 125)"),
    ManualFirmwareProfileOption("speeduino 202207", "speeduino 202207 (och 122)"),
    ManualFirmwareProfileOption("speeduino 202202", "speeduino 202202 (och 122)"),
    ManualFirmwareProfileOption("speeduino 202201", "speeduino 202201 (och 122)"),
    ManualFirmwareProfileOption("speeduino 202012", "speeduino 202012 (och 116)"),
    ManualFirmwareProfileOption("speeduino 202008", "speeduino 202008 (first modern / och 114)"),
    ManualFirmwareProfileOption("speeduino 201609", "speeduino 201609 (legacy / och 35)"),
    ManualFirmwareProfileOption("MS2Extra MegaSpeed", "MS2Extra MegaSpeed"),
    ManualFirmwareProfileOption("MS2Extra comms342h2", "MS2Extra comms342h2"),
    ManualFirmwareProfileOption("MS3 Format 0523.15", "MS3 Format 0523.15"),
    ManualFirmwareProfileOption(
        "rusEFI master.2026.03.02.proteus_f7.1679745342",
        "rusEFI proteus_f7 simulator"
    ),
)
