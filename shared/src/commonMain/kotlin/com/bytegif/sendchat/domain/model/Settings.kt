package com.bytegif.sendchat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class AppSettings(
    val deviceId: String,
    val deviceName: String,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val downloadDirectory: String = "",
    val port: Int = 42831,
    val autoSyncClipboardGlobal: Boolean = false,
    val autoStartOnBoot: Boolean = false
)
