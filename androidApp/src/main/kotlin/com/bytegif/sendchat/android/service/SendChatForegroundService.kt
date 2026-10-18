package com.bytegif.sendchat.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bytegif.sendchat.android.MainActivity
import com.bytegif.sendchat.android.R
import com.bytegif.sendchat.android.SendChatApplication

class SendChatForegroundService : Service {
    constructor() : super()

    companion object {
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure { e ->
            Log.w("SendChat", "Failed to start foreground notification", e)
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallIconRes = resources.getIdentifier("ic_notification", "drawable", packageName)
            .takeIf { it != 0 } ?: applicationInfo.icon

        val builder = NotificationCompat.Builder(this, SendChatApplication.CHANNEL_ID)
            .setContentTitle("SendChat is active")
            .setContentText("Ready to share files and text on local network")
            .setSmallIcon(smallIconRes)
            .setColor(0xFFFF5365.toInt())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }
}
