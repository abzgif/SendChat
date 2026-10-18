package com.bytegif.sendchat.platform

import com.bytegif.sendchat.domain.model.DevicePlatform

expect object PlatformInfo {
    val platform: DevicePlatform
    fun getDefaultDeviceName(): String
    fun getDefaultDownloadDirectory(): String
    fun generateDeviceId(): String
    fun openFileInDefaultApp(filePath: String)
    fun openDirectoryInFileManager(directoryPath: String)
    fun getLocalIpAddresses(): List<String>
    fun scanMediaFile(filePath: String)
}
