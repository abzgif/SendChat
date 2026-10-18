package com.bytegif.sendchat.domain.repository

import com.bytegif.sendchat.domain.model.Message
import com.bytegif.sendchat.domain.model.TransferStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessagesForDevice(deviceId: String): Flow<List<Message>>
    fun getLastMessageForDevice(deviceId: String): Flow<Message?>
    suspend fun getMessageById(messageId: String): Message?
    suspend fun insertMessage(message: Message)
    suspend fun updateMessageStatus(messageId: String, status: TransferStatus)
    suspend fun updateMessageFilePath(messageId: String, newPath: String)
    suspend fun deleteMessage(messageId: String)
    suspend fun clearMessagesForDevice(deviceId: String)
}
