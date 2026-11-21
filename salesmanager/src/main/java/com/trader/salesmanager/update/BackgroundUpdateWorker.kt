package com.trader.salesmanager.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.trader.salesmanager.BuildConfig
import com.trader.salesmanager.MainActivity
import java.util.concurrent.TimeUnit

class BackgroundUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val nm by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_URL)
        return if (downloadUrl.isNullOrBlank()) {
            doCheck()
        } else {
            doDownload(downloadUrl, inputData.getString(KEY_VERSION_NAME).orEmpty())
        }
    }

    private suspend fun doCheck(): Result {
        val info = AppUpdateChecker.check() ?: return Result.success()
        if (info.latestVersion <= BuildConfig.VERSION_CODE) return Result.success()

        ensureChannel()
        showUpdateAvailableNotification(info)
        return Result.success()
    }

    private suspend fun doDownload(url: String, versionName: String): Result {
        ensureChannel()
        showProgressNotification(0)

        val file = AppUpdateDownloader.downloadApk(applicationContext, url) { percent ->
            showProgressNotification(percent)
        }

        return if (file != null) {
            applicationContext.getSharedPreferences(AppUpdateViewModel.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(AppUpdateViewModel.KEY_APK_PATH, file.absolutePath)
                .apply()
            showReadyNotification()
            Result.success()
        } else {
            nm.cancel(NOTIF_ID_PROGRESS)
            Result.failure()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "تحديثات التطبيق",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "إشعارات التحديثات المتاحة للتطبيق"
                }
            )
        }
    }

    private fun showUpdateAvailableNotification(info: AppUpdateInfo) {
        val openSettingsIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_SETTINGS)
        }
        val openSettingsPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val downloadIntent = Intent(applicationContext, UpdateDownloadReceiver::class.java).apply {
            putExtra(KEY_URL, info.downloadUrl)
            putExtra(KEY_VERSION_NAME, info.versionName)
        }
        val downloadPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("يوجد تحديث جديد — الإصدار ${info.versionName}")
            .setContentText("اضغط لفتح الإعدادات أو اختر تحديث الآن")
            .setContentIntent(openSettingsPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.stat_sys_download,
                "تحديث الآن",
                downloadPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifySafely(NOTIF_ID_AVAILABLE, notification)
    }

    private fun showProgressNotification(percent: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("تحديث التطبيق")
            .setContentText("جاري التحميل... $percent%")
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notifySafely(NOTIF_ID_PROGRESS, notification)
    }

    private fun showReadyNotification() {
        nm.cancel(NOTIF_ID_PROGRESS)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_INSTALL_UPDATE, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✅ التحديث جاهز للتثبيت — اضغط للتثبيت")
            .setContentText("اضغط لتثبيت التحديث الآن")
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notifySafely(NOTIF_ID_READY, notification)
    }

    private fun notifySafely(id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (_: SecurityException) {
            nm.notify(id, notification)
        }
    }

    companion object {
        const val KEY_URL = "download_url"
        const val KEY_VERSION_NAME = "version_name"
        const val WORKER_TAG = "update_check_worker"
        private const val UNIQUE_PERIODIC_WORK = "update_check_periodic"
        private const val UNIQUE_DOWNLOAD_WORK = "update_download"
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIF_ID_AVAILABLE = 3000
        private const val NOTIF_ID_PROGRESS = 3001
        private const val NOTIF_ID_READY = 3002
        private const val CHECK_INTERVAL_HOURS = 6L

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackgroundUpdateWorker>(
                CHECK_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .addTag(WORKER_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleDownload(context: Context, url: String, versionName: String) {
            val request = OneTimeWorkRequestBuilder<BackgroundUpdateWorker>()
                .setInputData(
                    workDataOf(
                        KEY_URL to url,
                        KEY_VERSION_NAME to versionName
                    )
                )
                .addTag(WORKER_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_DOWNLOAD_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
