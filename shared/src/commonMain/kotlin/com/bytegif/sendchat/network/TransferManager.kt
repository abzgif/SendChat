package com.bytegif.sendchat.network

import com.bytegif.sendchat.data.file.FileManager
import com.bytegif.sendchat.discovery.DiscoveryManager
import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.domain.model.Direction
import com.bytegif.sendchat.domain.model.Message
import com.bytegif.sendchat.domain.model.MessageContent
import com.bytegif.sendchat.domain.model.PairedDevice
import com.bytegif.sendchat.domain.model.TransferStatus
import com.bytegif.sendchat.domain.repository.DeviceRepository
import com.bytegif.sendchat.domain.repository.MessageRepository
import com.bytegif.sendchat.domain.repository.SettingsRepository
import com.bytegif.sendchat.network.client.TransferClient
import com.bytegif.sendchat.network.protocol.FileInitPayload
import com.bytegif.sendchat.network.protocol.MessagePayload
import com.bytegif.sendchat.network.protocol.PairRequest
import com.bytegif.sendchat.network.protocol.PairResponse
import com.bytegif.sendchat.network.server.IncomingPairPrompt
import com.bytegif.sendchat.network.server.TransferServer
import com.bytegif.sendchat.platform.ClipboardManager
import com.bytegif.sendchat.platform.PlatformInfo
import com.bytegif.sendchat.security.CryptoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class TransferManager(
    val deviceRepository: DeviceRepository,
    val messageRepository: MessageRepository,
    val settingsRepository: SettingsRepository,
    val discoveryManager: DiscoveryManager,
    val clipboardManager: ClipboardManager,
    val transferServer: TransferServer,
    val transferClient: TransferClient
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    val incomingPairRequests: SharedFlow<IncomingPairPrompt> = transferServer.incomingPairRequests
    private val activeOutgoingJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    fun cancelTransfer(transferId: String) {
        val job = activeOutgoingJobs.remove(transferId)
        if (job != null) {
            job.cancel()
            scope.launch {
                messageRepository.updateMessageStatus(transferId, TransferStatus.Failed("Cancelled by user"))
            }
        }
        transferServer.cancelReceivingTransfer(transferId)
    }

    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) return
        isInitialized = true

        val settings = settingsRepository.getCurrentSettings()
        val identity = CryptoEngine.getOrCreateDeviceIdentity(settings.deviceId, settings.deviceName)

        // Start server and get actual listening port
        val boundPort = transferServer.start(settings.port)

        // Start discovery
        val localDevice = Device(
            id = settings.deviceId,
            name = settings.deviceName,
            platform = PlatformInfo.platform,
            ip = PlatformInfo.getLocalIpAddresses().firstOrNull() ?: "127.0.0.1",
            port = boundPort,
            publicKeyFingerprint = identity.publicKeyFingerprint
        )
        discoveryManager.start(localDevice)

        // Observe discovered devices and update repository
        scope.launch {
            discoveryManager.discoveredDevices.collect { list ->
                deviceRepository.updateDiscoveredDevices(list)
            }
        }

        // Setup clipboard observer
        clipboardManager.startObserving { newClipText ->
            scope.launch {
                handleClipboardChanged(newClipText)
            }
        }
    }

    private suspend fun handleClipboardChanged(text: String) {
        val settings = settingsRepository.getCurrentSettings()
        val pairedList = deviceRepository.getPairedDevices().first()
        val onlineList = deviceRepository.getDiscoveredDevices().first()

        val targets = if (settings.autoSyncClipboardGlobal) {
            // When global sync is on, sync with all online discovered devices + paired devices
            val combined = mutableListOf<Device>()
            combined.addAll(onlineList)
            for (paired in pairedList) {
                if (combined.none { it.id == paired.id }) {
                    combined.add(
                        Device(
                            id = paired.id,
                            name = paired.name,
                            platform = paired.platform,
                            ip = paired.lastKnownIp,
                            port = paired.lastKnownPort
                        )
                    )
                }
            }
            combined
        } else {
            val activePairs = pairedList.filter { it.autoSyncClipboard }
            activePairs.map { paired ->
                onlineList.firstOrNull { it.id == paired.id } ?: Device(
                    id = paired.id,
                    name = paired.name,
                    platform = paired.platform,
                    ip = paired.lastKnownIp,
                    port = paired.lastKnownPort
                )
            }
        }

        for (target in targets) {
            if (target.ip.isNotBlank() && target.ip != "127.0.0.1") {
                runCatching {
                    sendClipboard(target, text)
                }
            }
        }
    }

    suspend fun pairWithDevice(targetDevice: Device): PairResponse {
        val settings = settingsRepository.getCurrentSettings()
        val identity = CryptoEngine.getOrCreateDeviceIdentity(settings.deviceId, settings.deviceName)

        val request = PairRequest(
            deviceId = settings.deviceId,
            deviceName = settings.deviceName,
            platform = PlatformInfo.platform.name,
            port = settings.port,
            publicKeyFingerprint = identity.publicKeyFingerprint,
            certificatePem = identity.certificatePem
        )

        val response = transferClient.sendPairRequest(targetDevice.ip, targetDevice.port, request)
        if (response.accepted) {
            val pairedDevice = PairedDevice(
                id = if (response.deviceId.isNotBlank()) response.deviceId else targetDevice.id,
                name = if (response.deviceName.isNotBlank()) response.deviceName else targetDevice.name,
                platform = if (response.platform.isNotBlank()) DevicePlatform.fromString(response.platform) else targetDevice.platform,
                publicKeyFingerprint = if (response.publicKeyFingerprint.isNotBlank()) response.publicKeyFingerprint else targetDevice.publicKeyFingerprint,
                lastKnownIp = targetDevice.ip,
                lastKnownPort = targetDevice.port,
                pairedAt = System.currentTimeMillis(),
                autoSyncClipboard = false
            )
            deviceRepository.savePairedDevice(pairedDevice)
        }
        return response
    }

    suspend fun pairWithIp(ip: String, port: Int = 42831): PairResponse {
        val targetDevice = Device(
            id = "",
            name = "Remote Device ($ip)",
            platform = DevicePlatform.UNKNOWN,
            ip = ip.trim(),
            port = port
        )
        return pairWithDevice(targetDevice)
    }

    suspend fun sendTextMessage(targetDevice: Device, text: String): Boolean {
        val settings = settingsRepository.getCurrentSettings()
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            deviceId = targetDevice.id,
            content = MessageContent.Text(text),
            direction = Direction.SENT,
            status = TransferStatus.InProgress(0.5f, 0L, 0L),
            timestamp = timestamp
        )
        messageRepository.insertMessage(message)

        val payload = MessagePayload(
            messageId = messageId,
            senderDeviceId = settings.deviceId,
            type = "TEXT",
            text = text,
            timestamp = timestamp
        )

        val success = transferClient.sendMessage(targetDevice.ip, targetDevice.port, payload)
        if (success) {
            messageRepository.updateMessageStatus(messageId, TransferStatus.Completed)
        } else {
            messageRepository.updateMessageStatus(messageId, TransferStatus.Failed("Delivery failed"))
        }
        return success
    }

    suspend fun sendClipboard(targetDevice: Device, text: String): Boolean {
        val settings = settingsRepository.getCurrentSettings()
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            deviceId = targetDevice.id,
            content = MessageContent.Clipboard(text),
            direction = Direction.SENT,
            status = TransferStatus.InProgress(0.5f, 0L, 0L),
            timestamp = timestamp
        )
        messageRepository.insertMessage(message)

        val payload = MessagePayload(
            messageId = messageId,
            senderDeviceId = settings.deviceId,
            type = "CLIPBOARD",
            text = text,
            timestamp = timestamp
        )

        val success = transferClient.sendMessage(targetDevice.ip, targetDevice.port, payload)
        if (success) {
            messageRepository.updateMessageStatus(messageId, TransferStatus.Completed)
        } else {
            messageRepository.updateMessageStatus(messageId, TransferStatus.Failed("Delivery failed"))
        }
        return success
    }

    suspend fun sendFile(targetDevice: Device, filePath: String): Boolean {
        if (!FileManager.fileExists(filePath)) return false

        val fileName = FileManager.getFileName(filePath)
        val fileSize = FileManager.getFileSize(filePath)

        val settings = settingsRepository.getCurrentSettings()
        val transferId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val isImage = isImageFile(fileName)
        val thumbBase64 = if (isImage) FileManager.generateImageThumbnailBase64(filePath, 256) else null

        val fileHash = if (fileSize in 1..(10 * 1024 * 1024)) {
            FileManager.calculateFileSha256(filePath)
        } else {
            "${fileName}_${fileSize}"
        }

        val content: MessageContent = if (isImage) {
            MessageContent.ImageAttachment(
                fileName = fileName,
                fileSize = fileSize,
                filePath = filePath,
                thumbnailBase64 = thumbBase64
            )
        } else {
            MessageContent.FileAttachment(
                fileName = fileName,
                fileSize = fileSize,
                filePath = filePath,
                mimeType = getMimeType(fileName),
                fileHash = fileHash
            )
        }

        val message = Message(
            id = transferId,
            deviceId = targetDevice.id,
            content = content,
            direction = Direction.SENT,
            status = TransferStatus.InProgress(0f, 0L, fileSize),
            timestamp = timestamp
        )
        messageRepository.insertMessage(message)

        val fileInit = FileInitPayload(
            transferId = transferId,
            senderDeviceId = settings.deviceId,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = getMimeType(fileName),
            fileHash = fileHash,
            isImage = isImage,
            thumbnailBase64 = thumbBase64,
            timestamp = timestamp
        )

        val currentJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        if (currentJob != null) {
            activeOutgoingJobs[transferId] = currentJob
        }

        val success = try {
            transferClient.sendFile(
                targetIp = targetDevice.ip,
                port = targetDevice.port,
                fileInit = fileInit,
                localFilePath = filePath
            ) { progress, sent, total ->
                messageRepository.updateMessageStatus(
                    transferId,
                    TransferStatus.InProgress(progress, sent, total)
                )
            }
        } finally {
            activeOutgoingJobs.remove(transferId)
        }

        if (success) {
            messageRepository.updateMessageStatus(transferId, TransferStatus.Completed)
        } else {
            messageRepository.updateMessageStatus(transferId, TransferStatus.Failed("Transfer interrupted"))
        }
        return success
    }

    private fun isImageFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "tar", "gz" -> "application/gzip"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "txt" -> "text/plain"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    suspend fun shutdown() {
        isInitialized = false
        clipboardManager.stopObserving()
        discoveryManager.stop()
        transferServer.stop()
        transferClient.close()
    }
}
