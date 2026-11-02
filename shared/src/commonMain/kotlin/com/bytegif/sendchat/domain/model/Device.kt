package com.bytegif.sendchat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DevicePlatform {
    ANDROID,
    WINDOWS,
    MACOS,
    LINUX,
    WEBPAGE,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            ANDROID -> "Android"
            WINDOWS -> "Windows"
            MACOS -> "macOS"
            LINUX -> "Linux"
            WEBPAGE -> "Webpage"
            UNKNOWN -> "Unknown"
        }

    companion object {
        fun fromString(value: String): DevicePlatform {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: if (value.equals("web", true) || value.equals("webpage", true) || value.equals("web_browser", true)) WEBPAGE
                else UNKNOWN
        }
    }
}

@Serializable
data class Device(
    val id: String,
    val name: String,
    val platform: DevicePlatform,
    val ip: String,
    val port: Int = 42831,
    val publicKeyFingerprint: String = "",
    val isPaired: Boolean = false,
    val isOnline: Boolean = true,
    val lastSeen: Long = 0L,
    val autoSyncClipboard: Boolean = false,
    val isMuted: Boolean = false
)

@Serializable
data class PairedDevice(
    val id: String,
    val name: String,
    val platform: DevicePlatform,
    val publicKeyFingerprint: String,
    val lastKnownIp: String,
    val lastKnownPort: Int = 42831,
    val pairedAt: Long = 0L,
    val autoSyncClipboard: Boolean = false,
    val isMuted: Boolean = false
) {
    fun toDevice(isOnline: Boolean = (id == "web_browser")): Device = Device(
        id = id,
        name = name,
        platform = if (id == "web_browser") DevicePlatform.WEBPAGE else platform,
        ip = lastKnownIp,
        port = lastKnownPort,
        publicKeyFingerprint = publicKeyFingerprint,
        isPaired = true,
        isOnline = if (id == "web_browser") true else isOnline,
        lastSeen = pairedAt,
        autoSyncClipboard = autoSyncClipboard,
        isMuted = isMuted
    )
}

@Serializable
data class PairingInfo(
    val deviceId: String,
    val deviceName: String,
    val platform: DevicePlatform,
    val ip: String,
    val port: Int,
    val pin: String,
    val publicKeyFingerprint: String,
    val certificateBase64: String = ""
)
