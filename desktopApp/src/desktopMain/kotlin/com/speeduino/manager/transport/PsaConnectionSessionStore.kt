package com.speeduino.manager.transport

import com.speeduino.manager.desktop.DesktopSettingsStore
import java.io.File
import java.util.Properties

class PsaConnectionSessionStore(
    private val baseDir: File = DesktopSettingsStore.settingsDir(),
) : PsaSessionStore {
    private val file = baseDir.resolve("psa_sessions.properties")

    override fun load(): PsaPersistedSession? {
        val raw = loadProperties().getProperty(KEY_LAST_SESSION) ?: return null
        val parts = raw.split("|")
        if (parts.size < 9) return null

        return runCatching {
            PsaPersistedSession(
                protocol = parts[0],
                txId = parts[1],
                rxId = parts[2],
                hintsCsv = parts[3],
                oemProfileId = parts[4],
                functionalHeader = parts[5],
                c4LiveModeEnabled = parts[6] == "1",
                isFunctional = parts[7] == "1",
                timestampMs = parts[8].toLongOrNull() ?: 0L,
            )
        }.getOrNull()
    }

    override fun save(session: PsaPersistedSession) {
        val payload = buildString {
            append(session.protocol)
            append('|').append(session.txId)
            append('|').append(session.rxId)
            append('|').append(session.hintsCsv)
            append('|').append(session.oemProfileId)
            append('|').append(session.functionalHeader)
            append('|').append(if (session.c4LiveModeEnabled) "1" else "0")
            append('|').append(if (session.isFunctional) "1" else "0")
            append('|').append(session.timestampMs)
        }
        val properties = loadProperties()
        properties.setProperty(KEY_LAST_SESSION, payload)
        storeProperties(properties)
    }

    override fun clear() {
        val properties = loadProperties()
        properties.remove(KEY_LAST_SESSION)
        storeProperties(properties)
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
            properties.store(output, "SpeeduinoManager Desktop PSA sessions")
        }
    }

    private companion object {
        const val KEY_LAST_SESSION = "last_session"
    }
}
