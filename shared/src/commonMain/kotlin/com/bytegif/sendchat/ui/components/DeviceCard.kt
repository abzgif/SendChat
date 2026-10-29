package com.bytegif.sendchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.ui.theme.OfflineGray
import com.bytegif.sendchat.ui.theme.OnlineGreen

@Composable
fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPairClick: (() -> Unit)? = null,
    onForgetClick: (() -> Unit)? = null,
    onRenameClick: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform Icon Avatar with soft tinted background
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPlatformIcon(device.platform),
                    contentDescription = device.platform.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                // Online indicator badge with clean white/dark outline
                Box(
                    modifier = Modifier
                    .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(if (device.isOnline) OnlineGreen else OfflineGray)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Device Name & Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        color = contentColor,
                        maxLines = 1
                    )
                    if (device.isMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${device.platform.displayName} • ${device.ip}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Actions
            if (!device.isPaired && onPairClick != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable(onClick = onPairClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Pair",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pair",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            } else {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Device Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.width(248.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (device.isOnline) "Connected now" else "Saved device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        if (onToggleMute != null) {
                            DropdownMenuItem(
                                text = { Text(if (device.isMuted) "Unmute Notification Sound" else "Mute Notification Sound") },
                                leadingIcon = {
                                    Icon(
                                        if (device.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleMute()
                                }
                            )
                        }
                        if (onRenameClick != null) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRenameClick()
                                }
                            )
                        }
                        if (onForgetClick != null) {
                            DropdownMenuItem(
                                text = { Text("Forget Device") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onForgetClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getPlatformIcon(platform: DevicePlatform): ImageVector {
    return when (platform) {
        DevicePlatform.ANDROID -> Icons.Default.Android
        DevicePlatform.WINDOWS -> Icons.Default.DesktopWindows
        DevicePlatform.MACOS -> Icons.Default.DesktopMac
        DevicePlatform.LINUX -> Icons.Default.Computer
        DevicePlatform.UNKNOWN -> Icons.Default.Computer
    }
}
