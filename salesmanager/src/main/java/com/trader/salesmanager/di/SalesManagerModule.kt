package com.trader.salesmanager.di

import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.manager.EmployeeSessionManager
import com.trader.core.data.remote.ChatService
import com.trader.core.data.remote.CloudinaryUploader
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.repository.ActivationRepositoryImpl
import com.trader.core.data.repository.CashBoxRepositoryImpl
import com.trader.core.data.repository.ChatRepositoryImpl
import com.trader.core.data.repository.CustomerRepositoryImpl
import com.trader.core.data.repository.EmployeeRepositoryImpl
import com.trader.core.data.repository.InventoryRepositoryImpl
import com.trader.core.data.repository.InvoiceItemRepositoryImpl
import com.trader.core.data.repository.MerchantStatusRepositoryImpl
import com.trader.core.data.repository.PaymentMethodRepositoryImpl
import com.trader.core.data.repository.ProductRepositoryImpl
import com.trader.core.data.repository.ReportsRepositoryImpl
import com.trader.core.data.repository.ReturnRepositoryImpl
import com.trader.core.data.repository.StockRepositoryImpl
import com.trader.core.data.repository.TransactionRepositoryImpl
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CashBoxRepository
import com.trader.core.domain.repository.ChatRepository
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.EmployeeRepository
import com.trader.core.domain.repository.InventoryRepository
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.MerchantStatusRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.ReportsRepository
import com.trader.core.domain.repository.ReturnRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.NetworkMonitor
import com.trader.core.sync.SyncCoordinator
import com.trader.salesmanager.ui.activation.ActivationViewModel
import com.trader.salesmanager.ui.boxes.BoxesViewModel
import com.trader.salesmanager.ui.activation.MerchantWatcherViewModel
import com.trader.salesmanager.ui.chat.ChatViewModel
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerViewModel
import com.trader.salesmanager.ui.customers.details.CustomerDetailsViewModel
import com.trader.salesmanager.ui.customers.list.CustomersViewModel
import com.trader.salesmanager.ui.debts.DebtsViewModel
import com.trader.salesmanager.ui.employees.EmployeeManagementViewModel
import com.trader.salesmanager.ui.home.HomeViewModel
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductViewModel
import com.trader.salesmanager.ui.inventory.detail.ProductDetailViewModel
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsViewModel
import com.trader.salesmanager.ui.inventory.list.InventoryListViewModel
import com.trader.salesmanager.ui.inventory.reports.StockReportsViewModel
import com.trader.salesmanager.ui.inventory.session.InventorySessionViewModel
import com.trader.salesmanager.ui.payments.PaymentMethodsViewModel
import com.trader.salesmanager.ui.reports.DayTransactionsViewModel
import com.trader.salesmanager.ui.reports.ReportsViewModel
import com.trader.salesmanager.ui.returns.ReturnViewModel
import com.trader.salesmanager.ui.settings.sessions.SessionsViewModel
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsViewModel
import com.trader.salesmanager.ui.transactions.list.TransactionsViewModel
import com.trader.salesmanager.update.AppUpdateViewModel
import com.trader.salesmanager.util.export.ExportViewModel
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
    single {
        get<AppDatabase>().returnDao()
    }
    single {
        get<AppDatabase>().sessionDao()
    }
    single {
        get<AppDatabase>().employeeDao()
    }
    single {
        get<AppDatabase>().cashBoxDao()
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

    // ── Repositories ─────────────────────────────────────────────
    single<ActivationRepository> {
        ActivationRepositoryImpl(
            androidContext(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }

    // ── merchantId helper ─────────────────────────────────────────
    single<String>(org.koin.core.qualifier.named("merchantId")) {
        runBlocking {
            get<ActivationRepository>().getMerchantCode()
        }
    }

    single<CustomerRepository> {
        CustomerRepositoryImpl(get(), get(), get(), get())
    }
    single<TransactionRepository> {
        TransactionRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get())
    }
    single<PaymentMethodRepository> {
        PaymentMethodRepositoryImpl(get(), get(), get(), get())
    }
    single<CashBoxRepository> {
        CashBoxRepositoryImpl(get(), get(), get(), get())
    }
    single<ChatRepository> {
        ChatRepositoryImpl(get())
    }
    single<MerchantStatusRepository> {
        MerchantStatusRepositoryImpl()
    }
    single<ProductRepository> {
        ProductRepositoryImpl(get(), get(), get(), get(), get<NetworkMonitor>())
    }
    single<StockRepository> {
        StockRepositoryImpl(
            get(),
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId"))
        )
    }
    single<InvoiceItemRepository> {
        InvoiceItemRepositoryImpl(
            get(),
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId"))
        )
    }
    single<InventoryRepository> {
        InventoryRepositoryImpl(
            get(),
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId"))
        )
    }
    single<ReturnRepository> {
        ReturnRepositoryImpl(
            get(),
            get(),
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId")),
            get(),
            get()
        )
    }
    single<ReportsRepository> {
        ReportsRepositoryImpl(get(), get(), get())
    }
    single<EmployeeRepository> {
        EmployeeRepositoryImpl(
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId")),
            get(),
            get()
        )
    }
    single {
        EmployeeSessionManager(get())
    }

    single {
        SyncCoordinator(
            networkMonitor = get(),
            stockRepository = get(),
            invoiceItemRepository = get(),
            cashBoxRepository = get()
        )
    }

    // ── ViewModels ───────────────────────────────────────────────
    viewModel {
        ActivationViewModel(get())
    }
    viewModel {
        ExportViewModel()
    }
    viewModel {
        MerchantWatcherViewModel(get(), get())
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
    viewModel { params ->
        CustomerDetailsViewModel(get(), get(), params.get())
    }
    viewModel {
        TransactionsViewModel(get())
    }
    viewModel { params ->
        TransactionDetailsViewModel(params.get(), get(), get(), get(), get())
    }
    viewModel {
        AddEditTransactionViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId"))
        )
    }
    viewModel {
        ReportsViewModel(get(), get(), get())
    }
    viewModel { params ->
        DayTransactionsViewModel(get(), params.get())
    }
    viewModel {
        PaymentMethodsViewModel(get())
    }
    viewModel {
        BoxesViewModel(get(), get())
    }
    viewModel {
        DebtsViewModel(get(), get())
    }
    viewModel {
        ChatViewModel(get(), get(), get())
    }
    viewModel {
        AppUpdateViewModel(get())
    }
    viewModel {
        SessionsViewModel(
            sessionDao = get(),
            merchantCode = get(qualifier = org.koin.core.qualifier.named("merchantId"))
        )
    }
    viewModel {
        EmployeeManagementViewModel(repository = get())
    }
    viewModel {
        InventoryListViewModel(get(), get())
    }
    viewModel {
        AddEditProductViewModel(get())
    }
    viewModel { params ->
        ProductDetailViewModel(get(), get(), params.get())
    }
    viewModel {
        InventorySessionViewModel(
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("merchantId"))
        )
    }
    viewModel {
        InvoiceItemsViewModel(get())
    }
    viewModel {
        StockReportsViewModel(get(), get())
    }
    viewModel { params ->
        ReturnViewModel(
            get(),
            get(),
            get(),
            get(org.koin.core.qualifier.named("merchantId")),
            params.get()
        )
    }

    single { CloudinaryUploader(get()) }
    single {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
}
