package com.bytegif.sendchat.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class MessageContent {
    @Serializable
    data class Text(val text: String) : MessageContent()

    @Serializable
    data class Clipboard(val text: String) : MessageContent()

    @Serializable
    data class FileAttachment(
        val fileName: String,
        val fileSize: Long,
        val filePath: String,
        val mimeType: String = "application/octet-stream",
        val fileHash: String = ""
    ) : MessageContent()

    @Serializable
    data class ImageAttachment(
        val fileName: String,
        val fileSize: Long,
        val filePath: String,
        val thumbnailBase64: String? = null,
        val width: Int = 0,
        val height: Int = 0
    ) : MessageContent()
}

@Serializable
enum class Direction {
    SENT,
    RECEIVED
}

@Serializable
sealed class TransferStatus {
    @Serializable
    data object Pending : TransferStatus()

    @Serializable
    data class InProgress(
        val progress: Float,
        val bytesTransferred: Long,
        val totalBytes: Long
    ) : TransferStatus()

    @Serializable
    data object Completed : TransferStatus()

    @Serializable
    data class Failed(val reason: String) : TransferStatus()
}

@Serializable
data class Message(
    val id: String,
    val deviceId: String,
    val content: MessageContent,
    val direction: Direction,
    val status: TransferStatus = TransferStatus.Pending,
    val timestamp: Long
)
