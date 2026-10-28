package com.bytegif.sendchat.viewmodel

import com.bytegif.sendchat.data.db.AndroidAppContextHolder

actual object AppVersion {
    actual val current: String
        get() {
            val context = AndroidAppContextHolder.appContext ?: return DEFAULT_APP_VERSION
            @Suppress("DEPRECATION")
            return runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    ?.removePrefix("v")
                    ?.takeIf(String::isNotBlank)
            }.getOrNull() ?: DEFAULT_APP_VERSION
        }
}
