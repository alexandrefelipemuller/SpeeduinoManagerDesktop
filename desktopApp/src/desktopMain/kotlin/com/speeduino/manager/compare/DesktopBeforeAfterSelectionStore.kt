package com.speeduino.manager.compare

import com.speeduino.manager.desktop.DesktopSettingsStore
import java.util.Properties

internal data class DesktopBeforeAfterSelection(
    val mode: String? = null,
    val beforeLogPath: String? = null,
    val afterLogPath: String? = null,
)

internal class DesktopBeforeAfterSelectionStore {
    private val file = DesktopSettingsStore.settingsDir().resolve("before_after_selection.properties")

    fun save(selection: DesktopBeforeAfterSelection) {
        val props = Properties().apply {
            selection.mode?.let { setProperty(KEY_MODE, it) }
            selection.beforeLogPath?.let { setProperty(KEY_BEFORE, it) }
            selection.afterLogPath?.let { setProperty(KEY_AFTER, it) }
        }
        file.outputStream().use { props.store(it, "Speeduino Manager Desktop before/after selection") }
    }

    fun load(): DesktopBeforeAfterSelection {
        if (!file.exists()) return DesktopBeforeAfterSelection()
        val props = Properties().apply { file.inputStream().use(::load) }
        return DesktopBeforeAfterSelection(
            mode = props.getProperty(KEY_MODE)?.takeIf { it.isNotBlank() },
            beforeLogPath = props.getProperty(KEY_BEFORE)?.takeIf { it.isNotBlank() },
            afterLogPath = props.getProperty(KEY_AFTER)?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_BEFORE = "before_log"
        const val KEY_AFTER = "after_log"
    }
}
