package com.trader.salesmanager.di

import com.trader.salesmanager.data.remote.FirebaseSyncService
import com.trader.salesmanager.util.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { FirebaseSyncService() }
    single { NetworkMonitor(androidContext()) }
}
