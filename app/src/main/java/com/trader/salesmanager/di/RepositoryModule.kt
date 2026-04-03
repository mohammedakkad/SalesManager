package com.trader.salesmanager.di

import com.trader.salesmanager.data.repository.ActivationRepositoryImpl
import com.trader.salesmanager.data.repository.CustomerRepositoryImpl
import com.trader.salesmanager.data.repository.PaymentMethodRepositoryImpl
import com.trader.salesmanager.data.repository.TransactionRepositoryImpl
import com.trader.salesmanager.domain.repository.ActivationRepository
import com.trader.salesmanager.domain.repository.CustomerRepository
import com.trader.salesmanager.domain.repository.PaymentMethodRepository
import com.trader.salesmanager.domain.repository.TransactionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<ActivationRepository> {
        ActivationRepositoryImpl(
            context          = androidContext(),
            firebaseService  = get(),
            customerDao      = get(),
            transactionDao   = get(),
            paymentMethodDao = get()
        )
    }
    single<CustomerRepository> {
        CustomerRepositoryImpl(
            dao            = get(),
            syncService    = get(),
            activationRepo = get()
        )
    }
    single<TransactionRepository> {
        TransactionRepositoryImpl(
            transactionDao   = get(),
            customerDao      = get(),
            paymentMethodDao = get(),
            syncService      = get(),
            activationRepo   = get()
        )
    }
    single<PaymentMethodRepository> {
        PaymentMethodRepositoryImpl(
            dao            = get(),
            syncService    = get(),
            activationRepo = get()
        )
    }
}
