package com.bytegif.sendchat.data.file

expect object FileManager {
    fun saveFile(directory: String, fileName: String, bytes: ByteArray): String
    fun getUniqueFilePath(directory: String, fileName: String): String
    fun calculateFileSha256(filePath: String): String
    fun generateImageThumbnailBase64(filePath: String, maxDimension: Int = 256): String?
    fun getFileSize(filePath: String): Long
    fun getFileName(filePath: String): String
    fun openInputStream(filePath: String): java.io.InputStream?
    fun fileExists(filePath: String): Boolean
    fun deleteFile(filePath: String): Boolean
}

fun isImageFile(fileName: String): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
}

fun getMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "tar", "gz" -> "application/gzip"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "txt" -> "text/plain"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }
}
