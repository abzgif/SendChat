package com.bytegif.sendchat.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import java.util.Base64

actual object ImageBitmapLoader {
    actual fun loadFromPath(filePath: String, maxDimension: Int): ImageBitmap? {
        return runCatching {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return null
            val bytes = file.readBytes()
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        }.getOrNull()
    }

    actual fun loadFromBase64(base64: String): ImageBitmap? {
        return runCatching {
            val bytes = Base64.getDecoder().decode(base64)
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        }.getOrNull()
    }
}
