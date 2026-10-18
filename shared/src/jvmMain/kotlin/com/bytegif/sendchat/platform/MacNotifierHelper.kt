package com.bytegif.sendchat.platform

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object MacNotifierHelper {
    private var helperExecutable: File? = null
    private var isRegistered = false

    init {
        runCatching {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            if (os.contains("mac")) {
                prepareHelper()
            }
        }
    }

    @Synchronized
    private fun prepareHelper() {
        val userHome = System.getProperty("user.home") ?: "/tmp"
        val targetDir = File(userHome, ".sendchat/bin")
        targetDir.mkdirs()

        val appDir = File(targetDir, "SendChatAlert.app")
        val alertExe = File(appDir, "Contents/MacOS/sendchat_alert")

        if (!alertExe.exists() || !alertExe.canExecute()) {
            extractZipResource("/macos/SendChatAlert.zip", targetDir)
            alertExe.setExecutable(true, false)
        }

        if (appDir.exists() && !isRegistered) {
            runCatching {
                val lsregister = File("/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister")
                if (lsregister.exists()) {
                    ProcessBuilder(lsregister.absolutePath, "-f", appDir.absolutePath).start().waitFor()
                }
                isRegistered = true
            }
        }

        if (alertExe.exists() && alertExe.canExecute()) {
            helperExecutable = alertExe
        }
    }

    private fun extractZipResource(resourcePath: String, destDir: File) {
        val stream = object {}.javaClass.getResourceAsStream(resourcePath)
            ?: Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath.removePrefix("/"))
            ?: return

        ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    if (newFile.name == "sendchat_alert") {
                        newFile.setExecutable(true, false)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun show(title: String, body: String, isMuted: Boolean = false): Boolean {
        val exe = helperExecutable ?: run {
            prepareHelper()
            helperExecutable
        }

        if (exe != null && exe.exists() && exe.canExecute()) {
            val cleanTitle = if (title.startsWith("SendChat")) title else "SendChat: $title"
            val args = if (isMuted) {
                listOf(exe.absolutePath, cleanTitle, body, "--silent")
            } else {
                listOf(exe.absolutePath, cleanTitle, body)
            }
            val success = runCatching {
                ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start()
                true
            }.getOrDefault(false)

            if (success) return true
        }

        return false
    }
}
