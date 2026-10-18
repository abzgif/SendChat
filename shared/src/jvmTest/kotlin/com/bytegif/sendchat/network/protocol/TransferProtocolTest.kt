package com.bytegif.sendchat.network.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferProtocolTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testPairRequestSerialization() {
        val request = PairRequest(
            deviceId = "dev-abc",
            deviceName = "Pixel 9",
            platform = "ANDROID",
            port = 42831,
            publicKeyFingerprint = "11:22:33:44",
            certificatePem = "CERT_DATA"
        )
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<PairRequest>(serialized)

        assertEquals(request, deserialized)
    }

    @Test
    fun testUdpBeaconPayloadSerialization() {
        val beacon = UdpBeaconPayload(
            protocol = "sendchat_v1",
            deviceId = "dev-abc",
            deviceName = "Pixel 9",
            platform = "ANDROID",
            port = 42831,
            publicKeyFingerprint = "11:22:33:44",
            isReply = false
        )
        val serialized = json.encodeToString(beacon)
        val deserialized = json.decodeFromString<UdpBeaconPayload>(serialized)

        assertEquals(beacon, deserialized)
    }

    @Test
    fun testMessagePayloadSerialization() {
        val payload = MessagePayload(
            messageId = "msg-999",
            senderDeviceId = "dev-123",
            type = "CLIPBOARD",
            text = "Copied text from clipboard",
            timestamp = 1700000000000L
        )
        val serialized = json.encodeToString(payload)
        val deserialized = json.decodeFromString<MessagePayload>(serialized)

        assertEquals(payload, deserialized)
    }

    @Test
    fun testFileInitAndChunkPayloadSerialization() {
        val fileInit = FileInitPayload(
            transferId = "transfer-123",
            senderDeviceId = "dev-456",
            fileName = "photo.jpg",
            fileSize = 2048576L,
            mimeType = "image/jpeg",
            fileHash = "abc123hash",
            isImage = true,
            thumbnailBase64 = "base64thumb",
            timestamp = 1700000000000L
        )
        val fileInitSerialized = json.encodeToString(fileInit)
        val fileInitDeserialized = json.decodeFromString<FileInitPayload>(fileInitSerialized)
        assertEquals(fileInit, fileInitDeserialized)

        val chunk = FileChunkPayload(
            transferId = "transfer-123",
            chunkIndex = 3,
            totalChunks = 10,
            chunkDataBase64 = "ABCD1234==",
            isLast = false
        )
        val chunkSerialized = json.encodeToString(chunk)
        val chunkDeserialized = json.decodeFromString<FileChunkPayload>(chunkSerialized)
        assertEquals(chunk, chunkDeserialized)
    }
}
