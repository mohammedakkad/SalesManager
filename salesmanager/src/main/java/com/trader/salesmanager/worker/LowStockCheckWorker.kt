package com.trader.salesmanager.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.trader.core.domain.repository.LowStockAlertRepository
import com.trader.core.domain.repository.LowStockSettingsRepository
import com.trader.salesmanager.MainActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LowStockCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val alertRepository: LowStockAlertRepository by inject()
    private val settingsRepository: LowStockSettingsRepository by inject()

    override suspend fun doWork(): Result {
        val settings = try {
            settingsRepository.getSettings()
        } catch (_: Exception) {
            return Result.retry()
        }
        if (!settings.enabled) return Result.success()

        val now = System.currentTimeMillis()
        val candidates = try {
            alertRepository.getNotificationCandidates(now, COOLDOWN_MILLIS)
        } catch (_: Exception) {
            return Result.retry()
        }
        if (candidates.isEmpty()) return Result.success()

        ensureChannel()
        val productCount = candidates.map { it.productId }.distinct().size
        if (!showNotification(productCount)) return Result.success()

        runCatching {
            alertRepository.markNotified(candidates, now)
        }
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات نقص المخزون",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "تنبيه بالأصناف التي أوشكت على النفاد أو نفدت"
                }
            )
        }
    }

    private fun showNotification(productCount: Int): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_LOW_STOCK)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = "لديك $productCount منتجات أوشكت على النفاد"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("تنبيه نقص المخزون")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val WORK_NAME = "low_stock_daily_check"
        const val COOLDOWN_DAYS = 3L
        private const val CHANNEL_ID = "low_stock_alert_channel"
        private const val NOTIFICATION_ID = 5000
        private val COOLDOWN_MILLIS = TimeUnit.DAYS.toMillis(COOLDOWN_DAYS)

        fun schedule(context: Context, preferredHour: Int, replaceExisting: Boolean = false) {
            val request = PeriodicWorkRequestBuilder<LowStockCheckWorker>(
                24,
                TimeUnit.HOURS
            )
                .setInitialDelay(initialDelayMillis(preferredHour), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                if (replaceExisting) {
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                } else {
                    ExistingPeriodicWorkPolicy.KEEP
                },
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        internal fun initialDelayMillis(
            preferredHour: Int,
            nowMillis: Long = System.currentTimeMillis(),
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val nextRun = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, preferredHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            return nextRun.timeInMillis - nowMillis
        }
    }
}
