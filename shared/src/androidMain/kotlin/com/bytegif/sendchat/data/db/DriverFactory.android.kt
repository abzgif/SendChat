package com.bytegif.sendchat.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object AndroidAppContextHolder {
    var appContext: Context? = null
}

actual class DriverFactory(private val context: Context? = null) {
    actual fun createDriver(): SqlDriver {
        val targetContext = context ?: AndroidAppContextHolder.appContext
            ?: throw IllegalStateException("Android Context must be initialized in AndroidAppContextHolder or passed to DriverFactory")
        val driver = AndroidSqliteDriver(SendChatDatabase.Schema, targetContext, "sendchat.db")
        runCatching {
            driver.execute(null, "ALTER TABLE paired_device_entity ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0;", 0)
        }
        return driver
    }
}
