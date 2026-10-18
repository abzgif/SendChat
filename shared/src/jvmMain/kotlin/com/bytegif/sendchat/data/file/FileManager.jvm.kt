package com.bytegif.sendchat.data.file

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Base64
import javax.imageio.ImageIO

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
            FileInputStream(filePath).use { stream ->
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
            val file = File(filePath)
            if (!file.exists()) return null
            val original = ImageIO.read(file) ?: return null

            val width = original.width
            val height = original.height
            val scale = maxDimension.toDouble() / maxOf(width, height).coerceAtLeast(1)

            val targetW = (width * scale).toInt().coerceAtLeast(1)
            val targetH = (height * scale).toInt().coerceAtLeast(1)

            val thumb = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
            val g = thumb.createGraphics()
            g.drawImage(original.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH), 0, 0, null)
            g.dispose()

            val baos = ByteArrayOutputStream()
            ImageIO.write(thumb, "jpg", baos)
            Base64.getEncoder().encodeToString(baos.toByteArray())
        }.getOrNull()
    }

    actual fun getFileSize(filePath: String): Long {
        return runCatching { File(filePath).length() }.getOrDefault(0L)
    }

    actual fun getFileName(filePath: String): String {
        return runCatching { File(filePath).name }.getOrDefault("file")
    }

    actual fun openInputStream(filePath: String): java.io.InputStream? {
        return runCatching { FileInputStream(File(filePath)) }.getOrNull()
    }

    actual fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    actual fun deleteFile(filePath: String): Boolean {
        return runCatching { File(filePath).delete() }.getOrDefault(false)
    }
}
