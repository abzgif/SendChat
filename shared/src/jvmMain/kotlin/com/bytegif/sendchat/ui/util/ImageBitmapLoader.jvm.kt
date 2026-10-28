package com.bytegif.sendchat.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.ceil

actual object ImageBitmapLoader {
    actual fun loadFromPath(filePath: String, maxDimension: Int): ImageBitmap? {
        return runCatching {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return null

            loadScaledWithImageIo(file, maxDimension.coerceAtLeast(1))?.let { return it }

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

    private fun loadScaledWithImageIo(file: File, maxDimension: Int): ImageBitmap? {
        val stream = ImageIO.createImageInputStream(file) ?: return null
        return stream.use { imageStream ->
            val reader = ImageIO.getImageReaders(imageStream).asSequence().firstOrNull() ?: return null
            try {
                reader.input = imageStream
                val largestSide = maxOf(reader.getWidth(0), reader.getHeight(0))
                val sampleSize = ceil(largestSide / maxDimension.toDouble()).toInt().coerceAtLeast(1)
                val readParameters = reader.defaultReadParam
                if (sampleSize > 1) {
                    readParameters.setSourceSubsampling(sampleSize, sampleSize, 0, 0)
                }
                reader.read(0, readParameters)?.toComposeImageBitmap()
            } finally {
                reader.dispose()
            }
        }
    }
}
