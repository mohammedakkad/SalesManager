package com.trader.salesmanager.di

import com.trader.salesmanager.data.remote.FirebaseActivationService
import org.koin.dsl.module

val appModule = module {
    single { FirebaseActivationService() }
}