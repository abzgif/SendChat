package com.bytegif.sendchat.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.KeyboardFocusManager
import java.awt.MouseInfo
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

class FileTransferable(private val files: List<File>) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor == DataFlavor.javaFileListFlavor
    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor == DataFlavor.javaFileListFlavor) return files
        throw UnsupportedFlavorException(flavor)
    }
}

actual fun Modifier.dragAndDropFileExport(filePath: String): Modifier {
    if (filePath.isBlank()) return this
    val file = File(filePath)
    if (!file.exists()) return this

    return this.pointerInput(filePath) {
        detectDragGestures(
            onDragStart = {
                SwingUtilities.invokeLater {
                    try {
                        val activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
                        val comp: JComponent? = (activeWindow as? JFrame)?.contentPane as? JComponent
                            ?: (activeWindow as? java.awt.Container)?.components?.firstOrNull { it is JComponent } as? JComponent
                            ?: (activeWindow as? JComponent)

                        if (comp != null) {
                            val mousePos = MouseInfo.getPointerInfo()?.location
                            val compLocation = runCatching { comp.locationOnScreen }.getOrNull() ?: Point(0, 0)
                            val relX = if (mousePos != null) mousePos.x - compLocation.x else 0
                            val relY = if (mousePos != null) mousePos.y - compLocation.y else 0

                            comp.transferHandler = object : TransferHandler() {
                                override fun getSourceActions(c: JComponent?): Int = COPY
                                override fun createTransferable(c: JComponent?): Transferable {
                                    return FileTransferable(listOf(file))
                                }
                            }

                            val mouseEvent = MouseEvent(
                                comp,
                                MouseEvent.MOUSE_DRAGGED,
                                System.currentTimeMillis(),
                                InputEvent.BUTTON1_DOWN_MASK,
                                relX,
                                relY,
                                1,
                                false,
                                MouseEvent.BUTTON1
                            )
                            comp.transferHandler.exportAsDrag(comp, mouseEvent, TransferHandler.COPY)
                        }
                    } catch (_: Exception) {
                        // Ignore drag initiation errors
                    }
                }
            },
            onDrag = { _, _ -> },
            onDragEnd = {},
            onDragCancel = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.onExternalFileDrop(onFilesDropped: (List<String>) -> Unit): Modifier {
    val target = remember(onFilesDropped) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    if (!files.isNullOrEmpty()) {
                        val validPaths = files.map { it.absolutePath }.filter { it.isNotBlank() && File(it).exists() }
                        if (validPaths.isNotEmpty()) {
                            onFilesDropped(validPaths)
                            return true
                        }
                    }
                }
                return false
            }
        }
    }

    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        },
        target = target
    )
}
