package com.bytegif.sendchat.network.client

import com.bytegif.sendchat.data.file.FileManager
import com.bytegif.sendchat.network.protocol.ApiResponse
import com.bytegif.sendchat.network.protocol.FileChunkPayload
import com.bytegif.sendchat.network.protocol.FileInitPayload
import com.bytegif.sendchat.network.protocol.MessagePayload
import com.bytegif.sendchat.network.protocol.PairRequest
import com.bytegif.sendchat.network.protocol.PairResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class TransferClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 300_000
        }
    }

    suspend fun sendPairRequest(targetIp: String, port: Int, request: PairRequest): PairResponse {
        return runCatching {
            val response = client.post("http://$targetIp:$port/api/pair") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.body<PairResponse>()
        }.getOrElse { error ->
            PairResponse(accepted = false, error = error.message ?: "Connection failed")
        }
    }

    suspend fun sendMessage(targetIp: String, port: Int, payload: MessagePayload): Boolean {
        return runCatching {
            val response = client.post("http://$targetIp:$port/api/message") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val apiRes = response.body<ApiResponse>()
            apiRes.success
        }.getOrDefault(false)
    }

    suspend fun sendFile(
        targetIp: String,
        port: Int,
        fileInit: FileInitPayload,
        localFilePath: String,
        onProgress: suspend (progress: Float, bytesSent: Long, totalBytes: Long) -> Unit
    ): Boolean {
        return runCatching {
            val inputStream = FileManager.openInputStream(localFilePath)
            if (inputStream == null) {
                println("[SendChat] Could not open stream for file: $localFilePath")
                return false
            }

            println("[SendChat] Initiating file transfer '${fileInit.fileName}' (${fileInit.fileSize} bytes) to $targetIp:$port...")

            // Step 1: Initialize File Transfer
            val initResponse = client.post("http://$targetIp:$port/api/file/init") {
                contentType(ContentType.Application.Json)
                setBody(fileInit)
            }
            if (initResponse.status.value !in 200..299) {
                println("[SendChat] /api/file/init HTTP error ${initResponse.status.value}")
                return false
            }
            val initResult = runCatching { initResponse.body<ApiResponse>() }.getOrNull()
            if (initResult?.success != true) {
                println("[SendChat] /api/file/init rejected by target: ${initResult?.message}")
                return false
            }

            // Step 2: Stream Chunks (512 KB high-throughput chunks)
            val chunkSize = 512 * 1024
            val totalSize = fileInit.fileSize
            val buffer = ByteArray(chunkSize)
            var bytesSent = 0L

            java.io.BufferedInputStream(inputStream, chunkSize).use { input ->
                var chunkIndex = 0
                while (currentCoroutineContext().isActive) {
                    val read = input.read(buffer)
                    val isLast = (read <= 0) || (bytesSent + maxOf(0, read) >= totalSize)

                    val chunkBytes = when {
                        read <= 0 -> ByteArray(0)
                        read == chunkSize -> buffer
                        else -> buffer.copyOf(read)
                    }

                    if (read > 0) {
                        bytesSent += read
                    }

                    val chunkPayload = FileChunkPayload(
                        transferId = fileInit.transferId,
                        chunkIndex = chunkIndex,
                        totalChunks = 0,
                        chunkDataBase64 = if (chunkBytes.isNotEmpty()) Base64.getEncoder().encodeToString(chunkBytes) else "",
                        isLast = isLast
                    )

                    val chunkRes = client.post("http://$targetIp:$port/api/file/chunk") {
                        contentType(ContentType.Application.Json)
                        setBody(chunkPayload)
                    }
                    if (chunkRes.status.value !in 200..299) {
                        println("[SendChat] /api/file/chunk HTTP error ${chunkRes.status.value} at chunk $chunkIndex")
                        return false
                    }
                    val chunkApi = runCatching { chunkRes.body<ApiResponse>() }.getOrNull()
                    if (chunkApi?.success != true) {
                        println("[SendChat] /api/file/chunk failed at chunk $chunkIndex: ${chunkApi?.message}")
                        return false
                    }

                    val progress = if (totalSize > 0) (bytesSent.toFloat() / totalSize).coerceIn(0f, 1f) else 1f
                    onProgress(progress, bytesSent, totalSize)

                    if (isLast) break
                    chunkIndex++
                }
            }
            println("[SendChat] File '${fileInit.fileName}' sent successfully to $targetIp:$port")
            true
        }.onFailure { error ->
            println("[SendChat] sendFile error to $targetIp:$port: ${error.message}")
            error.printStackTrace()
        }.getOrDefault(false)
    }

    fun close() {
        client.close()
    }
}
