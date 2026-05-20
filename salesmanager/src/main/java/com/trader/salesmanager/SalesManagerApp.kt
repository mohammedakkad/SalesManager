package com.trader.salesmanager

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.remote.RemoteConfigManager
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.sync.SyncCoordinator
import com.trader.core.util.ExpiryNotificationHelper
import com.trader.core.worker.StatusCheckWorker
import com.trader.salesmanager.di.salesManagerModule
import com.trader.salesmanager.worker.UnpaidDebtWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class SalesManagerApp : Application(), KoinComponent {
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

        // ✅ Fix 5: تشغيل SyncCoordinator — يراقب الشبكة ويُزامن البيانات المعلقة تلقائياً
        // يُشغَّل مرة واحدة فقط طوال عمر التطبيق (single() في Koin)
        val syncCoordinator: SyncCoordinator by inject()
        syncCoordinator.start()

        // ── Freemium: initialise Remote Config after Koin is ready ──
        // Uses SupervisorJob so a crash here doesn't kill the app
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val activationRepo: ActivationRepository by inject()
                val tier = activationRepo.getMerchantTier()
                RemoteConfigManager.initialize(tier)
            }
        }
    }
}
