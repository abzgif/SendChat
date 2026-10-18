package com.bytegif.sendchat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.bytegif.sendchat.data.db.SendChatDatabase
import com.bytegif.sendchat.data.db.Settings_entity
import com.bytegif.sendchat.domain.model.AppSettings
import com.bytegif.sendchat.domain.model.ThemeMode
import com.bytegif.sendchat.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val database: SendChatDatabase,
    private val defaultDeviceId: String,
    private val defaultDeviceName: String,
    private val defaultDownloadDir: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return database.sendChatDatabaseQueries
            .selectSettings()
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { entity ->
                if (entity == null) {
                    initDefaultSettings()
                } else {
                    entity.toDomain()
                }
            }
    }

    override suspend fun getCurrentSettings(): AppSettings = withContext(dispatcher) {
        val entity = database.sendChatDatabaseQueries.selectSettings().executeAsOneOrNull()
        if (entity == null) {
            initDefaultSettings()
        } else {
            entity.toDomain()
        }
    }

    override suspend fun updateDeviceName(name: String) = withContext(dispatcher) {
        ensureSettingsInitialized()
        database.sendChatDatabaseQueries.updateDeviceName(name)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) = withContext(dispatcher) {
        ensureSettingsInitialized()
        database.sendChatDatabaseQueries.updateThemeMode(themeMode.name)
    }

    override suspend fun updateDownloadDirectory(path: String) = withContext(dispatcher) {
        ensureSettingsInitialized()
        database.sendChatDatabaseQueries.updateDownloadDirectory(path)
    }

    override suspend fun updateAutoSyncClipboard(enabled: Boolean) = withContext(dispatcher) {
        ensureSettingsInitialized()
        database.sendChatDatabaseQueries.updateAutoSyncClipboard(if (enabled) 1L else 0L)
    }

    override suspend fun updateAutoStartOnBoot(enabled: Boolean) = withContext(dispatcher) {
        ensureSettingsInitialized()
        database.sendChatDatabaseQueries.updateAutoStartOnBoot(if (enabled) 1L else 0L)
    }

    private fun initDefaultSettings(): AppSettings {
        val initial = AppSettings(
            deviceId = defaultDeviceId,
            deviceName = defaultDeviceName,
            themeMode = ThemeMode.SYSTEM,
            downloadDirectory = defaultDownloadDir,
            port = 42831,
            autoSyncClipboardGlobal = false,
            autoStartOnBoot = false
        )
        database.sendChatDatabaseQueries.insertOrReplaceSettings(
            deviceId = initial.deviceId,
            deviceName = initial.deviceName,
            themeMode = initial.themeMode.name,
            downloadDirectory = initial.downloadDirectory,
            port = initial.port.toLong(),
            autoSyncClipboardGlobal = 0L,
            autoStartOnBoot = 0L
        )
        return initial
    }

    private fun ensureSettingsInitialized() {
        val entity = database.sendChatDatabaseQueries.selectSettings().executeAsOneOrNull()
        if (entity == null) {
            initDefaultSettings()
        }
    }

    private fun Settings_entity.toDomain(): AppSettings {
        val theme = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.SYSTEM)
        return AppSettings(
            deviceId = deviceId,
            deviceName = deviceName,
            themeMode = theme,
            downloadDirectory = downloadDirectory,
            port = port.toInt(),
            autoSyncClipboardGlobal = autoSyncClipboardGlobal == 1L,
            autoStartOnBoot = autoStartOnBoot == 1L
        )
    }
}
