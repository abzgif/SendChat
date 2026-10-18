package com.bytegif.sendchat.ui.util

import androidx.compose.ui.graphics.ImageBitmap

expect object ImageBitmapLoader {
    fun loadFromPath(filePath: String, maxDimension: Int = 1024): ImageBitmap?
    fun loadFromBase64(base64: String): ImageBitmap?
}
