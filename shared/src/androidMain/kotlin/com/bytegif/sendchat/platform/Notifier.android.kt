package com.bytegif.sendchat.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bytegif.sendchat.data.db.AndroidAppContextHolder

actual object Notifier {
    private const val CHANNEL_ID = "sendchat_messages_notifications"
    private var isChannelCreated = false

    private fun ensureChannel(context: Context) {
        if (!isChannelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Messages & File Transfers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming messages and received files"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
            isChannelCreated = true
        }
    }

    actual fun showNotification(title: String, body: String, deviceId: String?, isMuted: Boolean) {
        val context = AndroidAppContextHolder.appContext ?: return
        ensureChannel(context)

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!deviceId.isNullOrBlank()) {
                putExtra("EXTRA_DEVICE_ID", deviceId)
                action = "OPEN_CHAT_$deviceId"
            }
        }
        val requestCode = deviceId?.hashCode() ?: 0
        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val smallIconRes = context.resources.getIdentifier("ic_notification", "drawable", context.packageName)
            .takeIf { it != 0 } ?: context.applicationInfo.icon

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setColor(0xFFFF5365.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSilent(isMuted)

        val notificationId = if (!deviceId.isNullOrBlank()) (deviceId.hashCode() % 100000) else (System.currentTimeMillis() % 100000).toInt()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, builder.build())
    }
}
