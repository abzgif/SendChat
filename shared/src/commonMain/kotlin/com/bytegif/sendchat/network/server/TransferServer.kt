package com.bytegif.sendchat.network.server

import com.bytegif.sendchat.data.file.FileManager
import com.bytegif.sendchat.data.file.isImageFile
import com.bytegif.sendchat.data.file.getMimeType
import com.bytegif.sendchat.domain.model.AppSettings
import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.domain.model.Direction
import com.bytegif.sendchat.domain.model.Message
import com.bytegif.sendchat.domain.model.MessageContent
import com.bytegif.sendchat.domain.model.PairedDevice
import com.bytegif.sendchat.domain.model.TransferStatus
import com.bytegif.sendchat.domain.repository.DeviceRepository
import com.bytegif.sendchat.domain.repository.MessageRepository
import com.bytegif.sendchat.domain.repository.SettingsRepository
import com.bytegif.sendchat.network.protocol.ApiResponse
import com.bytegif.sendchat.network.protocol.FileChunkPayload
import com.bytegif.sendchat.network.protocol.FileInitPayload
import com.bytegif.sendchat.network.protocol.MessagePayload
import com.bytegif.sendchat.network.protocol.PairRequest
import com.bytegif.sendchat.network.protocol.PairResponse
import com.bytegif.sendchat.platform.ClipboardManager
import com.bytegif.sendchat.platform.Notifier
import com.bytegif.sendchat.platform.PlatformInfo
import com.bytegif.sendchat.security.CryptoEngine
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class IncomingPairPrompt(
    val request: PairRequest,
    val deferredResponse: CompletableDeferred<PairResponse>
)

class TransferServer(
    private val deviceRepository: DeviceRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    private val clipboardManager: ClipboardManager
) {
    val webShareManager = WebShareManager()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var isRunning: Boolean = false

    var boundPort: Int = 42831
        private set

    private val _incomingPairRequests = MutableSharedFlow<IncomingPairPrompt>(extraBufferCapacity = 10)
    val incomingPairRequests: SharedFlow<IncomingPairPrompt> = _incomingPairRequests.asSharedFlow()

    private val activeReceivingFiles = ConcurrentHashMap<String, ReceivingFileState>()

    private data class ReceivingFileState(
        val payload: FileInitPayload,
        val partFile: File,
        val finalFilePath: String,
        val outputStream: java.io.BufferedOutputStream,
        var bytesWritten: Long = 0L
    )

    fun cancelReceivingTransfer(transferId: String) {
        val state = activeReceivingFiles.remove(transferId)
        if (state != null) {
            runCatching {
                state.outputStream.close()
                state.partFile.delete()
            }
            scope.launch {
                messageRepository.updateMessageStatus(transferId, TransferStatus.Failed("Cancelled by user"))
            }
        }
    }

    suspend fun start(port: Int = 42831): Int {
        if (server != null && isRunning) {
            return boundPort
        }
        stop()

        var currentPort = port
        var attempts = 0
        while (!isRunning && attempts < 5) {
            try {
                val s = embeddedServer(CIO, port = currentPort, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = true
                        })
                    }
                    install(CORS) {
                        anyHost()
                        allowHeader("*")
                        allowMethod(io.ktor.http.HttpMethod.Options)
                        allowMethod(io.ktor.http.HttpMethod.Post)
                        allowMethod(io.ktor.http.HttpMethod.Get)
                    }

                    routing {
                        get("/api/ping") {
                            val settings = settingsRepository.getCurrentSettings()
                            val identity = CryptoEngine.getOrCreateDeviceIdentity(settings.deviceId, settings.deviceName)
                            call.respond(
                                PairResponse(
                                    accepted = true,
                                    deviceId = settings.deviceId,
                                    deviceName = settings.deviceName,
                                    platform = com.bytegif.sendchat.platform.PlatformInfo.platform.name,
                                    publicKeyFingerprint = identity.publicKeyFingerprint,
                                    certificatePem = identity.certificatePem
                                )
                            )
                        }

                        post("/api/pair") {
                            val req = call.receive<PairRequest>()
                            val settings = settingsRepository.getCurrentSettings()
                            val identity = CryptoEngine.getOrCreateDeviceIdentity(settings.deviceId, settings.deviceName)

                            val existingPaired = deviceRepository.getPairedDeviceById(req.deviceId)
                            if (existingPaired != null) {
                                val pairedDevice = PairedDevice(
                                    id = req.deviceId,
                                    name = req.deviceName,
                                    platform = DevicePlatform.fromString(req.platform),
                                    publicKeyFingerprint = req.publicKeyFingerprint,
                                    lastKnownIp = call.request.local.remoteAddress,
                                    lastKnownPort = req.port,
                                    pairedAt = existingPaired.pairedAt,
                                    autoSyncClipboard = existingPaired.autoSyncClipboard,
                                    isMuted = existingPaired.isMuted
                                )
                                deviceRepository.savePairedDevice(pairedDevice)

                                call.respond(
                                    PairResponse(
                                        accepted = true,
                                        deviceId = settings.deviceId,
                                        deviceName = settings.deviceName,
                                        platform = com.bytegif.sendchat.platform.PlatformInfo.platform.name,
                                        publicKeyFingerprint = identity.publicKeyFingerprint,
                                        certificatePem = identity.certificatePem
                                    )
                                )
                            } else {
                                val deferred = CompletableDeferred<PairResponse>()
                                _incomingPairRequests.tryEmit(IncomingPairPrompt(req, deferred))

                                val response = deferred.await()
                                if (response.accepted) {
                                    val pairedDevice = PairedDevice(
                                        id = req.deviceId,
                                        name = req.deviceName,
                                        platform = DevicePlatform.fromString(req.platform),
                                        publicKeyFingerprint = req.publicKeyFingerprint,
                                        lastKnownIp = call.request.local.remoteAddress,
                                        lastKnownPort = req.port,
                                        pairedAt = System.currentTimeMillis(),
                                        autoSyncClipboard = false,
                                        isMuted = false
                                    )
                                    deviceRepository.savePairedDevice(pairedDevice)
                                }
                                call.respond(response)
                            }
                        }

                        post("/api/message") {
                            val msgPayload = call.receive<MessagePayload>()
                            val paired = deviceRepository.getPairedDeviceById(msgPayload.senderDeviceId)
                            val senderName = paired?.name ?: "Remote Device"
                            val isMuted = paired?.isMuted ?: false

                            val content: MessageContent = if (msgPayload.type == "CLIPBOARD") {
                                val clipText = msgPayload.text
                                val settings = settingsRepository.getCurrentSettings()
                                if (settings.autoSyncClipboardGlobal || (paired?.autoSyncClipboard == true)) {
                                    clipboardManager.setText(clipText)
                                }
                                Notifier.showNotification("Clipboard Received", "Copied text from $senderName", msgPayload.senderDeviceId, isMuted = isMuted)
                                MessageContent.Clipboard(clipText)
                            } else {
                                Notifier.showNotification(senderName, msgPayload.text, msgPayload.senderDeviceId, isMuted = isMuted)
                                MessageContent.Text(msgPayload.text)
                            }

                            val message = Message(
                                id = msgPayload.messageId,
                                deviceId = msgPayload.senderDeviceId,
                                content = content,
                                direction = Direction.RECEIVED,
                                status = TransferStatus.Completed,
                                timestamp = msgPayload.timestamp
                            )
                            messageRepository.insertMessage(message)
                            call.respond(ApiResponse(success = true))
                        }

                        post("/api/file/init") {
                            try {
                                val fileInit = call.receive<FileInitPayload>()
                                val settings = settingsRepository.getCurrentSettings()
                                val configuredDir = settings.downloadDirectory.ifBlank {
                                    com.bytegif.sendchat.platform.PlatformInfo.getDefaultDownloadDirectory()
                                }

                                var downloadDir = configuredDir
                                var finalFilePath = FileManager.getUniqueFilePath(downloadDir, fileInit.fileName)
                                var finalFile = File(finalFilePath)
                                var partFile = File("$finalFilePath.part")
                                var outputStream: java.io.BufferedOutputStream? = null

                                // Tier 1: Try configured download directory
                                try {
                                    finalFile.parentFile?.mkdirs()
                                    if (partFile.exists()) partFile.delete()
                                    outputStream = java.io.BufferedOutputStream(FileOutputStream(partFile), 512 * 1024)
                                } catch (_: Throwable) {}

                                // Tier 2: Try PlatformInfo default download directory
                                if (outputStream == null) {
                                    try {
                                        downloadDir = com.bytegif.sendchat.platform.PlatformInfo.getDefaultDownloadDirectory()
                                        finalFilePath = FileManager.getUniqueFilePath(downloadDir, fileInit.fileName)
                                        finalFile = File(finalFilePath)
                                        finalFile.parentFile?.mkdirs()
                                        partFile = File("$finalFilePath.part")
                                        if (partFile.exists()) partFile.delete()
                                        outputStream = java.io.BufferedOutputStream(FileOutputStream(partFile), 512 * 1024)
                                    } catch (_: Throwable) {}
                                }

                                // Tier 3: Guaranteed writable fallback
                                if (outputStream == null) {
                                    try {
                                        val tempDir = File.createTempFile("sc_test", ".tmp").parentFile ?: File("/tmp")
                                        downloadDir = tempDir.absolutePath
                                        finalFilePath = FileManager.getUniqueFilePath(downloadDir, fileInit.fileName)
                                        finalFile = File(finalFilePath)
                                        finalFile.parentFile?.mkdirs()
                                        partFile = File("$finalFilePath.part")
                                        if (partFile.exists()) partFile.delete()
                                        outputStream = java.io.BufferedOutputStream(FileOutputStream(partFile), 512 * 1024)
                                    } catch (e: Throwable) {
                                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = "Storage error: ${e.message}"))
                                        return@post
                                    }
                                }

                                activeReceivingFiles[fileInit.transferId] = ReceivingFileState(
                                    payload = fileInit,
                                    partFile = partFile,
                                    finalFilePath = finalFilePath,
                                    outputStream = outputStream
                                )

                                val initialContent: MessageContent = if (fileInit.isImage) {
                                    MessageContent.ImageAttachment(
                                        fileName = fileInit.fileName,
                                        fileSize = fileInit.fileSize,
                                        filePath = "",
                                        thumbnailBase64 = fileInit.thumbnailBase64
                                    )
                                } else {
                                    MessageContent.FileAttachment(
                                        fileName = fileInit.fileName,
                                        fileSize = fileInit.fileSize,
                                        filePath = "",
                                        mimeType = fileInit.mimeType,
                                        fileHash = fileInit.fileHash
                                    )
                                }

                                val message = Message(
                                    id = fileInit.transferId,
                                    deviceId = fileInit.senderDeviceId,
                                    content = initialContent,
                                    direction = Direction.RECEIVED,
                                    status = TransferStatus.InProgress(0f, 0L, fileInit.fileSize),
                                    timestamp = fileInit.timestamp
                                )
                                messageRepository.insertMessage(message)
                                call.respond(ApiResponse(success = true))
                            } catch (e: Throwable) {
                                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = e.message ?: "Init error"))
                            }
                        }

                        post("/api/file/chunk") {
                            try {
                                val chunk = call.receive<FileChunkPayload>()
                                val state = activeReceivingFiles[chunk.transferId]
                                if (state == null) {
                                    val existingMsg = messageRepository.getMessageById(chunk.transferId)
                                    if (existingMsg != null && existingMsg.status is TransferStatus.Completed) {
                                        val filePath = when (val c = existingMsg.content) {
                                            is MessageContent.FileAttachment -> c.filePath
                                            is MessageContent.ImageAttachment -> c.filePath
                                            else -> ""
                                        }
                                        call.respond(ApiResponse(success = true, message = filePath))
                                        return@post
                                    }
                                    call.respond(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Transfer not found"))
                                    return@post
                                }

                                if (chunk.chunkDataBase64.isNotBlank()) {
                                    val bytes = runCatching {
                                        java.util.Base64.getDecoder().decode(chunk.chunkDataBase64.trim())
                                    }.getOrDefault(ByteArray(0))
                                    if (bytes.isNotEmpty()) {
                                        state.outputStream.write(bytes)
                                        state.bytesWritten += bytes.size
                                    }
                                }

                                val progress = if (state.payload.fileSize > 0) {
                                    (state.bytesWritten.toFloat() / state.payload.fileSize).coerceIn(0f, 1f)
                                } else 1f

                                if (!chunk.isLast) {
                                    messageRepository.updateMessageStatus(
                                        chunk.transferId,
                                        TransferStatus.InProgress(progress, state.bytesWritten, state.payload.fileSize)
                                    )
                                    call.respond(ApiResponse(success = true))
                                } else {
                                    runCatching { state.outputStream.flush() }
                                    runCatching { state.outputStream.close() }

                                    val finalFile = File(state.finalFilePath)
                                    if (finalFile.exists()) finalFile.delete()
                                    val renamed = state.partFile.renameTo(finalFile)
                                    if (!renamed) {
                                        runCatching {
                                            state.partFile.copyTo(finalFile, overwrite = true)
                                            state.partFile.delete()
                                        }
                                    }

                                    activeReceivingFiles.remove(chunk.transferId)

                                    messageRepository.updateMessageFilePath(chunk.transferId, state.finalFilePath)
                                    messageRepository.updateMessageStatus(chunk.transferId, TransferStatus.Completed)

                                    PlatformInfo.scanMediaFile(state.finalFilePath)

                                    val paired = deviceRepository.getPairedDeviceById(state.payload.senderDeviceId)
                                    val senderName = paired?.name ?: "Remote Device"
                                    val isMuted = paired?.isMuted ?: false
                                    val fileType = if (state.payload.isImage) "image" else "file"
                                    Notifier.showNotification("Received $fileType", "${state.payload.fileName} from $senderName", state.payload.senderDeviceId, isMuted = isMuted)

                                    call.respond(ApiResponse(success = true, message = state.finalFilePath))
                                }
                            } catch (e: Throwable) {
                                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = e.message ?: "Chunk error"))
                            }
                        }

                        // Local Web Share endpoints
                        get("/") {
                            val settings = settingsRepository.getCurrentSettings()
                            call.respondText(webShareManager.getHtml(settings.deviceName), ContentType.Text.Html)
                        }

                        get("/web") {
                            val settings = settingsRepository.getCurrentSettings()
                            call.respondText(webShareManager.getHtml(settings.deviceName), ContentType.Text.Html)
                        }

                        get("/api/web/files") {
                            call.respond(webShareManager.sharedFiles.value)
                        }

                        get("/api/web/download/{id}") {
                            val id = call.parameters["id"] ?: ""
                            val shared = webShareManager.getFile(id)
                            if (shared == null) {
                                call.respond(HttpStatusCode.NotFound, "File not found")
                                return@get
                            }
                            val stream = FileManager.openInputStream(shared.filePath)
                            if (stream == null) {
                                call.respond(HttpStatusCode.NotFound, "File missing or unreadable")
                                return@get
                            }
                            val encodedFileName = runCatching {
                                java.net.URLEncoder.encode(shared.name, "UTF-8").replace("+", "%20")
                            }.getOrDefault(shared.name)
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                "attachment; filename=\"${shared.name.replace("\"", "\\\"")}\"; filename*=UTF-8''$encodedFileName"
                            )
                            if (shared.size > 0) {
                                call.response.header(HttpHeaders.ContentLength, shared.size.toString())
                            }
                            call.respondOutputStream(
                                contentType = ContentType.Application.OctetStream,
                                status = HttpStatusCode.OK
                            ) {
                                stream.use { input -> input.copyTo(this) }
                            }
                        }

                        post("/api/web/upload") {
                            try {
                                val encodedName = call.request.header("X-Filename") ?: "shared_file"
                                val fileName = runCatching { java.net.URLDecoder.decode(encodedName, "UTF-8") }.getOrDefault(encodedName)
                                val settings = settingsRepository.getCurrentSettings()
                                val configuredDir = settings.downloadDirectory.ifBlank {
                                    PlatformInfo.getDefaultDownloadDirectory()
                                }
                                val finalFilePath = FileManager.getUniqueFilePath(configuredDir, fileName)
                                val finalFile = File(finalFilePath)
                                finalFile.parentFile?.mkdirs()

                                val channel = call.receiveChannel()
                                val fos = FileOutputStream(finalFile)
                                channel.copyTo(fos)
                                fos.close()

                                val isImage = isImageFile(fileName)
                                val actualSize = finalFile.length()
                                val transferId = UUID.randomUUID().toString()

                                val webDevice = PairedDevice(
                                    id = "web_browser",
                                    name = "Web Browser",
                                    platform = DevicePlatform.UNKNOWN,
                                    publicKeyFingerprint = "",
                                    lastKnownIp = call.request.local.remoteAddress,
                                    lastKnownPort = 0,
                                    pairedAt = System.currentTimeMillis(),
                                    autoSyncClipboard = false,
                                    isMuted = false
                                )
                                deviceRepository.savePairedDevice(webDevice)

                                val content = if (isImage) {
                                    MessageContent.ImageAttachment(
                                        fileName = fileName,
                                        fileSize = actualSize,
                                        filePath = finalFilePath,
                                        thumbnailBase64 = FileManager.generateImageThumbnailBase64(finalFilePath, 512)
                                    )
                                } else {
                                    MessageContent.FileAttachment(
                                        fileName = fileName,
                                        fileSize = actualSize,
                                        filePath = finalFilePath,
                                        mimeType = getMimeType(fileName),
                                        fileHash = ""
                                    )
                                }

                                val message = Message(
                                    id = transferId,
                                    deviceId = "web_browser",
                                    content = content,
                                    direction = Direction.RECEIVED,
                                    status = TransferStatus.Completed,
                                    timestamp = System.currentTimeMillis()
                                )
                                messageRepository.insertMessage(message)
                                PlatformInfo.scanMediaFile(finalFilePath)
                                Notifier.showNotification("Web Browser", "Received $fileName", "web_browser", isMuted = false)
                                call.respond(ApiResponse(success = true, message = finalFilePath))
                            } catch (e: Throwable) {
                                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = e.message ?: "Upload failed"))
                            }
                        }

                        post("/api/web/message") {
                            try {
                                val req = call.receive<WebMessageRequest>()
                                val transferId = UUID.randomUUID().toString()

                                val webDevice = PairedDevice(
                                    id = "web_browser",
                                    name = "Web Browser",
                                    platform = DevicePlatform.UNKNOWN,
                                    publicKeyFingerprint = "",
                                    lastKnownIp = call.request.local.remoteAddress,
                                    lastKnownPort = 0,
                                    pairedAt = System.currentTimeMillis(),
                                    autoSyncClipboard = false,
                                    isMuted = false
                                )
                                deviceRepository.savePairedDevice(webDevice)

                                val message = Message(
                                    id = transferId,
                                    deviceId = "web_browser",
                                    content = MessageContent.Text(req.text),
                                    direction = Direction.RECEIVED,
                                    status = TransferStatus.Completed,
                                    timestamp = System.currentTimeMillis()
                                )
                                messageRepository.insertMessage(message)
                                Notifier.showNotification("Web Browser", req.text, "web_browser", isMuted = false)
                                call.respond(ApiResponse(success = true))
                            } catch (e: Throwable) {
                                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = e.message ?: "Message failed"))
                            }
                        }
                    }
                }
                this.server = s
                s.start(wait = false)
                boundPort = currentPort
                isRunning = true
            } catch (e: Throwable) {
                attempts++
                currentPort++
                runCatching { this.server?.stop(100, 500) }
                this.server = null
            }
        }
        return boundPort
    }

    suspend fun stop() {
        isRunning = false
        runCatching { server?.stop(500, 1000) }
        server = null
        for (state in activeReceivingFiles.values) {
            runCatching {
                state.outputStream.close()
                state.partFile.delete()
            }
        }
        activeReceivingFiles.clear()
    }
}
