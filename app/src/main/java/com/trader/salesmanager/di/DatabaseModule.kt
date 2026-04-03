package com.trader.salesmanager.di

import com.trader.salesmanager.data.local.db.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.buildDatabase(androidContext()) }
    single { get<AppDatabase>().customerDao() }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().paymentMethodDao() }
}