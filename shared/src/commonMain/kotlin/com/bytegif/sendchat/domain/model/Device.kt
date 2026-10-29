package com.bytegif.sendchat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DevicePlatform {
    ANDROID,
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            ANDROID -> "Android"
            WINDOWS -> "Windows"
            MACOS -> "macOS"
            LINUX -> "Linux"
            UNKNOWN -> "Unknown"
        }

    companion object {
        fun fromString(value: String): DevicePlatform {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
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
    fun toDevice(isOnline: Boolean = false): Device = Device(
        id = id,
        name = name,
        platform = platform,
        ip = lastKnownIp,
        port = lastKnownPort,
        publicKeyFingerprint = publicKeyFingerprint,
        isPaired = true,
        isOnline = isOnline,
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
