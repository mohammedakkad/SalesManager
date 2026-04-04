package com.trader.salesmanager

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.trader.salesmanager.di.appModule
import com.trader.salesmanager.di.databaseModule
import com.trader.salesmanager.di.repositoryModule
import com.trader.salesmanager.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SalesManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Must be called BEFORE any other Firebase Database usage.
        // Queues all offline writes to disk and syncs automatically
        // when connectivity is restored — fixes offline sync issue.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        startKoin {
            androidContext(this@SalesManagerApp)
            modules(appModule, databaseModule, repositoryModule, viewModelModule)
        }
    }
}
