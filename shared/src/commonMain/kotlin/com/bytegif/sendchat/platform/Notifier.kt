package com.bytegif.sendchat.platform

expect object Notifier {
    fun showNotification(title: String, body: String, deviceId: String? = null, isMuted: Boolean = false)
}
