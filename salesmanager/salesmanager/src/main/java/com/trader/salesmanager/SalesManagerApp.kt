package com.trader.salesmanager

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.trader.salesmanager.di.salesManagerModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SalesManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        startKoin {
            androidContext(this@SalesManagerApp)
            modules(salesManagerModule)
        }
    }
}
