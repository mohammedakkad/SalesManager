package com.trader.salesmanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trader.salesmanager.MainActivity

object NotificationService {

    private const val CHANNEL_EXPIRY = "expiry_channel"
    private const val CHANNEL_GENERAL = "general_channel"

    private const val BLOCKED_NOTIF_ID = 9001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_EXPIRY, "انتهاء الاشتراك", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "إشعارات اقتراب انتهاء مدة الاشتراك"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_GENERAL, "إشعارات عامة", NotificationManager.IMPORTANCE_DEFAULT)
            )
            nm.createNotificationChannel(
                NotificationChannel("debt_reminder_channel", "تذكير الديون", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "تذكير بالزبائن الذين لم يسددوا"
                }
            )
        }
    }

    fun showBlockedNotification(context: Context, reason: String) {
        val message = when (reason) {
            "DISABLED" -> "تم تعطيل حسابك من الإدارة"
            "EXPIRED" -> "انتهت مدة اشتراكك"
            "DELETED" -> "تم حذف حسابك"
            else -> "حسابك غير نشط"
        }

        // Changed CHANNEL_ID to CHANNEL_EXPIRY (or CHANNEL_GENERAL)
        val notif = NotificationCompat.Builder(context, CHANNEL_EXPIRY)
        // Ensure you have the correct import for R or use a system icon for now
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("مدير المبيعات — تنبيه")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

        try {
            NotificationManagerCompat.from(context).notify(BLOCKED_NOTIF_ID, notif)
        } catch (e: SecurityException) {
            /* permission not granted */
        }
    }


    fun showExpiryWarning(context: Context, daysLeft: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (daysLeft <= 0) "⚠️ انتهى اشتراكك!" else "⚠️ اشتراكك سينتهي قريباً"
        val body = if (daysLeft <= 0) "تواصل مع الإدارة لتجديد اشتراكك"
        else "متبقي $daysLeft يوم فقط — تواصل مع الإدارة للتجديد"

        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRY)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            /* permission not granted */
        }
    }

    fun showStatusChanged(context: Context, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("تغيير حالة الحساب")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
        try {
            NotificationManagerCompat.from(context).notify(1002, notification)
        } catch (e: SecurityException) {
            /* permission not granted */
        }
    }
}