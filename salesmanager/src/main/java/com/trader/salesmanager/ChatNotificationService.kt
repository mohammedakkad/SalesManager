package com.trader.salesmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM push notifications for chat messages.
 *
 * When admin sends a message from the admin panel, it triggers an FCM notification
 * to this service. The notification is shown even when the app is in background/killed.
 *
 * FCM setup on the admin side: when admin sends a message, it should also trigger
 * a Cloud Function (or the admin app calls Firebase Admin SDK) to send FCM to the
 * merchant's device token.
 */
class ChatNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "رسالة جديدة من الإدارة"
        val body  = message.notification?.body  ?: message.data["body"]  ?: "لديك رسالة جديدة"

        showChatNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: save token to Firestore under merchants/{merchantCode}/fcmToken
        // so the admin app / Cloud Function can send targeted notifications
    }

    private fun showChatNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        // Create channel (required Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "رسائل الدعم الفني",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات رسائل الدعم الفني"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Tap → open MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(CHAT_NOTIF_ID, notif)
    }

    companion object {
        const val CHAT_CHANNEL_ID = "chat_channel"
        const val CHAT_NOTIF_ID   = 2001
    }
}
