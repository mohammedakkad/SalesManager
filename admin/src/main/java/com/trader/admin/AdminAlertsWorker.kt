package com.trader.admin

import android.content.Context
import androidx.work.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AdminAlertsWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Task A: Check Expiring Merchants (Firestore)
            AdminMessagingService.checkExpiringMerchants(applicationContext)

            // Task B: Check Pending Subscription Requests (RTDB)
            checkPendingRequests()

            Result.success()
        } catch (e: Exception) {
            // Retry on network failures (exponential backoff handled by WorkManager)
            Result.retry()
        }
    }

    private suspend fun checkPendingRequests() {
        val rtdb = FirebaseDatabase.getInstance().reference
        val snapshot = rtdb.child("subscription_requests")
            .get()
            .await()

        val pendingCount = snapshot.children.count { 
            it.child("status").value == "PENDING" 
        }

        if (pendingCount > 0) {
            AdminMessagingService.showNotification(
                context = applicationContext,
                title = "طلبات اشتراك جديدة",
                body = "لديك $pendingCount طلب اشتراك قيد الانتظار"
            )
        }
    }

    companion object {
        private const val WORK_NAME = "AdminAlertsWork"

        fun startPeriodicChecks(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AdminAlertsWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}