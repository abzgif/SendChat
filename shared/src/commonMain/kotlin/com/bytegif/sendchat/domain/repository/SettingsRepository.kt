package com.bytegif.sendchat.domain.repository

import com.bytegif.sendchat.domain.model.AppSettings
import com.bytegif.sendchat.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun getCurrentSettings(): AppSettings
    suspend fun updateDeviceName(name: String)
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateDownloadDirectory(path: String)
    suspend fun updateAutoSyncClipboard(enabled: Boolean)
    suspend fun updateAutoStartOnBoot(enabled: Boolean)
}
