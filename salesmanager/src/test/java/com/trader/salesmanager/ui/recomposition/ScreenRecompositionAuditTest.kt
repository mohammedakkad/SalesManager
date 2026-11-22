package com.trader.salesmanager.ui.recomposition

import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.LowStockSettingsRepository
import com.trader.core.sync.GlobalSyncState
import com.trader.salesmanager.ui.activation.ActivationScreen
import com.trader.salesmanager.ui.activation.ActivationViewModel
import com.trader.salesmanager.ui.boxes.BoxesScreen
import com.trader.salesmanager.ui.boxes.BoxesViewModel
import com.trader.salesmanager.ui.chat.ChatScreen
import com.trader.salesmanager.ui.chat.ChatViewModel
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerScreen
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerViewModel
import com.trader.salesmanager.ui.customers.details.CustomerDetailsScreen
import com.trader.salesmanager.ui.customers.details.CustomerDetailsViewModel
import com.trader.salesmanager.ui.customers.list.CustomersScreen
import com.trader.salesmanager.ui.customers.list.CustomersUiState
import com.trader.salesmanager.ui.customers.list.CustomersViewModel
import com.trader.salesmanager.ui.debts.DebtsScreen
import com.trader.salesmanager.ui.debts.DebtsViewModel
import com.trader.salesmanager.ui.employees.EmployeeManagementScreen
import com.trader.salesmanager.ui.employees.EmployeeManagementViewModel
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductScreen
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductViewModel
import com.trader.salesmanager.ui.inventory.detail.ProductDetailScreen
import com.trader.salesmanager.ui.inventory.detail.ProductDetailViewModel
import com.trader.salesmanager.ui.inventory.detail.ProductDetailUiState
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsScreen
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsUiState
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsViewModel
import com.trader.salesmanager.ui.inventory.list.InventoryListScreen
import com.trader.salesmanager.ui.inventory.list.InventoryListUiState
import com.trader.salesmanager.ui.inventory.list.InventoryListViewModel
import com.trader.salesmanager.ui.inventory.reports.StockReportsScreen
import com.trader.salesmanager.ui.inventory.reports.StockReportsViewModel
import com.trader.salesmanager.ui.inventory.session.InventorySessionScreen
import com.trader.salesmanager.ui.inventory.session.InventorySessionViewModel
import com.trader.salesmanager.ui.navigation.BlockedScreen
import com.trader.salesmanager.ui.navigation.SplashCheckScreen
import com.trader.salesmanager.ui.payments.PaymentMethodsScreen
import com.trader.salesmanager.ui.payments.PaymentMethodsViewModel
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.returns.ReturnProcessScreen
import com.trader.salesmanager.ui.returns.ReturnViewModel
import com.trader.salesmanager.ui.settings.SettingsScreen
import com.trader.salesmanager.ui.settings.backup.BackupScreen
import com.trader.salesmanager.ui.settings.backup.BackupViewModel
import com.trader.salesmanager.ui.settings.sessions.SessionsScreen
import com.trader.salesmanager.ui.settings.sessions.SessionsViewModel
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionScreen
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionUiState
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsScreen
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsViewModel
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsUiState
import com.trader.salesmanager.ui.transactions.list.TransactionsScreen
import com.trader.salesmanager.ui.transactions.list.TransactionsViewModel
import com.trader.salesmanager.update.AppUpdateViewModel
import com.trader.testing.recomposition.FakeViewModels.fake
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recomposition audit of every seller screen (see RecompositionAuditFixture). Writes build/recomposition. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ar-w360dp-h800dp")
class ScreenRecompositionAuditTest : RecompositionAuditFixture() {
    private val day = 1767225600000L
    private val customer = Customer(7, "أحمد علي", phone = "0599123456")
    private val txs = listOf(
        Transaction(41, 7, "أحمد علي", 120.0, isPaid = true, date = day),
        Transaction(42, 8, "سارة محمود", 75.25, isPaid = false, date = day)
    )
    @org.junit.Before fun koinExtras() {
        org.koin.core.context.loadKoinModules(org.koin.dsl.module {
            single<com.trader.core.domain.repository.PaymentMethodRepository> { mockk(relaxed = true) }
        })
    }
    private fun product(i: Int) = ProductWithUnits(
        Product("p$i", "62900$i", "صنف $i", "عام"),
        listOf(ProductUnit("u$i", "p$i", UnitType.PIECE, "قطعة", 2.5 + i, 1.0, 20.0, isDefault = true))
    )

    @Test fun home() {
        val (vm, _) = fake<HomeViewModel>(mapOf("uiState" to HomeUiState(todayTotal = 1250.5, todayPaid = 900.0, todayUnpaid = 350.5,
            unreadChatCount = 3, recentTransactions = txs, dashboard = DashboardUiState(1250.5, 14, 2340.0,
                listOf(TopSellingProduct("p", "أرز بسمتي", 32.0)), listOf(TopDebtorCustomer(7, "محمد خالد", 540.0)),
                listOf(DailySales(day, 100.0), DailySales(day + 86_400_000, 300.0))))))
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.AllSynced))
        measure("home", { ui.onNodeWithText("عملية جديدة", useUnmergedTree = true).performClick() }) { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }

    @Test fun reports() {
        val (vm, _) = fake<ReportsViewModel>(mapOf("uiState" to ReportsUiState(isLoading = false, totalAmount = 5400.0, paidAmount = 4100.0,
            unpaidAmount = 1300.0, txCount = 48, netProfit = 980.75,
            dailySales = listOf(DaySalesEntry("01/01", 300.0, 200.0), DaySalesEntry("02/01", 120.0, 120.0)),
            paymentShares = listOf(PaymentShare("نقد", 70.0), PaymentShare("دين", 30.0)),
            salesProfitLast7Days = listOf(SalesProfitDayEntry("01/01", 300.0, 60.0)))))
        measure("reports") { ReportsScreen({}, viewModel = vm) }
    }

    @Test fun dayTransactions() {
        val (vm, _) = fake<DayTransactionsViewModel>(mapOf("uiState" to DayTxUiState(all = txs, paymentMethods = listOf("نقد"),
            totalAmount = 195.25, paidAmount = 120.0, unpaidAmount = 75.25, isLoading = false)))
        measure("day-transactions") { DayTransactionsScreen(day, {}, {}, vm) }
    }

    @Test fun addTransaction() {
        val (vm, _) = fake<AddEditTransactionViewModel>(mapOf("uiState" to AddEditTransactionUiState(customers = listOf(customer),
            selectedCustomer = customer, amount = "125")))
        measure("transaction-add") { AddEditTransactionScreen(null, null, {}, viewModel = vm) }
    }

    @Test fun editTransaction() {
        val (vm, _) = fake<AddEditTransactionViewModel>(mapOf("uiState" to AddEditTransactionUiState(customers = listOf(customer),
            selectedCustomer = customer, amount = "125", note = "ملاحظة")))
        measure("transaction-edit") { AddEditTransactionScreen(41, null, {}, viewModel = vm) }
    }

    @Test fun customers() {
        val (vm, _) = fake<CustomersViewModel>(mapOf("uiState" to CustomersUiState(isLoading = false,
            customers = (1..12).map { Customer(it.toLong(), "زبون $it", phone = "05000000$it") }), "deleteConfirm" to null))
        measure("customers-list") { CustomersScreen({}, {}, {}, viewModel = vm) }
    }

    @Test fun invoice() {
        val products = (1..8).map(::product)
        val (vm, _) = fake<InvoiceItemsViewModel>(mapOf("uiState" to InvoiceItemsUiState(allProducts = products, isLoading = false)))
        measure("invoice-items") { InvoiceItemsScreen("أحمد علي", {}, onConfirm = { _, _ -> }, viewModel = vm) }
    }

    @Test fun inventoryList() {
        val (vm, _) = fake<InventoryListViewModel>(mapOf("uiState" to InventoryListUiState(products = (1..10).map(::product), isLoading = false)))
        measure("inventory-list") { InventoryListScreen({}, {}, {}, {}, viewModel = vm) }
    }

    @Test fun transactions() { val (vm, _) = fake<TransactionsViewModel>(); measure("transactions-list") { TransactionsScreen({}, {}, {}, vm) } }
    @Test fun transactionDetails() {
        val (vm, _) = fake<TransactionDetailsViewModel>(mapOf("uiState" to TransactionDetailsUiState(transaction = txs[0], customer = customer,
            invoiceItems = listOf(InvoiceItem("i", 41, "p1", "صنف 1", "u1", "قطعة", 2.0, 60.0, 120.0)), isItemsLoaded = true)))
        measure("transaction-details") { TransactionDetailsScreen(41, {}, {}, {}, vm) } }
    @Test fun employees() { val (vm, _) = fake<EmployeeManagementViewModel>(); measure("employees") { EmployeeManagementScreen({}, vm) } }
    @Test fun splash() = measure("splash", interaction = null) { SplashCheckScreen() }.let { }
    @Test fun blocked() = measure("blocked") { BlockedScreen("الحساب موقوف", true) {} }.let { }
    @Test fun addCustomer() { val (vm, _) = fake<AddEditCustomerViewModel>(); measure("customer-add") { AddEditCustomerScreen(null, {}, vm) } }
    @Test fun customerDetails() { val (vm, _) = fake<CustomerDetailsViewModel>(); measure("customer-details") { CustomerDetailsScreen(7, {}, {}, {}, {}, vm) } }
    @Test fun chat() { val (vm, _) = fake<ChatViewModel>(); measure("chat") { ChatScreen({}, vm) } }
    @Test fun sessions() { val (vm, _) = fake<SessionsViewModel>(); measure("sessions") { SessionsScreen({}, vm) } }
    @Test fun settings() {
        val (vm, _) = fake<AppUpdateViewModel>(); val low = mockk<LowStockSettingsRepository>(relaxed = true)
        measure("settings") { SettingsScreen({}, {}, updateViewModel = vm, lowStockSettingsRepository = low) }
    }
    @Test fun backup() { val (vm, _) = fake<BackupViewModel>(); measure("backup") { BackupScreen({}, vm) } }
    @Test fun productDetail() {
        val (vm, _) = fake<ProductDetailViewModel>(mapOf("uiState" to ProductDetailUiState(product = product(1), isLoading = false)))
        measure("product-detail") { ProductDetailScreen("p1", {}, {}, vm) } }
    @Test fun addProduct() { val (vm, _) = fake<AddEditProductViewModel>(); measure("product-add") { AddEditProductScreen(null, null, {}, vm) } }
    @Test fun inventorySession() { val (vm, _) = fake<InventorySessionViewModel>(); measure("inventory-session") { InventorySessionScreen({}, vm) } }
    @Test fun stockReports() { val (vm, _) = fake<StockReportsViewModel>(); measure("stock-reports") { StockReportsScreen({}, vm) } }
    @Test fun returns() { val (vm, _) = fake<ReturnViewModel>(); measure("return-process") { ReturnProcessScreen(41, {}, {}, vm) } }
    @Test fun boxes() { val (vm, _) = fake<BoxesViewModel>(); measure("cash-boxes") { BoxesScreen({}, vm) } }
    @Test fun payments() { val (vm, _) = fake<PaymentMethodsViewModel>(); measure("payment-methods") { PaymentMethodsScreen({}, vm) } }
    @Test fun debts() { val (vm, _) = fake<DebtsViewModel>(); measure("debts") { DebtsScreen({}, {}, viewModel = vm) } }
    @Test fun activation() { val (vm, _) = fake<ActivationViewModel>(); measure("activation") { ActivationScreen({}, {}, vm) } }

    // ── animated states that the default fixtures above do not reach ──
    @Test fun chatPendingMessage() {
        val (vm, _) = fake<ChatViewModel>(mapOf("uiState" to com.trader.salesmanager.ui.chat.ChatUiState(isLoading = false, merchantId = "m",
            pendingMessages = listOf(com.trader.core.data.local.entity.PendingMessageEntity("t", "m", "رسالة قيد الإرسال", "التاجر")))))
        measure("chat-pending", interaction = null) { ChatScreen({}, vm) }
    }
    @Test fun homeSyncPending() {
        val (vm, _) = fake<HomeViewModel>(mapOf("uiState" to HomeUiState()))
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.Pending(3)))
        measure("home-sync-pending", interaction = null) { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }
    @Test fun homeSyncFailed() {
        val (vm, _) = fake<HomeViewModel>(mapOf("uiState" to HomeUiState()))
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.Failed(2)))
        measure("home-sync-failed", interaction = null) { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }
    @Test fun exportInProgress() = measure("export-loading", interaction = null) {
        com.trader.salesmanager.util.export.ExportActionButton(com.trader.salesmanager.util.export.ExportTarget.INVOICE_PDF,
            com.trader.salesmanager.util.export.ExportState.Loading(), {}, {})
    }.let { }
    @Test fun emptyState() = measure("empty-state", interaction = null) {
        com.trader.salesmanager.ui.components.EmptyState(androidx.compose.material.icons.Icons.Rounded.Info, "لا توجد بيانات", "أضف أول عنصر")
    }.let { }
}
