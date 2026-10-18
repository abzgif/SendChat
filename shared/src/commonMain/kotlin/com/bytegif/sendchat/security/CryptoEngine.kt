package com.bytegif.sendchat.security

import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class DeviceIdentity(
    val deviceId: String,
    val publicKeyFingerprint: String,
    val certificatePem: String,
    val privateKeyPem: String
)

expect object CryptoEngine {
    fun getOrCreateDeviceIdentity(deviceId: String, deviceName: String): DeviceIdentity
    fun calculateFingerprint(certificatePem: String): String
    fun encryptAesGcm(data: ByteArray, secretKey: ByteArray): ByteArray
    fun decryptAesGcm(encryptedData: ByteArray, secretKey: ByteArray): ByteArray
    fun deriveSharedKey(pin: String, salt: String): ByteArray
    fun createServerSslContext(identity: DeviceIdentity): SSLContext
    fun createClientSslContext(pinnedFingerprints: Set<String>): Pair<SSLContext, X509TrustManager>
}
