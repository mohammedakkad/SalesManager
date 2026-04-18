package com.trader.salesmanager.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.trader.salesmanager.MainActivity
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

class BackgroundUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val nm by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val channelId = "update_channel"
    private val notifIdProgress = 3001
    private val notifIdReady = 3002 // معرّف مختلف لإشعار "جاهز"

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val versionName = inputData.getString(KEY_VERSION_NAME) ?: ""

        ensureChannel()
        showProgressNotification(0, versionName)

        var finalResult = Result.failure()
        AppUpdateDownloader.downloadApk(applicationContext, url).collect {
            state ->
            when (state) {
                is DownloadState.Progress -> {
                    showProgressNotification(state.percent, versionName)
                    setProgress(workDataOf(KEY_PROGRESS to state.percent))
                }
                is DownloadState.Success -> {
                    showReadyNotification(versionName)
                    // Save apk path for install trigger
                    applicationContext.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                    .edit().putString("apk_path", state.file.absolutePath).apply()
                    finalResult = Result.success()
                }
                is DownloadState.Error -> {
                    showErrorNotification(state.message)
                    finalResult = Result.failure()
                }
            }
        }
        return finalResult
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(
                channelId, "تحديثات التطبيق", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تحميل تحديثات التطبيق في الخلفية"
            })
        }
    }

    private fun showProgressNotification(percent: Int, version: String) {
        val notif = NotificationCompat.Builder(applicationContext, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("جاري تحميل التحديث $version")
        .setContentText("$percent% مكتمل")
        .setProgress(100, percent, percent == 0)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(notifIdProgress, notif)
        }
        catch (e: SecurityException) {
            nm.notify(notifIdProgress, notif)
        }
    }

    private fun showReadyNotification(version: String) {
        // ✅ إلغاء إشعار التقدم (ongoing) أولاً حتى لا يمنع ظهور إشعار "جاهز"
        nm.cancel(notifIdProgress)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("install_update", true)
        }
        val pi = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(applicationContext, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("✅ التحديث $version جاهز للتثبيت")
        .setContentText("اضغط لتثبيت التحديث الآن")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()
        // ✅ معرّف مستقل — لا علاقة له بالإشعار القديم
        try {
            NotificationManagerCompat.from(applicationContext).notify(notifIdReady, notif)
        }
        catch (e: SecurityException) {
            nm.notify(notifIdReady, notif)
        }
    }

    private fun showErrorNotification(msg: String) {
        nm.cancel(notifIdProgress)
        val notif = NotificationCompat.Builder(applicationContext, channelId)
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setContentTitle("فشل تحميل التحديث")
        .setContentText(msg)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(notifIdReady, notif)
        }
        catch (e: SecurityException) {
            nm.notify(notifIdReady, notif)
        }
    }

    companion object {
        const val KEY_URL = "download_url"
        const val KEY_VERSION_NAME = "version_name"
        const val KEY_PROGRESS = "progress"
        const val WORK_NAME = "background_update_download"

        fun schedule(context: Context, url: String, versionName: String): androidx.work.Operation {
            val request = OneTimeWorkRequestBuilder<BackgroundUpdateWorker>()
            .setInputData(workDataOf(KEY_URL to url, KEY_VERSION_NAME to versionName))
            .setConstraints(
                Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            )
            .build()
            return WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) =
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}