package com.bytegif.sendchat.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.bytegif.sendchat.data.db.DriverFactory
import com.bytegif.sendchat.platform.PlatformInfo
import com.bytegif.sendchat.ui.App
import com.bytegif.sendchat.ui.AppContainer
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.imageio.ImageIO

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val userHome = System.getProperty("user.home", ".")
            val logFile = File(userHome, "SendChat-crash.log")
            logFile.writeText("SendChat Crash on thread [${thread.name}]:\n${throwable.stackTraceToString()}\n")
        }
        throwable.printStackTrace()
    }

    application {
        val trayState = rememberTrayState()
        val windowState = rememberWindowState(width = 1050.dp, height = 720.dp)
        val driverFactory = remember { DriverFactory() }
        val container = remember { AppContainer(driverFactory) }

        val appIcon = remember {
            runCatching {
                val stream = object {}.javaClass.getResourceAsStream("/icon.png")
                if (stream != null) {
                    val bufferedImage = ImageIO.read(stream)
                    bufferedImage?.toComposeImageBitmap()
                } else null
            }.getOrNull()
        }

        val trayIcon = remember {
            runCatching {
                val stream = object {}.javaClass.getResourceAsStream("/tray_icon.png")
                    ?: object {}.javaClass.getResourceAsStream("/icon.png")
                if (stream != null) {
                    val bufferedImage = ImageIO.read(stream)
                    bufferedImage?.toComposeImageBitmap()
                } else null
            }.getOrNull()
        }

        val iconPainter = appIcon?.let { BitmapPainter(it) }
        val trayPainter = trayIcon?.let { BitmapPainter(it) } ?: iconPainter

        val isTraySupported = remember {
            runCatching { java.awt.SystemTray.isSupported() }.getOrDefault(false)
        }

        if (isTraySupported && trayPainter != null) {
            Tray(
                state = trayState,
                icon = trayPainter,
                tooltip = "SendChat",
                onAction = {}
            )
        }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "SendChat — Local Network Sharing",
        icon = iconPainter
    ) {
        val window = this.window

        // Setup native OS Drag & Drop on the window
        remember(window) {
            val dropListener = object : DropTargetAdapter() {
                override fun dragEnter(dtde: DropTargetDragEvent) {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY)
                    } else {
                        dtde.rejectDrag()
                    }
                }

                override fun dragOver(dtde: DropTargetDragEvent) {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY)
                    } else {
                        dtde.rejectDrag()
                    }
                }

                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        if (evt.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            evt.acceptDrop(DnDConstants.ACTION_COPY)
                            val transferable = evt.transferable
                            @Suppress("UNCHECKED_CAST")
                            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                            if (!files.isNullOrEmpty()) {
                                val paths = files.map { it.absolutePath }
                                container.chatViewModel.sendFiles(paths)
                                evt.dropComplete(true)
                                return
                            }
                        }
                        evt.rejectDrop()
                    } catch (_: Exception) {
                        evt.dropComplete(false)
                    }
                }
            }

            window.dropTarget = DropTarget(window, DnDConstants.ACTION_COPY, dropListener, true)
            window.contentPane.dropTarget = DropTarget(window.contentPane, DnDConstants.ACTION_COPY, dropListener, true)
            window.rootPane.dropTarget = DropTarget(window.rootPane, DnDConstants.ACTION_COPY, dropListener, true)
            true
        }

        App(
            container = container,
            onPickFiles = {
                val dialog = FileDialog(window as Frame, "Select Files to Send", FileDialog.LOAD).apply {
                    isMultipleMode = true
                    isVisible = true
                }
                val files = dialog.files
                if (files != null && files.isNotEmpty()) {
                    container.chatViewModel.sendFiles(files.map { it.absolutePath })
                }
            },
            onPickImages = {
                val dialog = FileDialog(window as Frame, "Select Images to Send", FileDialog.LOAD).apply {
                    isMultipleMode = true
                    setFilenameFilter { _, name ->
                        val lower = name.lowercase()
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")
                    }
                    isVisible = true
                }
                val files = dialog.files
                if (files != null && files.isNotEmpty()) {
                    container.chatViewModel.sendFiles(files.map { it.absolutePath })
                }
            },
            onPickDownloadDir = {
                val os = System.getProperty("os.name", "").lowercase()
                if (os.contains("mac")) {
                    System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    val dialog = FileDialog(window as Frame, "Choose Download Directory", FileDialog.LOAD).apply {
                        isVisible = true
                    }
                    System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null) {
                        val path = if (file != null) File(dir, file).absolutePath else File(dir).absolutePath
                        container.settingsViewModel.updateDownloadDirectory(path)
                    }
                } else {
                    val chooser = javax.swing.JFileChooser().apply {
                        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Choose Download Directory"
                        val current = container.settingsViewModel.settings.value.downloadDirectory.ifBlank {
                            PlatformInfo.getDefaultDownloadDirectory()
                        }
                        currentDirectory = File(current)
                    }
                    val result = chooser.showOpenDialog(window)
                    if (result == javax.swing.JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                        container.settingsViewModel.updateDownloadDirectory(chooser.selectedFile.absolutePath)
                    }
                }
            }
        )
    }
}
}
