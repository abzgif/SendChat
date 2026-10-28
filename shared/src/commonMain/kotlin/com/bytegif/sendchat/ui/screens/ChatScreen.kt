package com.bytegif.sendchat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Computer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.Direction
import com.bytegif.sendchat.domain.model.MessageContent
import com.bytegif.sendchat.ui.components.ChatInputBar
import com.bytegif.sendchat.ui.components.MessageBubble
import com.bytegif.sendchat.ui.theme.OfflineGray
import com.bytegif.sendchat.ui.theme.OnlineGreen
import com.bytegif.sendchat.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    device: Device?,
    viewModel: ChatViewModel,
    onBack: (() -> Unit)? = null,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (device == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select a device to start sharing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LaunchedEffect(device.id) {
        viewModel.setActiveDevice(device)
    }

    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (messages.size - 1).coerceAtLeast(0)
    )
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearWithFilesDialog by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteSelectionDialog by remember { mutableStateOf(false) }
    var alsoDeleteFilesFromStorage by remember { mutableStateOf(true) }

    if (showDeleteSelectionDialog) {
        val selectedCount = selectedMessageIds.size
        val hasPhysicalFiles = messages.filter { it.id in selectedMessageIds }.any {
            it.direction == Direction.RECEIVED && when (val c = it.content) {
                is MessageContent.FileAttachment -> c.filePath.isNotBlank()
                is MessageContent.ImageAttachment -> c.filePath.isNotBlank()
                else -> false
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteSelectionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete $selectedCount Message${if (selectedCount > 1) "s" else ""}?") },
            text = {
                Column {
                    Text("Are you sure you want to delete the selected message${if (selectedCount > 1) "s" else ""}?")
                    if (hasPhysicalFiles) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .clickable { alsoDeleteFilesFromStorage = !alsoDeleteFilesFromStorage }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = alsoDeleteFilesFromStorage,
                                onCheckedChange = { alsoDeleteFilesFromStorage = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Also delete downloaded files from storage",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectionDialog = false
                        viewModel.deleteMessages(selectedMessageIds, deleteFiles = hasPhysicalFiles && alsoDeleteFilesFromStorage)
                        isSelectionMode = false
                        selectedMessageIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteSelectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Chat History") },
            text = { Text("Are you sure you want to delete all messages with ${device.name}? Downloaded files will remain on your storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearChatHistory(deleteFiles = false)
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearWithFilesDialog) {
        AlertDialog(
            onDismissRequest = { showClearWithFilesDialog = false },
            title = { Text("Delete Chat History with Files Too?") },
            text = { Text("This will permanently delete all message history AND remove all downloaded files and images from ${device.name} from your device storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearWithFilesDialog = false
                        viewModel.clearChatHistory(deleteFiles = true)
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWithFilesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var hasScrolledToBottomInitially by remember(device.id) { mutableStateOf(false) }

    LaunchedEffect(device.id) {
        hasScrolledToBottomInitially = false
    }

    LaunchedEffect(messages.size, hasScrolledToBottomInitially) {
        if (messages.isNotEmpty()) {
            if (!hasScrolledToBottomInitially) {
                listState.scrollToItem(messages.size - 1)
                hasScrolledToBottomInitially = true
            } else {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top Bar
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedMessageIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedMessageIds = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection"
                            )
                        }
                    },
                    actions = {
                        val allSelected = messages.isNotEmpty() && selectedMessageIds.size == messages.size
                        IconButton(
                            onClick = {
                                selectedMessageIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    messages.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All"
                            )
                        }

                        IconButton(
                            onClick = { showDeleteSelectionDialog = true },
                            enabled = selectedMessageIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedMessageIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (device.platform) {
                                        com.bytegif.sendchat.domain.model.DevicePlatform.ANDROID -> Icons.Default.Android
                                        com.bytegif.sendchat.domain.model.DevicePlatform.WINDOWS -> Icons.Default.DesktopWindows
                                        com.bytegif.sendchat.domain.model.DevicePlatform.MACOS -> Icons.Default.DesktopMac
                                        com.bytegif.sendchat.domain.model.DevicePlatform.LINUX -> Icons.Default.Computer
                                        com.bytegif.sendchat.domain.model.DevicePlatform.UNKNOWN -> Icons.Default.Computer
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (device.isOnline) OnlineGreen else OfflineGray)
                                    )
                                    if (device.isMuted) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.NotificationsOff,
                                            contentDescription = "Muted",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (device.isOnline) "Online • ${device.ip}" else "Offline • ${device.ip}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.width(300.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                                    Text(
                                        text = "Conversation settings",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Notification sound",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (device.isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = device.isMuted,
                                            onCheckedChange = {
                                                viewModel.toggleDeviceMute(it)
                                            },
                                            modifier = Modifier.padding(start = 8.dp),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleDeviceMute(!device.isMuted)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Sync clipboard automatically",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                    trailingIcon = {
                                        Switch(
                                            checked = device.autoSyncClipboard,
                                            onCheckedChange = {
                                                viewModel.toggleClipboardAutoSync(it)
                                            },
                                            modifier = Modifier.padding(start = 8.dp),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleClipboardAutoSync(!device.autoSyncClipboard)
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                if (messages.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Select Messages") },
                                        leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            isSelectionMode = true
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Clear Chat History") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        showClearHistoryDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete Chat History with Files Too",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showClearWithFilesDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }

            // Message List
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Send text, clipboard snippets, or attach files and images below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isSelected = selectedMessageIds.contains(msg.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1040.dp)
                                .align(Alignment.CenterHorizontally)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedMessageIds = if (isSelected) {
                                                selectedMessageIds - msg.id
                                            } else {
                                                selectedMessageIds + msg.id
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        isSelectionMode = true
                                        selectedMessageIds = if (isSelected) {
                                            selectedMessageIds - msg.id
                                        } else {
                                            selectedMessageIds + msg.id
                                        }
                                    }
                                )
                                .background(
                                    if (isSelectionMode && isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else Color.Transparent
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedMessageIds = if (checked) {
                                            selectedMessageIds + msg.id
                                        } else {
                                            selectedMessageIds - msg.id
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MessageBubble(
                                    message = msg,
                                    onCopyText = { viewModel.copyMessageToClipboard(it) },
                                    onRetryMessage = { viewModel.retryMessage(it) },
                                    onCancelTransfer = { viewModel.cancelTransfer(it) },
                                    onDeleteMessage = { m, deleteStorage -> viewModel.deleteMessageAndFile(m, deleteStorage) },
                                    onImageClick = if (!isSelectionMode) onImageClick else { _ ->
                                        selectedMessageIds = if (isSelected) selectedMessageIds - msg.id else selectedMessageIds + msg.id
                                    },
                                    onLongClick = {
                                        isSelectionMode = true
                                        selectedMessageIds = if (isSelected) {
                                            selectedMessageIds - msg.id
                                        } else {
                                            selectedMessageIds + msg.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            ChatInputBar(
                text = inputText,
                onTextChange = { viewModel.inputText.value = it },
                onSendText = { viewModel.sendTextMessage() },
                onSendClipboard = { viewModel.sendCurrentClipboard() },
                onPickFiles = onPickFiles,
                onPickImages = onPickImages,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}
