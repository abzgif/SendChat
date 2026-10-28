package com.bytegif.sendchat.platform

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.bytegif.sendchat.data.db.AndroidAppContextHolder
import com.bytegif.sendchat.domain.model.DevicePlatform
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

actual object PlatformInfo {
    actual val platform: DevicePlatform = DevicePlatform.ANDROID

    actual fun getDefaultDeviceName(): String {
        val context = AndroidAppContextHolder.appContext
        val deviceName = if (context != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                } else {
                    null
                }
            }.getOrNull()
        } else null

        return if (!deviceName.isNullOrBlank()) {
            deviceName
        } else {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }
        }
    }

    actual fun getDefaultDownloadDirectory(): String {
        // Priority 1: Public Downloads/SendChat (/storage/emulated/0/Download/SendChat)
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val sendChatDir = File(publicDownloads, "SendChat")
        val isSendChatWritable = runCatching {
            sendChatDir.mkdirs()
            val test = File(sendChatDir, ".test_${System.currentTimeMillis()}")
            if (test.createNewFile()) {
                test.delete()
                true
            } else false
        }.getOrDefault(false)

        if (isSendChatWritable) {
            return sendChatDir.absolutePath
        }

        // Priority 2: Public Downloads root (/storage/emulated/0/Download)
        val isPublicWritable = runCatching {
            publicDownloads.mkdirs()
            val test = File(publicDownloads, ".test_${System.currentTimeMillis()}")
            if (test.createNewFile()) {
                test.delete()
                true
            } else false
        }.getOrDefault(false)

        if (isPublicWritable) {
            return publicDownloads.absolutePath
        }

        // Priority 3: App external files directory
        val context = AndroidAppContextHolder.appContext
        if (context != null) {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads")
            val appDownloads = File(baseDir, "SendChat")
            runCatching { appDownloads.mkdirs() }
            return appDownloads.absolutePath
        }

        return sendChatDir.absolutePath
    }

    actual fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    actual fun openFileInDefaultApp(filePath: String) {
        val context = AndroidAppContextHolder.appContext ?: return
        val file = File(filePath)
        if (!file.exists()) {
            runCatching {
                android.widget.Toast.makeText(context, "File does not exist at path: $filePath", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val ext = file.extension.lowercase()
            val mimeType = if (ext.isNotBlank()) {
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            } else null ?: context.contentResolver.getType(uri) ?: "*/*"

            if (ext == "apk" || mimeType == "application/vnd.android.package-archive") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    android.widget.Toast.makeText(
                        context,
                        "Allow SendChat to install unknown apps, then open the APK again.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                clipData = android.content.ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val launched = runCatching {
                val chooser = Intent.createChooser(intent, "Open with...").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            }.getOrDefault(false)

            if (!launched) {
                context.startActivity(intent)
            }
        }.onFailure {
            runCatching {
                android.widget.Toast.makeText(context, "No application found to open this file", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    actual fun openDirectoryInFileManager(directoryPath: String) {
        val context = AndroidAppContextHolder.appContext ?: return
        val dir = File(directoryPath)
        if (!dir.exists()) {
            runCatching { dir.mkdirs() }
        }

        var opened = false

        // 1. Try DocumentsContract with Downloads/SendChat
        if (!opened) {
            opened = runCatching {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.externalstorage.documents/document/primary:Download%2FSendChat"),
                        "vnd.android.document/directory"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        // 2. Try DocumentsContract with primary:Download
        if (!opened) {
            opened = runCatching {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"),
                        "vnd.android.document/directory"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        // 3. Try DownloadManager ACTION_VIEW_DOWNLOADS
        if (!opened) {
            opened = runCatching {
                val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        // 4. Try FileProvider directory intent
        if (!opened) {
            opened = runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dir)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        if (!opened) {
            runCatching {
                android.widget.Toast.makeText(context, "Saved to: ${dir.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
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
                        addr.hostAddress?.let { ips.add(it) }
                    }
                }
            }
        }
        return ips.ifEmpty { listOf("127.0.0.1") }
    }

    actual fun scanMediaFile(filePath: String) {
        val context = AndroidAppContextHolder.appContext ?: return
        runCatching {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                null
            ) { path, uri ->
                android.util.Log.d("SendChat", "Scanned media file into system index: $path -> $uri")
            }
        }
    }
}
