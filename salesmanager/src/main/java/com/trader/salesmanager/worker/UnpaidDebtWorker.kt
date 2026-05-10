package com.trader.salesmanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.trader.core.data.local.db.AppDatabase
import com.trader.salesmanager.MainActivity
import java.util.concurrent.TimeUnit

/**
 * Periodic worker — runs every 8 hours.
 *
 * Checks for unpaid transactions that are at least [HOURS_THRESHOLD] old.
 * Groups them by customer and shows one notification per customer.
 *
 * Firebase DB structure used:
 *   Reads directly from local Room DB — no network needed.
 *   This way it works offline too.
 */
class UnpaidDebtWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val channelId = "debt_reminder_channel"
    private val db by lazy { AppDatabase.build(applicationContext) }

    override suspend fun doWork(): Result {
        val thresholdMs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HOURS_THRESHOLD)
        val unpaid = db.transactionDao().getUnpaidOlderThan(thresholdMs)

        if (unpaid.isEmpty()) return Result.success()

        ensureChannel()

        // Group by customerId — one notification per customer
        val byCustomer = unpaid.groupBy { it.customerId }

        byCustomer.entries.forEachIndexed { index, (customerId, txList) ->
            val customerName = db.customerDao().getCustomerById(customerId)?.name ?: "زبون"
            val totalDebt    = txList.sumOf { it.amount }
            val txCount      = txList.size

            showDebtNotification(
                notifId      = DEBT_NOTIF_BASE_ID + index,
                customerId   = customerId,
                customerName = customerName,
                totalDebt    = totalDebt,
                txCount      = txCount
            )
        }

        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "تذكير الديون",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "تذكير بالعملاء الذين لم يسددوا"
                }
            )
        }
    }

    private fun showDebtNotification(
        notifId: Int,
        customerId: Long,
        customerName: String,
        totalDebt: Double,
        txCount: Int
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "customer_details")
            putExtra("customer_id", customerId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (txCount == 1)
            "لم يسدد فاتورة بقيمة ₪${String.format("%,.0f", totalDebt)} حتى الآن"
        else
            "لديه $txCount فواتير غير مسددة بإجمالي ₪${String.format("%,.0f", totalDebt)}"

        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("💰 تذكير: $customerName")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(notifId, notif)
        } catch (e: SecurityException) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            nm.notify(notifId, notif)
        }
    }

    companion object {
        const val HOURS_THRESHOLD     = 8L
        const val WORK_NAME           = "unpaid_debt_reminder"
        const val DEBT_NOTIF_BASE_ID  = 4000

        /**
         * Schedule the worker to run every [HOURS_THRESHOLD] hours.
         * Call from SalesManagerApp.onCreate() after activation.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UnpaidDebtWorker>(
                repeatInterval         = HOURS_THRESHOLD,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
            .setInitialDelay(HOURS_THRESHOLD, TimeUnit.HOURS)  // first run after 8h
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,   // don't reset if already scheduled
                    request
                )
        }

        fun cancel(context: Context) =
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
