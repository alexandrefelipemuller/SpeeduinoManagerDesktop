package com.speeduino.manager.transport

import com.speeduino.manager.desktop.DesktopSettingsStore
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties

class Obd2OptimizationProfileStore(
    private val baseDir: File = DesktopSettingsStore.settingsDir(),
) : Obd2ProfileStore {
    private val file = baseDir.resolve("obd2_profiles.properties")

    override fun load(profileKey: String): Obd2PersistedProfile? {
        val hash = profileKeyHash(profileKey)
        val raw = loadProperties().getProperty("$KEY_PREFIX$hash") ?: return null
        val parts = raw.split("|")
        if (parts.size < 3) return null

        val mask = parts[0].toIntOrNull() ?: 0
        val supportedPids = parts[1]
            .split(",")
            .mapNotNull { token -> token.toIntOrNull(16) }
            .filter { it in 1..0xE0 }
            .toSet()
        val preferredO2Pid = parts[2].toIntOrNull(16)?.takeIf { it in 1..0xE0 }
        return Obd2PersistedProfile(
            mask = mask,
            supportedPids = supportedPids,
            preferredO2Pid = preferredO2Pid
        )
    }

    override fun save(profileKey: String, profile: Obd2PersistedProfile) {
        val hash = profileKeyHash(profileKey)
        val supportedPidsHex = profile.supportedPids
            .sorted()
            .joinToString(",") { pid -> "%02X".format(Locale.US, pid) }
        val preferredO2Hex = profile.preferredO2Pid?.let { "%02X".format(Locale.US, it) } ?: ""
        val payload = "${profile.mask}|$supportedPidsHex|$preferredO2Hex"
        val properties = loadProperties()
        properties.setProperty("$KEY_PREFIX$hash", payload)
        storeProperties(properties)
    }

    private fun profileKeyHash(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun loadProperties(): Properties {
        val properties = Properties()
        if (file.exists()) {
            runCatching { file.inputStream().use(properties::load) }
        }
        return properties
    }

    private fun storeProperties(properties: Properties) {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            properties.store(output, "SpeeduinoManager Desktop OBD2 profiles")
        }
    }

    private companion object {
        const val KEY_PREFIX = "profile_"
    }
}
