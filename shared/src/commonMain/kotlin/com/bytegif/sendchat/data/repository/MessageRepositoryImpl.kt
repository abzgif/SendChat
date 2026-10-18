package com.bytegif.sendchat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.bytegif.sendchat.data.db.Message_entity
import com.bytegif.sendchat.data.db.SendChatDatabase
import com.bytegif.sendchat.domain.model.Direction
import com.bytegif.sendchat.domain.model.Message
import com.bytegif.sendchat.domain.model.MessageContent
import com.bytegif.sendchat.domain.model.TransferStatus
import com.bytegif.sendchat.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MessageRepositoryImpl(
    private val database: SendChatDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : MessageRepository {

    override fun getMessagesForDevice(deviceId: String): Flow<List<Message>> {
        return database.sendChatDatabaseQueries
            .selectMessagesForDevice(deviceId)
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getLastMessageForDevice(deviceId: String): Flow<Message?> {
        return database.sendChatDatabaseQueries
            .selectLastMessageForDevice(deviceId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun getMessageById(messageId: String): Message? = withContext(dispatcher) {
        database.sendChatDatabaseQueries.selectMessageById(messageId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insertMessage(message: Message) = withContext(dispatcher) {
        val (contentType, bodyText, fileName, fileSize, filePath, mimeType, fileHash, thumb, w, h) = extractContentFields(message.content)
        val (statusCode, progress, bytesTransferred, totalBytes, failureReason) = extractStatusFields(message.status)

        database.sendChatDatabaseQueries.insertOrReplaceMessage(
            id = message.id,
            deviceId = message.deviceId,
            contentType = contentType,
            bodyText = bodyText,
            fileName = fileName,
            fileSize = fileSize,
            filePath = filePath,
            mimeType = mimeType,
            fileHash = fileHash,
            thumbnailBase64 = thumb,
            imageWidth = w?.toLong(),
            imageHeight = h?.toLong(),
            direction = message.direction.name,
            statusCode = statusCode,
            progress = progress.toDouble(),
            bytesTransferred = bytesTransferred,
            totalBytes = totalBytes,
            failureReason = failureReason,
            timestamp = message.timestamp
        )
    }

    override suspend fun updateMessageStatus(messageId: String, status: TransferStatus) = withContext(dispatcher) {
        val (statusCode, progress, bytesTransferred, totalBytes, failureReason) = extractStatusFields(status)
        database.sendChatDatabaseQueries.updateMessageStatus(
            statusCode = statusCode,
            progress = progress.toDouble(),
            bytesTransferred = bytesTransferred,
            totalBytes = totalBytes,
            failureReason = failureReason,
            id = messageId
        )
    }

    override suspend fun updateMessageFilePath(messageId: String, newPath: String) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.updateMessageFilePath(newPath, messageId)
    }

    override suspend fun deleteMessage(messageId: String) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.deleteMessage(messageId)
    }

    override suspend fun clearMessagesForDevice(deviceId: String) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.clearMessagesForDevice(deviceId)
    }

    private data class ContentFields(
        val contentType: String,
        val bodyText: String?,
        val fileName: String?,
        val fileSize: Long?,
        val filePath: String?,
        val mimeType: String?,
        val fileHash: String?,
        val thumbnailBase64: String?,
        val imageWidth: Int?,
        val imageHeight: Int?
    )

    private fun extractContentFields(content: MessageContent): ContentFields {
        return when (content) {
            is MessageContent.Text -> ContentFields(
                contentType = "TEXT",
                bodyText = content.text,
                fileName = null,
                fileSize = null,
                filePath = null,
                mimeType = null,
                fileHash = null,
                thumbnailBase64 = null,
                imageWidth = null,
                imageHeight = null
            )
            is MessageContent.Clipboard -> ContentFields(
                contentType = "CLIPBOARD",
                bodyText = content.text,
                fileName = null,
                fileSize = null,
                filePath = null,
                mimeType = null,
                fileHash = null,
                thumbnailBase64 = null,
                imageWidth = null,
                imageHeight = null
            )
            is MessageContent.FileAttachment -> ContentFields(
                contentType = "FILE",
                bodyText = null,
                fileName = content.fileName,
                fileSize = content.fileSize,
                filePath = content.filePath,
                mimeType = content.mimeType,
                fileHash = content.fileHash,
                thumbnailBase64 = null,
                imageWidth = null,
                imageHeight = null
            )
            is MessageContent.ImageAttachment -> ContentFields(
                contentType = "IMAGE",
                bodyText = null,
                fileName = content.fileName,
                fileSize = content.fileSize,
                filePath = content.filePath,
                mimeType = "image/*",
                fileHash = null,
                thumbnailBase64 = content.thumbnailBase64,
                imageWidth = content.width,
                imageHeight = content.height
            )
        }
    }

    private data class StatusFields(
        val statusCode: String,
        val progress: Float,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val failureReason: String?
    )

    private fun extractStatusFields(status: TransferStatus): StatusFields {
        return when (status) {
            is TransferStatus.Pending -> StatusFields("PENDING", 0f, 0L, 0L, null)
            is TransferStatus.InProgress -> StatusFields("IN_PROGRESS", status.progress, status.bytesTransferred, status.totalBytes, null)
            is TransferStatus.Completed -> StatusFields("COMPLETED", 1f, 0L, 0L, null)
            is TransferStatus.Failed -> StatusFields("FAILED", 0f, 0L, 0L, status.reason)
        }
    }

    private fun Message_entity.toDomain(): Message {
        val content: MessageContent = when (contentType) {
            "TEXT" -> MessageContent.Text(bodyText ?: "")
            "CLIPBOARD" -> MessageContent.Clipboard(bodyText ?: "")
            "FILE" -> MessageContent.FileAttachment(
                fileName = fileName ?: "file",
                fileSize = fileSize ?: 0L,
                filePath = filePath ?: "",
                mimeType = mimeType ?: "application/octet-stream",
                fileHash = fileHash ?: ""
            )
            "IMAGE" -> MessageContent.ImageAttachment(
                fileName = fileName ?: "image",
                fileSize = fileSize ?: 0L,
                filePath = filePath ?: "",
                thumbnailBase64 = thumbnailBase64,
                width = imageWidth?.toInt() ?: 0,
                height = imageHeight?.toInt() ?: 0
            )
            else -> MessageContent.Text(bodyText ?: "")
        }

        val status: TransferStatus = when (statusCode) {
            "PENDING" -> TransferStatus.Pending
            "IN_PROGRESS" -> TransferStatus.InProgress(
                progress = progress.toFloat(),
                bytesTransferred = bytesTransferred,
                totalBytes = totalBytes
            )
            "COMPLETED" -> TransferStatus.Completed
            "FAILED" -> TransferStatus.Failed(failureReason ?: "Unknown error")
            else -> TransferStatus.Completed
        }

        val dir = if (direction == "SENT") Direction.SENT else Direction.RECEIVED

        return Message(
            id = id,
            deviceId = deviceId,
            content = content,
            direction = dir,
            status = status,
            timestamp = timestamp
        )
    }
}
