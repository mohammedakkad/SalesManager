package com.trader.salesmanager

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.dao.ProductDao
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.sync.SyncCoordinator
import com.trader.core.util.ExpiryNotificationHelper
import com.trader.core.worker.StatusCheckWorker
import com.trader.salesmanager.di.salesManagerModule
import com.trader.salesmanager.update.BackgroundUpdateWorker
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
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                android.util.Log.d("FCM", "Token: $token")
            }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        ExpiryNotificationHelper.createChannel(this)
        startKoin {
            androidContext(this@SalesManagerApp)
            modules(salesManagerModule)
        }
        UnpaidDebtWorker.schedule(this)
        StatusCheckWorker.schedule(this)
        BackgroundUpdateWorker.schedulePeriodic(this)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val syncCoordinator: SyncCoordinator by inject()
                syncCoordinator.start()
            }
        }
    }
}
