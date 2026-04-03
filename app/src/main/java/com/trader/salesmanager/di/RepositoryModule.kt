package com.trader.salesmanager.di

import com.trader.salesmanager.data.repository.*
import com.trader.salesmanager.domain.repository.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<CustomerRepository> { CustomerRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get()) }
    single<PaymentMethodRepository> { PaymentMethodRepositoryImpl(get()) }
    single<ActivationRepository> { ActivationRepositoryImpl(androidContext(), get()) }
}