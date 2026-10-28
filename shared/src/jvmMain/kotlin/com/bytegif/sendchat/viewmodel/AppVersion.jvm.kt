package com.bytegif.sendchat.viewmodel

actual object AppVersion {
    actual val current: String
        get() = AppVersion::class.java.`package`?.implementationVersion
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_APP_VERSION
}
