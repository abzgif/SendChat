package com.bytegif.sendchat.platform

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

actual object Notifier {
    actual fun showNotification(title: String, body: String, deviceId: String?, isMuted: Boolean) {
        val os = System.getProperty("os.name")?.lowercase() ?: ""

        // 1. macOS native notification via SendChatAlert.app helper (shows SendChat icon and name, never Script Editor)
        if (os.contains("mac")) {
            val shown = MacNotifierHelper.show(title, body, isMuted)
            if (shown) return
        }

        // 2. Linux native notify-send
        if (os.contains("nix") || os.contains("nux")) {
            val handled = runCatching {
                Runtime.getRuntime().exec(arrayOf("notify-send", "SendChat: $title", body))
                true
            }.getOrDefault(false)

            if (handled) return
        }

        // 3. Fallback to SystemTray if supported (Windows / Desktop)
        runCatching {
            if (SystemTray.isSupported()) {
                val tray = SystemTray.getSystemTray()
                val trayIcons = tray.trayIcons
                if (trayIcons.isNotEmpty()) {
                    trayIcons[0].displayMessage("SendChat: $title", body, TrayIcon.MessageType.INFO)
                    return
                }
            }
        }

        // 4. Fallback system beep
        if (!isMuted) {
            runCatching {
                Toolkit.getDefaultToolkit().beep()
            }
        }
    }
}
