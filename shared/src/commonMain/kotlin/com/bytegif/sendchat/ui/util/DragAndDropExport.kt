package com.bytegif.sendchat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun Modifier.dragAndDropFileExport(filePath: String): Modifier

@Composable
expect fun Modifier.onExternalFileDrop(onFilesDropped: (List<String>) -> Unit): Modifier
