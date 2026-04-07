package com.trader.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trader.core.domain.model.MerchantStatus
import java.util.concurrent.TimeUnit

class AdminMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "admin_alerts"

        // استدعاء من أي مكان (Application, WorkManager, إلخ)
        // بدون الحاجة لإنشاء instance من الـ Service يدوياً
        fun checkExpiringMerchants(context: Context) {
            val appContext = context.applicationContext
            val db = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            val twoDays = TimeUnit.DAYS.toMillis(2)

            db.collection("merchants").get().addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val isPermanent = doc.getBoolean("isPermanent") ?: true
                    val status = doc.getString("status") ?: "ACTIVE"
                    val expiry = doc.getTimestamp("expiryDate")?.toDate()?.time
                    val name = doc.getString("name") ?: "بائع"

                    if (!isPermanent && expiry != null && status == MerchantStatus.ACTIVE.name) {
                        val remaining = expiry - now
                        if (remaining in 0..twoDays) {
                            val hours = TimeUnit.MILLISECONDS.toHours(remaining)
                            showNotification(
                                context = appContext,
                                title = "⚠️ تحذير انتهاء اشتراك",
                                body = "$name — ينتهي خلال $hours ساعة"
                            )
                        }
                    }
                }
            }
        }

        private fun showNotification(context: Context, title: String, body: String) {
            val deepLinkIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("admin://notifications"),
                context,
                AdminMainActivity::class.java
            )
            val pendingIntent = PendingIntent.getActivity(
                context, 0, deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: return
        showNotification(context = this, title = title, body = body)
    }

    override fun onNewToken(token: String) {
        // حفظ التوكن إن احتجت لإرسال إشعارات للأدمن
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "تنبيهات الإدارة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات انتهاء اشتراك البائعين"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
