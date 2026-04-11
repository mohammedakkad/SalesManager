package com.trader.salesmanager

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.util.ExpiryNotificationHelper
import com.trader.salesmanager.di.salesManagerModule
import com.trader.salesmanager.worker.UnpaidDebtWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.trader.core.worker.StatusCheckWorker

class SalesManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Refresh FCM token on app start
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                // Token will be saved via onNewToken or here
                android.util.Log.d("FCM", "Token: $token")
            }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        // Offline writes queued on disk → auto-sync on reconnect
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        // Create notification channel (required for Android 8+)
        ExpiryNotificationHelper.createChannel(this)
        startKoin {
            androidContext(this@SalesManagerApp)
            modules(salesManagerModule)
        }
        // Schedule unpaid debt reminders — every 8 hours
        UnpaidDebtWorker.schedule(this)
        
        StatusCheckWorker.schedule(this)
    }
}
