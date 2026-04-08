package com.trader.core.worker

import android.content.Context
import androidx.work.*
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.data.repository.ActivationRepositoryImpl
import com.trader.salesmanager.service.NotificationService
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import com.trader.core.data.local.appDataStore

class StatusCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        // قراءة الكود المحفوظ من DataStore مباشرة
        val prefs = context.getSharedPreferences("status_worker_prefs", Context.MODE_PRIVATE)
        // نقرأ من DataStore بنفس طريقة ActivationRepositoryImpl
        val dataStore = context.appDataStore // نفس الـ extension property
        val isActivated = dataStore.data.map {
            it[booleanPreferencesKey("is_activated")] ?: false
        }.first()
        val code = dataStore.data.map {
            it[stringPreferencesKey("merchant_code")] ?: ""
        }.first()

        if (!isActivated || code.isEmpty()) return Result.success()

        val firebase = FirebaseSyncService()
        val status = firebase.getCodeStatus(code) ?: return Result.retry() // offline

        when (status) {
            "DISABLED", "EXPIRED", "DELETED" -> {
                // مسح التفعيل المحلي
                dataStore.edit {
                    it[booleanPreferencesKey("is_activated")] = false
                    it[stringPreferencesKey("merchant_code")] = ""
                }
                // إرسال إشعار للمستخدم
                NotificationService.showBlockedNotification(context, status)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "merchant_status_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatusCheckWorker>(
                repeatInterval = 30, repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            )
            .build()

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