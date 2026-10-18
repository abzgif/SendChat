package com.bytegif.sendchat.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.KeySpec
import java.util.Base64
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

actual object CryptoEngine {

    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private var cachedIdentity: DeviceIdentity? = null

    actual fun getOrCreateDeviceIdentity(deviceId: String, deviceName: String): DeviceIdentity {
        cachedIdentity?.let { return it }

        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()
        val appDir = when {
            osName.contains("win") -> File(System.getenv("APPDATA") ?: userHome, "SendChat")
            osName.contains("mac") -> File(userHome, "Library/Application Support/SendChat")
            else -> File(System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config", "sendchat")
        }
        appDir.mkdirs()

        val certFile = File(appDir, "identity.cert")
        val keyFile = File(appDir, "identity.key")

        if (certFile.exists() && keyFile.exists()) {
            val certPem = certFile.readText()
            val keyPem = keyFile.readText()
            val fp = calculateFingerprint(certPem)
            val identity = DeviceIdentity(deviceId, fp, certPem, keyPem)
            cachedIdentity = identity
            return identity
        }

        // Generate new KeyPair and Self-Signed Certificate
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000L) // 10 years
        val serialNumber = BigInteger(64, SecureRandom())

        val subject = X500Name("CN=SendChat-$deviceId, O=SendChat, OU=$deviceName")
        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val x509Cert = JcaX509CertificateConverter().getCertificate(certHolder)

        val certPem = "-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(x509Cert.encoded) +
                "\n-----END CERTIFICATE-----\n"

        val keyPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded) +
                "\n-----END PRIVATE KEY-----\n"

        certFile.writeText(certPem)
        keyFile.writeText(keyPem)

        val fp = calculateFingerprint(certPem)
        val identity = DeviceIdentity(deviceId, fp, certPem, keyPem)
        cachedIdentity = identity
        return identity
    }

    actual fun calculateFingerprint(certificatePem: String): String {
        return runCatching {
            val cleanPem = certificatePem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\\s".toRegex(), "")
            val certBytes = Base64.getDecoder().decode(cleanPem)
            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        }.getOrDefault("")
    }

    actual fun encryptAesGcm(data: ByteArray, secretKey: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(secretKey.copyOf(32), "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val cipherText = cipher.doFinal(data)

        // Return IV + CipherText
        return iv + cipherText
    }

    actual fun decryptAesGcm(encryptedData: ByteArray, secretKey: ByteArray): ByteArray {
        require(encryptedData.size >= GCM_IV_LENGTH) { "Ciphertext too short" }
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(secretKey.copyOf(32), "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(cipherText)
    }

    actual fun deriveSharedKey(pin: String, salt: String): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), 10000, 256)
        return factory.generateSecret(spec).encoded
    }

    actual fun createServerSslContext(identity: DeviceIdentity): SSLContext {
        val certBytes = parsePem(identity.certificatePem)
        val keyBytes = parsePem(identity.privateKeyPem)

        val certFactory = CertificateFactory.getInstance("X.509")
        val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate

        val kf = java.security.KeyFactory.getInstance("RSA")
        val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
        val privateKey = kf.generatePrivate(keySpec)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("sendchat-server", privateKey, "".toCharArray(), arrayOf(cert))
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "".toCharArray())
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, null, SecureRandom())
        return sslContext
    }

    actual fun createClientSslContext(pinnedFingerprints: Set<String>): Pair<SSLContext, X509TrustManager> {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain.isNullOrEmpty()) throw java.security.cert.CertificateException("Empty certificate chain")
                if (pinnedFingerprints.isEmpty()) return // If no pinned fingerprints yet (pairing mode), accept

                val serverCert = chain[0]
                val digest = MessageDigest.getInstance("SHA-256").digest(serverCert.encoded)
                val fp = digest.joinToString(":") { "%02X".format(it) }

                if (!pinnedFingerprints.contains(fp)) {
                    throw java.security.cert.CertificateException("Certificate fingerprint mismatch! Got: $fp, expected one of: $pinnedFingerprints")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return Pair(sslContext, trustManager)
    }

    private fun parsePem(pem: String): ByteArray {
        val clean = pem.lines()
            .filterNot { it.startsWith("-----") }
            .joinToString("")
        return Base64.getDecoder().decode(clean)
    }
}
