package com.trader.salesmanager.di

import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.remote.ChatService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.data.repository.*
import com.trader.core.domain.repository.*
import com.trader.core.data.repository.MerchantStatusRepositoryImpl
import com.trader.core.util.NetworkMonitor
import com.trader.salesmanager.ui.activation.ActivationViewModel
import com.trader.salesmanager.ui.activation.MerchantWatcherViewModel
import com.trader.salesmanager.ui.chat.ChatViewModel
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerViewModel
import com.trader.salesmanager.ui.customers.details.CustomerDetailsViewModel
import com.trader.salesmanager.ui.customers.list.CustomersViewModel
import com.trader.salesmanager.ui.debts.DebtsViewModel
import com.trader.salesmanager.ui.home.HomeViewModel
import com.trader.salesmanager.ui.payments.PaymentMethodsViewModel
import com.trader.salesmanager.ui.reports.ReportsViewModel
import com.trader.salesmanager.ui.reports.DayTransactionsViewModel
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import com.trader.salesmanager.ui.transactions.list.TransactionsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val salesManagerModule = module {
    // ── Database ─────────────────────────────────────────────────
    single { AppDatabase.build(androidContext()) }
    single { get<AppDatabase>().customerDao() }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().paymentMethodDao() }
    single { get<AppDatabase>().pendingMessageDao() }   // ← جديد

    // ── Remote ───────────────────────────────────────────────────
    single { FirebaseSyncService() }
    single { ChatService() }
    single { NetworkMonitor(androidContext()) }

    // ── Repositories ─────────────────────────────────────────────
    single<ActivationRepository>    { ActivationRepositoryImpl(androidContext(), get(), get(), get(), get()) }
    single<CustomerRepository>      { CustomerRepositoryImpl(get(), get(), get()) }
    single<TransactionRepository>   { TransactionRepositoryImpl(get(), get(), get(), get(), get()) }
    single<PaymentMethodRepository> { PaymentMethodRepositoryImpl(get(), get(), get()) }
    single<ChatRepository>          { ChatRepositoryImpl(get()) }
    single<MerchantStatusRepository>{ MerchantStatusRepositoryImpl() }

    // ── ViewModels ───────────────────────────────────────────────
    viewModel { ActivationViewModel(get(), get()) }
    viewModel { MerchantWatcherViewModel(get(), get(), androidContext()) }
    viewModel { HomeViewModel(get(), get(), get()) }           // ← أُضيف chatRepo + activationRepo
    viewModel { CustomersViewModel(get()) }
    viewModel { AddEditCustomerViewModel(get()) }
    viewModel { params -> CustomerDetailsViewModel(get(), get(), params.get()) }
    viewModel { TransactionsViewModel(get()) }
    viewModel { AddEditTransactionViewModel(get(), get()) }
    viewModel { ReportsViewModel(get(), get()) }
    viewModel { params -> DayTransactionsViewModel(get(), params.get()) }
    viewModel { PaymentMethodsViewModel(get()) }
    viewModel { DebtsViewModel(get(), get()) }
    viewModel { ChatViewModel(get(), get(), get()) }           // ← أُضيف pendingMessageDao
}
