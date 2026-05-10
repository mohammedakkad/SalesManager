package com.trader.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.*
import com.trader.core.data.local.appDataStore
import com.trader.core.data.remote.FirebaseSyncService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class StatusCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx       = applicationContext
        val dataStore = ctx.appDataStore

        val isActivated = dataStore.data.map { prefs ->
            prefs[booleanPreferencesKey("is_activated")] ?: false
        }.first()

        val code = dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("merchant_code")] ?: ""
        }.first()

        if (!isActivated || code.isEmpty()) return Result.success()

        val status = FirebaseSyncService().getCodeStatus(code)
            ?: return Result.retry() // offline — retry later

        when (status) {
            "DISABLED", "EXPIRED", "DELETED" -> {
                // Clear local activation
                dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("is_activated")] = false
                    prefs[stringPreferencesKey("merchant_code")]  = ""
                }
                // Show notification — defined locally to avoid cross-module dep
                showBlockedNotification(ctx, status)
            }
        }
        return Result.success()
    }

    // ── Notification — defined here to avoid importing from :salesmanager ──
    private fun showBlockedNotification(context: Context, reason: String) {
        val channelId = "status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(channelId, "حالة الحساب", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val title = when (reason) {
            "DISABLED" -> "⚠️ تم تعطيل حسابك"
            "EXPIRED"  -> "⚠️ انتهت مدة اشتراكك"
            "DELETED"  -> "⚠️ تم حذف حسابك"
            else       -> "⚠️ مشكلة في الحساب"
        }
        val body = when (reason) {
            "DISABLED" -> "تواصل مع الإدارة لإعادة التفعيل"
            "EXPIRED"  -> "تواصل مع الإدارة لتجديد الاشتراك"
            "DELETED"  -> "تواصل مع الإدارة للحصول على كود جديد"
            else       -> "تواصل مع الإدارة"
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2001, notification)
        } catch (e: SecurityException) { /* permission not granted */ }
    }

    companion object {
        private const val WORK_NAME = "merchant_status_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatusCheckWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
