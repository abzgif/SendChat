package com.bytegif.sendchat.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): SendChatDatabase {
    val driver = driverFactory.createDriver()
    return SendChatDatabase(driver)
}
