package com.bytegif.sendchat.viewmodel

import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.Direction
import com.bytegif.sendchat.domain.model.Message
import com.bytegif.sendchat.domain.model.MessageContent
import com.bytegif.sendchat.domain.repository.DeviceRepository
import com.bytegif.sendchat.domain.repository.MessageRepository
import com.bytegif.sendchat.network.TransferManager
import com.bytegif.sendchat.platform.ClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val deviceRepository: DeviceRepository,
    private val transferManager: TransferManager,
    private val clipboardManager: ClipboardManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _activeDevice = MutableStateFlow<Device?>(null)
    val activeDevice: StateFlow<Device?> = _activeDevice.asStateFlow()

    val inputText = MutableStateFlow("")

    val messages: StateFlow<List<Message>> = _activeDevice.flatMapLatest { device ->
        if (device != null) {
            messageRepository.getMessagesForDevice(device.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveDevice(device: Device?) {
        _activeDevice.value = device
    }

    fun openChatWithDeviceId(deviceId: String) {
        scope.launch {
            val paired = deviceRepository.getPairedDeviceById(deviceId)
            if (paired != null) {
                _activeDevice.value = paired.toDevice()
            }
        }
    }

    fun sendTextMessage() {
        val device = _activeDevice.value ?: return
        val text = inputText.value.trim()
        if (text.isBlank()) return

        inputText.value = ""
        scope.launch {
            transferManager.sendTextMessage(device, text)
        }
    }

    fun sendCurrentClipboard() {
        val device = _activeDevice.value ?: return
        val text = clipboardManager.getText() ?: return
        if (text.isBlank()) return

        scope.launch {
            transferManager.sendClipboard(device, text)
        }
    }

    fun copyMessageToClipboard(text: String) {
        clipboardManager.setText(text)
    }

    fun sendFiles(filePaths: List<String>) {
        val device = _activeDevice.value ?: return
        scope.launch {
            for (path in filePaths) {
                try {
                    transferManager.sendFile(device, path)
                } finally {
                    if (path.contains("shared_file_")) {
                        runCatching { java.io.File(path).delete() }
                    }
                }
            }
        }
    }

    fun retryMessage(message: Message) {
        val device = _activeDevice.value ?: return
        scope.launch {
            when (val content = message.content) {
                is MessageContent.Text -> transferManager.sendTextMessage(device, content.text)
                is MessageContent.Clipboard -> transferManager.sendClipboard(device, content.text)
                is MessageContent.FileAttachment -> transferManager.sendFile(device, content.filePath)
                is MessageContent.ImageAttachment -> transferManager.sendFile(device, content.filePath)
            }
        }
    }

    fun cancelTransfer(transferId: String) {
        transferManager.cancelTransfer(transferId)
    }

    fun deleteMessageAndFile(message: Message, deleteFromStorage: Boolean = false) {
        scope.launch {
            if (deleteFromStorage && message.direction == Direction.RECEIVED) {
                when (val content = message.content) {
                    is MessageContent.FileAttachment -> {
                        if (content.filePath.isNotBlank()) {
                            runCatching { java.io.File(content.filePath).delete() }
                        }
                    }
                    is MessageContent.ImageAttachment -> {
                        if (content.filePath.isNotBlank()) {
                            runCatching { java.io.File(content.filePath).delete() }
                        }
                    }
                    else -> {}
                }
            }
            messageRepository.deleteMessage(message.id)
        }
    }

    fun deleteMessage(messageId: String) {
        scope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun deleteMessages(messageIds: Set<String>, deleteFiles: Boolean = false) {
        if (messageIds.isEmpty()) return
        scope.launch {
            if (deleteFiles) {
                val currentMessages = messages.value.filter { it.id in messageIds }
                for (msg in currentMessages) {
                    if (msg.direction == Direction.RECEIVED) {
                        when (val content = msg.content) {
                            is MessageContent.FileAttachment -> {
                                if (content.filePath.isNotBlank()) {
                                    runCatching { java.io.File(content.filePath).delete() }
                                }
                            }
                            is MessageContent.ImageAttachment -> {
                                if (content.filePath.isNotBlank()) {
                                    runCatching { java.io.File(content.filePath).delete() }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
            for (id in messageIds) {
                messageRepository.deleteMessage(id)
            }
        }
    }

    fun clearChatHistory(deleteFiles: Boolean = false) {
        val device = _activeDevice.value ?: return
        scope.launch {
            if (deleteFiles) {
                val currentMessages = messages.value
                for (msg in currentMessages) {
                    if (msg.direction == Direction.RECEIVED) {
                        when (val content = msg.content) {
                            is MessageContent.FileAttachment -> {
                                if (content.filePath.isNotBlank()) {
                                    runCatching { java.io.File(content.filePath).delete() }
                                }
                            }
                            is MessageContent.ImageAttachment -> {
                                if (content.filePath.isNotBlank()) {
                                    runCatching { java.io.File(content.filePath).delete() }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
            messageRepository.clearMessagesForDevice(device.id)
        }
    }

    fun toggleClipboardAutoSync(enabled: Boolean) {
        val device = _activeDevice.value ?: return
        scope.launch {
            deviceRepository.updateDeviceClipboardSync(device.id, enabled)
            _activeDevice.value = device.copy(autoSyncClipboard = enabled)
        }
    }

    fun toggleDeviceMute(isMuted: Boolean) {
        val device = _activeDevice.value ?: return
        scope.launch {
            deviceRepository.updateDeviceMute(device.id, isMuted)
            _activeDevice.value = device.copy(isMuted = isMuted)
        }
    }
}
