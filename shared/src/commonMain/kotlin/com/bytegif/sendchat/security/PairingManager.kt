package com.bytegif.sendchat.security

import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.domain.model.PairingInfo
import java.security.SecureRandom

object PairingManager {

    fun generateSixDigitPin(): String {
        val random = SecureRandom()
        val pin = 100000 + random.nextInt(900000)
        return pin.toString()
    }

    fun encodePairingQrUrl(info: PairingInfo): String {
        return "sendchat://pair?" +
                "id=${info.deviceId}&" +
                "name=${info.deviceName.encodeUrl()}&" +
                "platform=${info.platform.name}&" +
                "ip=${info.ip}&" +
                "port=${info.port}&" +
                "pin=${info.pin}&" +
                "fp=${info.publicKeyFingerprint}"
    }

    fun parsePairingQrUrl(url: String): PairingInfo? {
        if (!url.startsWith("sendchat://pair?")) return null
        return runCatching {
            val query = url.substringAfter("sendchat://pair?")
            val params = query.split("&").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to parts.getOrElse(1) { "" }
            }
            PairingInfo(
                deviceId = params["id"] ?: return null,
                deviceName = params["name"]?.decodeUrl() ?: "Unknown Device",
                platform = DevicePlatform.fromString(params["platform"] ?: "UNKNOWN"),
                ip = params["ip"] ?: return null,
                port = params["port"]?.toIntOrNull() ?: 42831,
                pin = params["pin"] ?: return null,
                publicKeyFingerprint = params["fp"] ?: ""
            )
        }.getOrNull()
    }

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }

    private fun String.decodeUrl(): String {
        return java.net.URLDecoder.decode(this, "UTF-8")
    }
}
