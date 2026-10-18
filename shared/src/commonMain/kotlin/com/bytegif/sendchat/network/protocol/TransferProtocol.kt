package com.bytegif.sendchat.network.protocol

import kotlinx.serialization.Serializable

@Serializable
data class PairRequest(
    val deviceId: String,
    val deviceName: String,
    val platform: String,
    val port: Int,
    val publicKeyFingerprint: String = "",
    val certificatePem: String = ""
)

@Serializable
data class PairResponse(
    val accepted: Boolean,
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "",
    val publicKeyFingerprint: String = "",
    val certificatePem: String = "",
    val error: String? = null
)

@Serializable
data class UdpBeaconPayload(
    val protocol: String = "sendchat_v1",
    val deviceId: String,
    val deviceName: String,
    val platform: String,
    val port: Int,
    val publicKeyFingerprint: String = "",
    val isReply: Boolean = false
)

@Serializable
data class MessagePayload(
    val messageId: String,
    val senderDeviceId: String,
    val type: String, // TEXT, CLIPBOARD
    val text: String,
    val timestamp: Long
)

@Serializable
data class FileInitPayload(
    val transferId: String,
    val senderDeviceId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String = "application/octet-stream",
    val fileHash: String = "",
    val isImage: Boolean = false,
    val thumbnailBase64: String? = null,
    val timestamp: Long
)

@Serializable
data class FileChunkPayload(
    val transferId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val chunkDataBase64: String,
    val isLast: Boolean
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null
)
