package com.bytegif.sendchat.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CryptoEngineTest {

    @Test
    fun testDeviceIdentityAndFingerprint() {
        val identity = CryptoEngine.getOrCreateDeviceIdentity("test-device-id", "Test Device")
        assertNotNull(identity)
        assertEquals("test-device-id", identity.deviceId)
        assertTrue(identity.publicKeyFingerprint.isNotBlank())
        assertTrue(identity.publicKeyFingerprint.contains(":"))
        assertTrue(identity.certificatePem.contains("BEGIN CERTIFICATE"))
        assertTrue(identity.privateKeyPem.contains("BEGIN PRIVATE KEY"))
    }

    @Test
    fun testAesGcmEncryptionDecryptionRoundtrip() {
        val secretKey = CryptoEngine.deriveSharedKey("123456", "test-salt")
        val originalText = "Hello from SendChat test suite! 🚀 123456"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = CryptoEngine.encryptAesGcm(originalBytes, secretKey)
        assertTrue(encrypted.size > originalBytes.size)

        val decrypted = CryptoEngine.decryptAesGcm(encrypted, secretKey)
        val decryptedText = String(decrypted, Charsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testPinAndQrParsing() {
        val pin = PairingManager.generateSixDigitPin()
        assertEquals(6, pin.length)
        assertTrue(pin.all { it.isDigit() })

        val info = com.bytegif.sendchat.domain.model.PairingInfo(
            deviceId = "dev-123",
            deviceName = "MacBook Pro",
            platform = com.bytegif.sendchat.domain.model.DevicePlatform.MACOS,
            ip = "192.168.1.50",
            port = 42831,
            pin = pin,
            publicKeyFingerprint = "AA:BB:CC:DD"
        )

        val qrUrl = PairingManager.encodePairingQrUrl(info)
        assertTrue(qrUrl.startsWith("sendchat://pair?"))

        val parsed = PairingManager.parsePairingQrUrl(qrUrl)
        assertNotNull(parsed)
        assertEquals(info.deviceId, parsed.deviceId)
        assertEquals(info.deviceName, parsed.deviceName)
        assertEquals(info.platform, parsed.platform)
        assertEquals(info.ip, parsed.ip)
        assertEquals(info.port, parsed.port)
        assertEquals(info.pin, parsed.pin)
        assertEquals(info.publicKeyFingerprint, parsed.publicKeyFingerprint)
    }
}
