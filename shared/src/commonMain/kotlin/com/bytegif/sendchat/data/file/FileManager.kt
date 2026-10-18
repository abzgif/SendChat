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
