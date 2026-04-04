package com.trader.salesmanager.di

import com.trader.salesmanager.data.remote.FirebaseSyncService
import org.koin.dsl.module

val appModule = module {
    single { FirebaseSyncService() }
}