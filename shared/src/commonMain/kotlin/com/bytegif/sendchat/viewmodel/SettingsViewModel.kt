package com.bytegif.sendchat.viewmodel

import com.bytegif.sendchat.domain.model.AppSettings
import com.bytegif.sendchat.domain.model.ThemeMode
import com.bytegif.sendchat.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val settings: StateFlow<AppSettings> = settingsRepository.getSettings().stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings(
            deviceId = "loading",
            deviceName = "SendChat Device"
        )
    )

    fun updateDeviceName(name: String) {
        if (name.isBlank()) return
        scope.launch {
            settingsRepository.updateDeviceName(name.trim())
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        scope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun updateDownloadDirectory(path: String) {
        if (path.isBlank()) return
        scope.launch {
            settingsRepository.updateDownloadDirectory(path.trim())
        }
    }

    fun updateAutoSyncClipboard(enabled: Boolean) {
        scope.launch {
            settingsRepository.updateAutoSyncClipboard(enabled)
        }
    }

    fun updateAutoStartOnBoot(enabled: Boolean) {
        scope.launch {
            settingsRepository.updateAutoStartOnBoot(enabled)
        }
    }
}
