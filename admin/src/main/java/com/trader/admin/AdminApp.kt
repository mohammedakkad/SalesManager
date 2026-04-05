package com.trader.admin

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.trader.admin.di.adminModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        startKoin {
            androidContext(this@AdminApp)
            modules(adminModule)
        }
    }
}
