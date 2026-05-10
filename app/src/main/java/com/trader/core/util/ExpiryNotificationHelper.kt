package com.trader.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object ExpiryNotificationHelper {
    const val CHANNEL_ID   = "expiry_channel"
    const val CHANNEL_NAME = "تنبيهات انتهاء الصلاحية"
    const val NOTIF_ID     = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "تنبيه عند اقتراب انتهاء صلاحية الاشتراك" }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun showExpiryWarning(context: Context, daysLeft: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val title = "⚠️ اشتراكك على وشك الانتهاء"
        val body  = "متبقي $daysLeft ${if (daysLeft == 1) "يوم" else "أيام"} فقط على انتهاء اشتراكك. تواصل مع الإدارة للتجديد."
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_ID, notif)
    }

    fun showExpiredNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val body = "انتهت صلاحية اشتراكك. يرجى التواصل مع الإدارة لتجديد الاشتراك."
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("❌ انتهى اشتراكك")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_ID + 1, notif)
    }
}
