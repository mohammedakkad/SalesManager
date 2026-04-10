package com.trader.salesmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles incoming FCM push notifications for chat messages.
 *
 * Flow:
 * 1. Admin sends message → Firestore message added + notifications/{merchantId} written
 * 2. Cloud Function OR AdminMessagingService reads notifications doc → sends FCM
 * 3. This service receives FCM → shows notification with text + timestamp
 *
 * Also saves FCM token to Firestore so admin can send targeted notifications.
 */
class ChatNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title  = message.notification?.title ?: message.data["title"] ?: "رسالة جديدة من الإدارة"
        val body   = message.notification?.body  ?: message.data["body"]  ?: ""
        val sentAt = message.data["sentAt"] ?: ""

        // Format: body + timestamp on new line
        val formattedBody = if (sentAt.isNotEmpty()) "$body\n🕐 $sentAt" else body
        showChatNotification(title, formattedBody)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveFcmToken(token)
    }

    private fun saveFcmToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val code = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("merchant_code", "") ?: ""
                if (code.isEmpty()) {
                    // Try DataStore
                    return@launch
                }
                FirebaseFirestore.getInstance()
                    .collection("merchants")
                    .whereEqualTo("activationCode", code)
                    .get()
                    .addOnSuccessListener { docs ->
                        docs.documents.firstOrNull()?.reference
                            ?.update("fcmToken", token)
                    }
            } catch (_: Exception) {}
        }
    }

    private fun showChatNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHAT_CHANNEL_ID, "رسائل الدعم الفني", NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        description = "رسائل من إدارة التطبيق"
                        enableVibration(true)
                        enableLights(true)
                    }
            )
        }

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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        manager.notify(CHAT_NOTIF_ID, notif)
    }

    companion object {
        const val CHAT_CHANNEL_ID = "chat_channel"
        const val CHAT_NOTIF_ID   = 2001

        /**
         * Call this from SalesManagerApp after activation to save the FCM token.
         * Must be called once the merchant code is known.
         */
        fun refreshAndSaveToken(context: android.content.Context, merchantCode: String) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    FirebaseFirestore.getInstance()
                        .collection("merchants")
                        .whereEqualTo("activationCode", merchantCode)
                        .get()
                        .addOnSuccessListener { docs ->
                            docs.documents.firstOrNull()?.reference?.update("fcmToken", token)
                        }
                }
        }
    }
}
