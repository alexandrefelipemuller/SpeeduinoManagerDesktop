package com.speeduino.manager.tuning

import com.speeduino.manager.desktop.DesktopSettingsStore
import java.util.Properties

internal data class DesktopTuningAssistantState(
    val logPath: String? = null,
    val strategy: String? = null,
)

internal class DesktopTuningAssistantStateStore {
    private val file = DesktopSettingsStore.settingsDir().resolve("tuning_assistant_state.properties")

    fun save(state: DesktopTuningAssistantState) {
        val props = Properties().apply {
            state.logPath?.let { setProperty(KEY_LOG_PATH, it) }
            state.strategy?.let { setProperty(KEY_STRATEGY, it) }
        }
        file.outputStream().use { props.store(it, "Speeduino Manager Desktop tuning assistant state") }
    }

    fun load(): DesktopTuningAssistantState {
        if (!file.exists()) return DesktopTuningAssistantState()
        val props = Properties().apply { file.inputStream().use(::load) }
        return DesktopTuningAssistantState(
            logPath = props.getProperty(KEY_LOG_PATH)?.takeIf { it.isNotBlank() },
            strategy = props.getProperty(KEY_STRATEGY)?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val KEY_LOG_PATH = "log_path"
        const val KEY_STRATEGY = "strategy"
    }
}
