package com.bytegif.sendchat.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.ui.components.IncomingPairRequestDialog
import com.bytegif.sendchat.viewmodel.ChatViewModel
import com.bytegif.sendchat.viewmodel.DeviceListViewModel
import com.bytegif.sendchat.viewmodel.PairingViewModel
import com.bytegif.sendchat.viewmodel.SettingsViewModel
import com.bytegif.sendchat.viewmodel.UpdateViewModel

import com.bytegif.sendchat.ui.util.BackHandler
import com.bytegif.sendchat.ui.util.onExternalFileDrop

enum class MobileCurrentScreen {
    DEVICE_LIST,
    CHAT,
    SETTINGS
}

@Composable
fun MainScreen(
    deviceListViewModel: DeviceListViewModel,
    chatViewModel: ChatViewModel,
    pairingViewModel: PairingViewModel,
    settingsViewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onPickDownloadDir: (() -> Unit)? = null,
    onPickWebShareFiles: (() -> Unit)? = null,
    onCopyText: (String) -> Unit
) {
    var mobileScreen by remember { mutableStateOf(MobileCurrentScreen.DEVICE_LIST) }
    var showSettings by remember { mutableStateOf(false) }

    var pairingTargetDevice by remember { mutableStateOf<Device?>(null) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showWebShareDialog by remember { mutableStateOf(false) }

    var viewingImageFilePath by remember { mutableStateOf<String?>(null) }

    val incomingPrompt by pairingViewModel.incomingPrompt.collectAsState()
    val activeDevice by chatViewModel.activeDevice.collectAsState()
    val deviceList by deviceListViewModel.devices.collectAsState()

    // Always resolve the live up-to-date Device object (with autoSyncClipboard updates)
    val currentDevice = deviceList.firstOrNull { it.id == activeDevice?.id } ?: activeDevice

    // Automatically navigate to chat when an active device is selected (e.g. via notification tap)
    LaunchedEffect(activeDevice?.id) {
        if (activeDevice != null) {
            mobileScreen = MobileCurrentScreen.CHAT
            showSettings = false
        }
    }

    // Handle back button navigation for dialogs and screens
    if (viewingImageFilePath != null) {
        BackHandler { viewingImageFilePath = null }
    } else if (showPairingDialog) {
        BackHandler {
            showPairingDialog = false
            pairingTargetDevice = null
            pairingViewModel.resetState()
        }
    } else if (showWebShareDialog) {
        BackHandler { showWebShareDialog = false }
    } else if (showSettings) {
        BackHandler { showSettings = false }
    } else if (mobileScreen != MobileCurrentScreen.DEVICE_LIST) {
        BackHandler {
            if (mobileScreen == MobileCurrentScreen.CHAT) {
                chatViewModel.setActiveDevice(null)
                deviceListViewModel.selectDevice(null)
            }
            mobileScreen = MobileCurrentScreen.DEVICE_LIST
        }
    }

    // Global Incoming Pair Request Prompt
    incomingPrompt?.let { prompt ->
        IncomingPairRequestDialog(
            request = prompt.request,
            onAccept = { pairingViewModel.respondToIncomingPair(true) },
            onDecline = { pairingViewModel.respondToIncomingPair(false) }
        )
    }

    // Global Pairing Dialog
    if (showPairingDialog) {
        PairingDialog(
            targetDevice = pairingTargetDevice,
            viewModel = pairingViewModel,
            onDismiss = {
                showPairingDialog = false
                pairingTargetDevice = null
                pairingViewModel.resetState()
            },
            onPairedSuccess = { device ->
                chatViewModel.setActiveDevice(device)
                deviceListViewModel.selectDevice(device)
                if (mobileScreen == MobileCurrentScreen.DEVICE_LIST) {
                    mobileScreen = MobileCurrentScreen.CHAT
                }
            }
        )
    }

    // Global Web Share Dialog
    if (showWebShareDialog) {
        WebShareDialog(
            transferManager = deviceListViewModel.transferManager,
            onDismiss = { showWebShareDialog = false },
            onPickFilesToShare = onPickWebShareFiles,
            onCopyText = onCopyText
        )
    }

    // Global Image Viewer
    viewingImageFilePath?.let { filePath ->
        ImageViewerDialog(
            filePath = filePath,
            onDismiss = { viewingImageFilePath = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .onExternalFileDrop { paths ->
                chatViewModel.sendFiles(paths)
            }
    ) { _ ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isWide = maxWidth >= 720.dp

            if (isWide) {
                // Desktop / Tablet Two-Pane Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                    ) {
                        DeviceListScreen(
                            viewModel = deviceListViewModel,
                            onOpenChat = { device ->
                                chatViewModel.setActiveDevice(device)
                                showSettings = false
                            },
                            onOpenPairing = { device ->
                                pairingTargetDevice = device
                                showPairingDialog = true
                            },
                            onOpenSettings = {
                                showSettings = true
                            },
                            onOpenWebShare = {
                                showWebShareDialog = true
                            }
                        )
                    }

                    VerticalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 1.dp
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (showSettings) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                updateViewModel = updateViewModel,
                                onBack = { showSettings = false },
                                onPickDownloadDir = onPickDownloadDir
                            )
                        } else {
                            ChatScreen(
                                device = currentDevice,
                                viewModel = chatViewModel,
                                onBack = null,
                                onPickFiles = onPickFiles,
                                onPickImages = onPickImages,
                                onImageClick = { viewingImageFilePath = it }
                            )
                        }
                    }
                }
            } else {
                // Mobile Single-Pane Layout
                when (mobileScreen) {
                    MobileCurrentScreen.DEVICE_LIST -> {
                        DeviceListScreen(
                            viewModel = deviceListViewModel,
                            onOpenChat = { device ->
                                chatViewModel.setActiveDevice(device)
                                mobileScreen = MobileCurrentScreen.CHAT
                            },
                            onOpenPairing = { device ->
                                pairingTargetDevice = device
                                showPairingDialog = true
                            },
                            onOpenSettings = {
                                mobileScreen = MobileCurrentScreen.SETTINGS
                            },
                            onOpenWebShare = {
                                showWebShareDialog = true
                            }
                        )
                    }

                    MobileCurrentScreen.CHAT -> {
                        ChatScreen(
                            device = currentDevice,
                            viewModel = chatViewModel,
                            onBack = {
                                chatViewModel.setActiveDevice(null)
                                deviceListViewModel.selectDevice(null)
                                mobileScreen = MobileCurrentScreen.DEVICE_LIST
                            },
                            onPickFiles = onPickFiles,
                            onPickImages = onPickImages,
                            onImageClick = { viewingImageFilePath = it }
                        )
                    }

                    MobileCurrentScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            updateViewModel = updateViewModel,
                            onBack = { mobileScreen = MobileCurrentScreen.DEVICE_LIST },
                            onPickDownloadDir = onPickDownloadDir
                        )
                    }
                }
            }
        }
    }
}
