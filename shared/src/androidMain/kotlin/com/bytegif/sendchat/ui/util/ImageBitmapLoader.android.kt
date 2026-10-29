package com.bytegif.sendchat.ui.util

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.bytegif.sendchat.data.db.AndroidAppContextHolder
import java.io.File

actual object ImageBitmapLoader {
    actual fun loadFromPath(filePath: String, maxDimension: Int): ImageBitmap? {
        return runCatching {
            if (filePath.startsWith("content://")) {
                val context = AndroidAppContextHolder.appContext ?: return null
                val uri = Uri.parse(filePath)

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                if (options.outWidth <= 0 || options.outHeight <= 0) return null

                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return null
                bitmap.asImageBitmap()
            } else {
                val file = File(filePath)
                if (!file.exists() || !file.canRead()) return null

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)

                if (options.outWidth <= 0 || options.outHeight <= 0) return null

                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
                bitmap.asImageBitmap()
            }
        }.getOrNull()
    }

    actual fun loadFromBase64(base64: String): ImageBitmap? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap?.asImageBitmap()
        }.getOrNull()
    }
}
