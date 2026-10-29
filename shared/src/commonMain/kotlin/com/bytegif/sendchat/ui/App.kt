package com.bytegif.sendchat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.bytegif.sendchat.data.db.DriverFactory
import com.bytegif.sendchat.data.db.createDatabase
import com.bytegif.sendchat.data.repository.DeviceRepositoryImpl
import com.bytegif.sendchat.data.repository.MessageRepositoryImpl
import com.bytegif.sendchat.data.repository.SettingsRepositoryImpl
import com.bytegif.sendchat.discovery.createDiscoveryManager
import com.bytegif.sendchat.network.TransferManager
import com.bytegif.sendchat.network.client.TransferClient
import com.bytegif.sendchat.network.server.TransferServer
import com.bytegif.sendchat.platform.ClipboardManager
import com.bytegif.sendchat.platform.PlatformInfo
import com.bytegif.sendchat.ui.screens.MainScreen
import com.bytegif.sendchat.ui.theme.SendChatTheme
import com.bytegif.sendchat.viewmodel.ChatViewModel
import com.bytegif.sendchat.viewmodel.DeviceListViewModel
import com.bytegif.sendchat.viewmodel.PairingViewModel
import com.bytegif.sendchat.viewmodel.SettingsViewModel
import com.bytegif.sendchat.viewmodel.UpdateViewModel

class AppContainer(driverFactory: DriverFactory) {
    val database = createDatabase(driverFactory)
    val clipboardManager = ClipboardManager()
    val discoveryManager = createDiscoveryManager()

    val defaultDeviceId = PlatformInfo.generateDeviceId()
    val defaultDeviceName = PlatformInfo.getDefaultDeviceName()
    val defaultDownloadDir = PlatformInfo.getDefaultDownloadDirectory()

    val deviceRepository = DeviceRepositoryImpl(database)
    val messageRepository = MessageRepositoryImpl(database)
    val settingsRepository = SettingsRepositoryImpl(
        database = database,
        defaultDeviceId = defaultDeviceId,
        defaultDeviceName = defaultDeviceName,
        defaultDownloadDir = defaultDownloadDir
    )

    val transferServer = TransferServer(
        deviceRepository = deviceRepository,
        messageRepository = messageRepository,
        settingsRepository = settingsRepository,
        clipboardManager = clipboardManager
    )

    val transferClient = TransferClient()

    val transferManager = TransferManager(
        deviceRepository = deviceRepository,
        messageRepository = messageRepository,
        settingsRepository = settingsRepository,
        discoveryManager = discoveryManager,
        clipboardManager = clipboardManager,
        transferServer = transferServer,
        transferClient = transferClient
    )

    val deviceListViewModel = DeviceListViewModel(deviceRepository, transferManager)
    val chatViewModel = ChatViewModel(messageRepository, deviceRepository, transferManager, clipboardManager)
    val pairingViewModel = PairingViewModel(settingsRepository, transferManager)
    val settingsViewModel = SettingsViewModel(settingsRepository)
    val updateViewModel = UpdateViewModel()
}

@Composable
fun App(
    container: AppContainer,
    onPickFiles: () -> Unit = {},
    onPickImages: () -> Unit = {},
    onPickDownloadDir: (() -> Unit)? = null,
    onPickWebShareFiles: (() -> Unit)? = null
) {
    LaunchedEffect(Unit) {
        container.transferManager.initialize()
    }

    val settings by container.settingsViewModel.settings.collectAsState()

    SendChatTheme(themeMode = settings.themeMode) {
        MainScreen(
            deviceListViewModel = container.deviceListViewModel,
            chatViewModel = container.chatViewModel,
            pairingViewModel = container.pairingViewModel,
            settingsViewModel = container.settingsViewModel,
            updateViewModel = container.updateViewModel,
            onPickFiles = onPickFiles,
            onPickImages = onPickImages,
            onPickDownloadDir = onPickDownloadDir,
            onPickWebShareFiles = onPickWebShareFiles,
            onCopyText = { container.clipboardManager.setText(it) }
        )
    }
}
