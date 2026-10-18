package com.bytegif.sendchat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun Modifier.dragAndDropFileExport(filePath: String): Modifier = this

@Composable
actual fun Modifier.onExternalFileDrop(onFilesDropped: (List<String>) -> Unit): Modifier = this
