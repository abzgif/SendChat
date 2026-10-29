package com.bytegif.sendchat.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bytegif.sendchat.android.service.SendChatForegroundService
import com.bytegif.sendchat.data.db.AndroidAppContextHolder
import com.bytegif.sendchat.data.db.DriverFactory
import com.bytegif.sendchat.ui.App
import com.bytegif.sendchat.ui.AppContainer
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSendChatServiceSafely()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AndroidAppContextHolder.appContext = applicationContext

        appContainer = (application as SendChatApplication).appContainer

        // Request notification permission on Android 13+ and start service
        checkAndStartService()

        setContent {
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (!uris.isNullOrEmpty()) {
                    appContainer.chatViewModel.sendFiles(uris.map { it.toString() })
                }
            }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                if (!uris.isNullOrEmpty()) {
                    appContainer.chatViewModel.sendFiles(uris.map { it.toString() })
                }
            }

            val dirPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    val path = uri.path ?: ""
                    val docId = if (path.contains("/tree/")) path.substringAfter("/tree/") else path
                    val decoded = Uri.decode(docId)
                    val resolvedPath = when {
                        decoded.startsWith("primary:") -> {
                            val rel = decoded.removePrefix("primary:")
                            java.io.File(android.os.Environment.getExternalStorageDirectory(), rel).absolutePath
                        }
                        decoded.contains(":") -> {
                            val parts = decoded.split(":", limit = 2)
                            "/storage/${parts[0]}/${parts[1]}"
                        }
                        else -> {
                            val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            java.io.File(publicDownloads, decoded).absolutePath
                        }
                    }
                    if (resolvedPath.isNotBlank()) {
                        appContainer.settingsViewModel.updateDownloadDirectory(resolvedPath)
                    }
                }
            }

            val webSharePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (!uris.isNullOrEmpty()) {
                    uris.forEach { uri ->
                        runCatching {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        appContainer.transferManager.webShareManager.addFile(uri.toString())
                    }
                }
            }

            App(
                container = appContainer,
                onPickFiles = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                onPickImages = {
                    imagePickerLauncher.launch("image/*")
                },
                onPickDownloadDir = {
                    dirPickerLauncher.launch(null)
                },
                onPickWebShareFiles = {
                    webSharePickerLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        handleIncomingIntent(intent)
    }

    private fun checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startSendChatServiceSafely()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startSendChatServiceSafely()
        }
    }

    private fun startSendChatServiceSafely() {
        runCatching {
            val serviceIntent = Intent(this, SendChatForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }.onFailure { e ->
            Log.w("SendChat", "Failed to start foreground service", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Direct navigation when tapping an incoming notification
        val deviceId = intent.getStringExtra("EXTRA_DEVICE_ID")
        if (!deviceId.isNullOrBlank()) {
            appContainer.chatViewModel.openChatWithDeviceId(deviceId)
            appContainer.deviceListViewModel.selectDeviceById(deviceId)
        }

        // 2. Incoming shared files/images from other apps
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    appContainer.chatViewModel.sendFiles(listOf(uri.toString()))
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    appContainer.chatViewModel.sendFiles(uris.map { it.toString() })
                }
            }
        }
    }
}
