package com.trader.salesmanager.di

import com.trader.core.data.local.appDataStore
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.remote.ChatService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.repository.*
import com.trader.core.domain.repository.*
import com.trader.core.util.NetworkMonitor
import com.trader.salesmanager.ui.inventory.list.InventoryListViewModel
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductViewModel
import com.trader.salesmanager.ui.inventory.detail.ProductDetailViewModel
import com.trader.salesmanager.ui.inventory.session.InventorySessionViewModel
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsViewModel
import com.trader.salesmanager.ui.inventory.reports.StockReportsViewModel
import com.trader.salesmanager.update.AppUpdateViewModel
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
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsViewModel
import com.trader.salesmanager.ui.activation.ActivationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val salesManagerModule = module {
    // ── Database ─────────────────────────────────────────────────
    single {
        AppDatabase.build(androidContext())
    }
    single {
        get<AppDatabase>().customerDao()
    }
    single {
        get<AppDatabase>().transactionDao()
    }
    single {
        get<AppDatabase>().paymentMethodDao()
    }
    single {
        get<AppDatabase>().pendingMessageDao()
    }
    single {
        get<AppDatabase>().productDao()
    }
    single {
        get<AppDatabase>().stockMovementDao()
    }
    single {
        get<AppDatabase>().invoiceItemDao()
    }
    single {
        get<AppDatabase>().inventoryDao()
    }

    // ── Remote ───────────────────────────────────────────────────
    single {
        FirebaseSyncService()
    }
    single {
        ChatService()
    }
    single {
        ProductFirestoreService()
    }
    single {
        NetworkMonitor(androidContext())
    }

    // ── merchantId helper ─────────────────────────────────────────
    // merchant_code = activationCode = merchantId المستخدم في Firestore
    single<String>(org.koin.core.qualifier.named("merchantId")) {
        runBlocking {
            androidContext().appDataStore.data
            .map {
                prefs -> prefs[androidx.datastore.preferences.core.stringPreferencesKey("merchant_code")] ?: ""
            }
            .first()
        }
    }

    // ── Repositories ─────────────────────────────────────────────
    single<ActivationRepository> {
        ActivationRepositoryImpl(androidContext(), get(), get(), get(), get(), get(), get())
    }
    single<CustomerRepository> {
        CustomerRepositoryImpl(get(), get(), get())
    }
    single<TransactionRepository> {
        TransactionRepositoryImpl(get(), get(), get(), get(), get(), get(), get())
    }
    single<PaymentMethodRepository> {
        PaymentMethodRepositoryImpl(get(), get(), get())
    }
    single<ChatRepository> {
        ChatRepositoryImpl(get())
    }
    single<MerchantStatusRepository> {
        MerchantStatusRepositoryImpl()
    }
    single<ProductRepository> {
        ProductRepositoryImpl(get(),get(), get(), get(), get<NetworkMonitor>())
    }
    single<StockRepository> {
        StockRepositoryImpl(get(), get(), get(), get(qualifier = org.koin.core.qualifier.named("merchantId")))
    }
    single<InvoiceItemRepository> {
        InvoiceItemRepositoryImpl(get(), get(), get(qualifier = org.koin.core.qualifier.named("merchantId")))
    }
    single<InventoryRepository> {
        InventoryRepositoryImpl(get(), get(), get(), get(qualifier = org.koin.core.qualifier.named("merchantId")))
    }

    // ── ViewModels ───────────────────────────────────────────────
    viewModel {
        ActivationViewModel(get(), get())
    }
    viewModel {
        MerchantWatcherViewModel(get(), get(), androidContext())
    }
    viewModel {
        HomeViewModel(get(), get(), get())
    }
    viewModel {
        CustomersViewModel(get())
    }
    viewModel {
        AddEditCustomerViewModel(get())
    }
    viewModel {
        params -> CustomerDetailsViewModel(get(), get(), params.get())
    }
    viewModel {
        TransactionsViewModel(get())
    }
    viewModel {
        params ->
        TransactionDetailsViewModel(params.get(), get(), get(), get())
    }
    viewModel {
        AddEditTransactionViewModel(get(), get(), get(), get(), get(), get(qualifier = org.koin.core.qualifier.named("merchantId")))
    }
    viewModel {
        ReportsViewModel(get(), get())
    }
    viewModel {
        params -> DayTransactionsViewModel(get(), params.get())
    }
    viewModel {
        PaymentMethodsViewModel(get())
    }
    viewModel {
        DebtsViewModel(get(), get())
    }
    viewModel {
        ChatViewModel(get(), get(), get())
    }
    viewModel {
        AppUpdateViewModel()
    }
    // ── Inventory ─────────────────────────────────────────────────
    viewModel {
        InventoryListViewModel(get(), get())
    }
    viewModel {
        AddEditProductViewModel(get())
    }
    viewModel {
        params -> ProductDetailViewModel(get(), get(), params.get())
    }
    viewModel {
        InventorySessionViewModel(get(), get(), get(qualifier = org.koin.core.qualifier.named("merchantId")))
    }
    viewModel {
        InvoiceItemsViewModel(get())
    }
    viewModel {
        StockReportsViewModel(get(), get())
    }
}