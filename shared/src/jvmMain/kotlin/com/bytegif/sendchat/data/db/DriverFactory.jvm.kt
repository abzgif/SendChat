package com.bytegif.sendchat.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()
        val appDir = when {
            osName.contains("win") -> File(System.getenv("APPDATA") ?: userHome, "SendChat")
            osName.contains("mac") -> File(userHome, "Library/Application Support/SendChat")
            else -> File(System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config", "sendchat")
        }
        appDir.mkdirs()
        val dbFile = File(appDir, "sendchat.db")
        val isNew = !dbFile.exists()

        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            SendChatDatabase.Schema.create(driver)
        } else {
            runCatching {
                driver.execute(null, "ALTER TABLE paired_device_entity ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0;", 0)
            }
        }
        return driver
    }
}
