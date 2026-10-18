package com.bytegif.sendchat.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.bytegif.sendchat.data.db.AndroidAppContextHolder
import com.bytegif.sendchat.data.db.DriverFactory
import com.bytegif.sendchat.ui.AppContainer

class SendChatApplication : Application() {

    companion object {
        const val CHANNEL_ID = "sendchat_foreground_service"
        lateinit var instance: SendChatApplication
            private set
    }

    val appContainer: AppContainer by lazy {
        val driverFactory = DriverFactory(this)
        AppContainer(driverFactory)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AndroidAppContextHolder.appContext = this
        createNotificationChannel()
        cleanStaleCacheFiles()
    }

    fun cleanStaleCacheFiles() {
        runCatching {
            cacheDir?.listFiles()?.forEach { file ->
                runCatching { file.deleteRecursively() }
            }
            externalCacheDir?.listFiles()?.forEach { file ->
                runCatching { file.deleteRecursively() }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
