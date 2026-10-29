package com.bytegif.sendchat.data.file

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.bytegif.sendchat.data.db.AndroidAppContextHolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

actual object FileManager {
    actual fun saveFile(directory: String, fileName: String, bytes: ByteArray): String {
        val targetPath = getUniqueFilePath(directory, fileName)
        val file = File(targetPath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun getUniqueFilePath(directory: String, fileName: String): String {
        val dir = File(directory)
        dir.mkdirs()

        val name = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
        val ext = if (fileName.contains(".")) "." + fileName.substringAfterLast(".") else ""

        var file = File(dir, fileName)
        var counter = 1
        while (file.exists()) {
            file = File(dir, "$name ($counter)$ext")
            counter++
        }
        return file.absolutePath
    }

    actual fun calculateFileSha256(filePath: String): String {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            openInputStream(filePath)?.use { stream ->
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }

    actual fun generateImageThumbnailBase64(filePath: String, maxDimension: Int): String? {
        return runCatching {
            val stream = openInputStream(filePath) ?: return null
            val bytes = stream.readBytes()

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension * 2 || options.outHeight / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            val width = original.width
            val height = original.height
            val scale = maxDimension.toFloat() / maxOf(width, height).coerceAtLeast(1)

            val targetW = (width * scale).toInt().coerceAtLeast(1)
            val targetH = (height * scale).toInt().coerceAtLeast(1)

            val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    actual fun getFileSize(filePath: String): Long {
        if (filePath.startsWith("content://")) {
            val context = AndroidAppContextHolder.appContext ?: return 0L
            val uri = Uri.parse(filePath)
            return runCatching {
                var size = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
                if (size <= 0L) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        size = pfd.statSize
                    }
                }
                size
            }.getOrDefault(0L)
        }
        return runCatching { File(filePath).length() }.getOrDefault(0L)
    }

    actual fun getFileName(filePath: String): String {
        if (filePath.startsWith("content://")) {
            val context = AndroidAppContextHolder.appContext ?: return "file"
            val uri = Uri.parse(filePath)
            return runCatching {
                var name: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        name = cursor.getString(nameIndex)
                    }
                }
                name ?: uri.lastPathSegment ?: "file"
            }.getOrDefault("file")
        }
        return runCatching { File(filePath).name }.getOrDefault("file")
    }

    actual fun openInputStream(filePath: String): InputStream? {
        if (filePath.startsWith("content://")) {
            val context = AndroidAppContextHolder.appContext ?: return null
            return runCatching {
                context.contentResolver.openInputStream(Uri.parse(filePath))
            }.getOrNull()
        }
        return runCatching { FileInputStream(File(filePath)) }.getOrNull()
    }

    actual fun fileExists(filePath: String): Boolean {
        if (filePath.startsWith("content://")) {
            val context = AndroidAppContextHolder.appContext ?: return false
            val uri = Uri.parse(filePath)
            return runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { true }
                    ?: (context.contentResolver.openInputStream(uri)?.use { true } == true)
            }.getOrDefault(false)
        }
        return File(filePath).exists()
    }

    actual fun deleteFile(filePath: String): Boolean {
        if (filePath.startsWith("content://")) {
            return false
        }
        return runCatching { File(filePath).delete() }.getOrDefault(false)
    }
}
