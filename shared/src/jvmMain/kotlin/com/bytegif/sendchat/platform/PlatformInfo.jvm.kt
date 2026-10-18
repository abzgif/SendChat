package com.bytegif.sendchat.platform

import com.bytegif.sendchat.domain.model.DevicePlatform
import java.awt.Desktop
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID

actual object PlatformInfo {
    actual val platform: DevicePlatform
        get() {
            val os = System.getProperty("os.name", "").lowercase()
            return when {
                os.contains("win") -> DevicePlatform.WINDOWS
                os.contains("mac") -> DevicePlatform.MACOS
                os.contains("nix") || os.contains("nux") || os.contains("aix") -> DevicePlatform.LINUX
                else -> DevicePlatform.LINUX
            }
        }

    actual fun getDefaultDeviceName(): String {
        return runCatching {
            val hostname = InetAddress.getLocalHost().hostName
            if (!hostname.isNullOrBlank()) hostname else System.getProperty("user.name", "Desktop Device")
        }.getOrDefault("Desktop Device")
    }

    actual fun getDefaultDownloadDirectory(): String {
        val userHome = System.getProperty("user.home", "")
        val dir = File(userHome, "Downloads/SendChat")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    actual fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    actual fun openFileInDefaultApp(filePath: String) {
        runCatching {
            val file = File(filePath)
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            }
        }
    }

    actual fun openDirectoryInFileManager(directoryPath: String) {
        runCatching {
            val dir = File(directoryPath)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir)
            }
        }
    }

    actual fun getLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        ips.add(addr.hostAddress)
                    }
                }
            }
        }
        return ips.ifEmpty { listOf("127.0.0.1") }
    }

    actual fun scanMediaFile(filePath: String) {
        // No-op on JVM/desktop
    }
}
