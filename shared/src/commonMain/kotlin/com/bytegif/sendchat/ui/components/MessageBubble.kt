package com.bytegif.sendchat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytegif.sendchat.domain.model.Direction
import com.bytegif.sendchat.domain.model.Message
import com.bytegif.sendchat.domain.model.MessageContent
import com.bytegif.sendchat.domain.model.TransferStatus
import com.bytegif.sendchat.platform.PlatformInfo
import com.bytegif.sendchat.ui.theme.ClipboardBadgeColor
import com.bytegif.sendchat.ui.theme.ErrorRed
import com.bytegif.sendchat.ui.util.ImageBitmapLoader
import com.bytegif.sendchat.ui.util.dragAndDropFileExport
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onCopyText: (String) -> Unit,
    onRetryMessage: (Message) -> Unit,
    onCancelTransfer: (String) -> Unit,
    onDeleteMessage: (Message, Boolean) -> Unit,
    onImageClick: ((String) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSent = message.direction == Direction.SENT
    val alignment = if (isSent) Alignment.End else Alignment.Start

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val hasPhysicalFile = !isSent && when (val c = message.content) {
        is MessageContent.FileAttachment -> c.filePath.isNotBlank() && File(c.filePath).exists()
        is MessageContent.ImageAttachment -> c.filePath.isNotBlank() && File(c.filePath).exists()
        else -> false
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Delete Message")
            },
            text = {
                Text(
                    text = if (hasPhysicalFile) {
                        "Do you also want to permanently delete the downloaded file from your device storage?"
                    } else {
                        "Are you sure you want to delete this message?"
                    }
                )
            },
            confirmButton = {
                if (hasPhysicalFile) {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteMessage(message, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Delete File & Message")
                    }
                } else {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteMessage(message, false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                if (hasPhysicalFile) {
                    OutlinedButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteMessage(message, false)
                        }
                    ) {
                        Text("Delete Message Only")
                    }
                } else {
                    OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    val bubbleShape = if (isSent) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    val bubbleColor = if (isSent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val onBubbleColor = if (isSent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleBorder = if (isSent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, bubbleBorder),
            modifier = Modifier.widthIn(max = 560.dp),
            tonalElevation = if (isSent) 2.dp else 0.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (val content = message.content) {
                    is MessageContent.Text -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = content.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = onBubbleColor,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = onBubbleColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.widthIn(min = 180.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 4.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Copy Text") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onCopyText(content.text)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                                        onClick = {
                                            menuExpanded = false
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is MessageContent.Clipboard -> {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = onBubbleColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = onBubbleColor.copy(alpha = 0.9f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Clipboard",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onBubbleColor.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { onCopyText(content.text) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = onBubbleColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = onBubbleColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = content.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = onBubbleColor
                            )
                        }
                    }

                    is MessageContent.FileAttachment -> {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dragAndDropFileExport(content.filePath)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(onBubbleColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = onBubbleColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = content.fileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = onBubbleColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = formatFileSize(content.fileSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = onBubbleColor.copy(alpha = 0.7f)
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete File",
                                        tint = onBubbleColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Progress with Cancel Button
                            when (val status = message.status) {
                                is TransferStatus.InProgress -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { status.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = onBubbleColor,
                                        trackColor = onBubbleColor.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(status.progress * 100).toInt()}% • ${formatFileSize(status.bytesTransferred)} of ${formatFileSize(status.totalBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onBubbleColor.copy(alpha = 0.7f)
                                        )

                                        // Cancel Button
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = onBubbleColor.copy(alpha = 0.15f),
                                            modifier = Modifier.clickable { onCancelTransfer(message.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel",
                                                    tint = onBubbleColor.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Cancel",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = onBubbleColor.copy(alpha = 0.9f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                is TransferStatus.Completed -> {
                                    if (content.filePath.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = onBubbleColor.copy(alpha = 0.12f),
                                                modifier = Modifier.clickable {
                                                    PlatformInfo.openFileInDefaultApp(content.filePath)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                        contentDescription = null,
                                                        tint = onBubbleColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Open",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = onBubbleColor
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = onBubbleColor.copy(alpha = 0.12f),
                                                modifier = Modifier.clickable {
                                                    val dir = File(content.filePath).parent ?: ""
                                                    if (dir.isNotBlank()) {
                                                        PlatformInfo.openDirectoryInFileManager(dir)
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = onBubbleColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Show in folder",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = onBubbleColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    is MessageContent.ImageAttachment -> {
                        val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                            initialValue = null,
                            key1 = content.filePath,
                            key2 = content.thumbnailBase64
                        ) {
                            value = withContext(Dispatchers.IO) {
                                val fromFile = content.filePath
                                    .takeIf { it.isNotBlank() }
                                    ?.let { ImageBitmapLoader.loadFromPath(it) }
                                fromFile ?: content.thumbnailBase64
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { ImageBitmapLoader.loadFromBase64(it) }
                            }
                        }

                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = 320.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(onBubbleColor.copy(alpha = 0.08f))
                                    .dragAndDropFileExport(content.filePath)
                                    .combinedClickable(
                                        onClick = {
                                            if (content.filePath.isNotBlank()) {
                                                onImageClick?.invoke(content.filePath)
                                            }
                                        },
                                        onLongClick = onLongClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                imageBitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = content.fileName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: run {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Image preview",
                                        tint = onBubbleColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                // Bottom label overlay
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = content.fileName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatFileSize(content.fileSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }

                                // Top overlay buttons
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                            .size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Image",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (message.status is TransferStatus.InProgress) {
                                val inProgress = message.status as TransferStatus.InProgress
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { inProgress.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = onBubbleColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(inProgress.progress * 100).toInt()}% • Sending...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = onBubbleColor.copy(alpha = 0.7f)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = onBubbleColor.copy(alpha = 0.15f),
                                        modifier = Modifier.clickable { onCancelTransfer(message.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = onBubbleColor.copy(alpha = 0.9f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Cancel",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = onBubbleColor.copy(alpha = 0.9f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp and Status indicator
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = onBubbleColor.copy(alpha = 0.6f)
                    )

                    if (isSent) {
                        when (val status = message.status) {
                            is TransferStatus.Completed -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Delivered",
                                    tint = onBubbleColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            is TransferStatus.InProgress -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = onBubbleColor
                                )
                            }

                            is TransferStatus.Failed -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = onBubbleColor.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRetryMessage(message) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Failed",
                                            tint = onBubbleColor.copy(alpha = 0.9f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (status.reason.isNotBlank()) status.reason else "Delivery Failed • Tap to Retry",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = onBubbleColor.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Retry",
                                            tint = onBubbleColor.copy(alpha = 0.9f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "%.1f %s".format(Locale.US, value, units[digitGroups.coerceIn(0, units.size - 1)])
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
