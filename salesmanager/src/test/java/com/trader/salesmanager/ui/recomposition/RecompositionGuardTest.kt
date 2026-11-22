package com.trader.salesmanager.ui.recomposition

import com.trader.core.domain.model.*
import com.trader.core.sync.GlobalSyncState
import com.trader.salesmanager.ui.customers.list.CustomersScreen
import com.trader.salesmanager.ui.customers.list.CustomersUiState
import com.trader.salesmanager.ui.customers.list.CustomersViewModel
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsScreen
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsUiState
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsViewModel
import com.trader.salesmanager.ui.recomposition.FakeViewModels.fake
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionScreen
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionUiState
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ar-w360dp-h800dp")
class RecompositionGuardTest : RecompositionAuditFixture() {
    private val day = 1767225600000L
    private val customer = Customer(7, "أحمد علي", phone = "0599123456")
    private val txs = listOf(Transaction(41, 7, "أحمد علي", 120.0, isPaid = true, date = day), Transaction(42, 8, "سارة محمود", 75.25, isPaid = false, date = day))
    private fun product(i: Int) = ProductWithUnits(Product("p$i", "62900$i", "صنف $i", "عام"),
        listOf(ProductUnit("u$i", "p$i", UnitType.PIECE, "قطعة", 2.5 + i, 1.0, 20.0, isDefault = true)))

    @Before fun koinExtras() {
        org.koin.core.context.loadKoinModules(org.koin.dsl.module {
            single<com.trader.core.domain.repository.PaymentMethodRepository> { mockk(relaxed = true) }
        })
    }

    /**
     * Guards: (1) 10 s idle runs no composable at all; (2) one state change re-runs only the named
     * composables in [allowedNamed] (lambdas aside) and at most [maxApp] app composable bodies.
     * Caps are the audited values; raising one needs a reason in docs/qa/recomposition-audit.md.
     */
    private fun guard(label: String, idle: Phase, change: Phase, maxApp: Int, allowedNamed: Set<String> = emptySet()) {
        println("GUARD $label idle=${idle.total}/${idle.app} change=${change.total}/${change.app} ${change.appCounts}")
        assertEquals("$label: idle frames must not recompose anything: ${idle.hot}", 0, idle.total)
        val named = change.appCounts.keys.filterNot { "<anonymous>" in it || it.endsWith("<get-appColors>") }
            .map { it.substringAfterLast('.') }.toSet()
        assertTrue("$label: unexpected composables recomposed: ${named - allowedNamed}", named.all { it in allowedNamed })
        assertTrue("$label: ${change.app} app recompositions > $maxApp: ${change.appCounts}", change.app <= maxApp)
    }

    private fun idleOnly(label: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        open(content)
        val idle = idle()
        assertEquals("$label: idle frames must not recompose anything: ${idle.hot}", 0, idle.total)
    }

    @Test fun home() {
        val homeState = HomeUiState(todayTotal = 1250.5, todayPaid = 900.0, todayUnpaid = 350.5, unreadChatCount = 3, recentTransactions = txs,
            dashboard = DashboardUiState(1250.5, 14, 2340.0, listOf(TopSellingProduct("p", "أرز بسمتي", 32.0)),
                listOf(TopDebtorCustomer(7, "محمد خالد", 540.0)), listOf(DailySales(day, 100.0))))
        val (vm, flows) = fake<HomeViewModel>(mapOf("uiState" to homeState))
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.AllSynced))
        open { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
        val idle = idle()
        val change = after { flows.getValue("uiState").value = homeState.copy(unreadChatCount = 4) }
        guard("home", idle, change, maxApp = 4)
    }

    @Test fun reports() {
        val s = ReportsUiState(isLoading = false, totalAmount = 5400.0, paidAmount = 4100.0, unpaidAmount = 1300.0, txCount = 48, netProfit = 980.75,
            dailySales = listOf(DaySalesEntry("01/01", 300.0, 200.0), DaySalesEntry("02/01", 120.0, 120.0)),
            paymentShares = listOf(PaymentShare("نقد", 70.0), PaymentShare("دين", 30.0)), salesProfitLast7Days = listOf(SalesProfitDayEntry("01/01", 300.0, 60.0)))
        val (vm, flows) = fake<ReportsViewModel>(mapOf("uiState" to s))
        open { ReportsScreen({}, viewModel = vm) }
        val idle = idle()
        val change = after { flows.getValue("uiState").value = s.copy(netProfit = 990.75) }
        guard("reports", idle, change, maxApp = 8, allowedNamed = setOf("MetricCard"))
    }

    @Test fun transactionAdd() {
        val s = AddEditTransactionUiState(customers = listOf(customer), selectedCustomer = customer, amount = "125")
        val (vm, flows) = fake<AddEditTransactionViewModel>(mapOf("uiState" to s))
        open { AddEditTransactionScreen(null, null, {}, viewModel = vm) }
        val idle = idle()
        val change = after { flows.getValue("uiState").value = s.copy(amount = "1250") }
        guard("transaction-add", idle, change, maxApp = 15, allowedNamed = setOf("AddEditTransactionScreen"))
    }

    @Test fun transactionEdit() {
        val s = AddEditTransactionUiState(customers = listOf(customer), selectedCustomer = customer, amount = "125", note = "ملاحظة")
        val (vm, flows) = fake<AddEditTransactionViewModel>(mapOf("uiState" to s))
        open { AddEditTransactionScreen(41, null, {}, viewModel = vm) }
        val idle = idle()
        val change = after { flows.getValue("uiState").value = s.copy(note = "ملاحظة ٢") }
        guard("transaction-edit", idle, change, maxApp = 15, allowedNamed = setOf("AddEditTransactionScreen"))
    }

    @Test fun customers() {
        val s = CustomersUiState(isLoading = false, customers = (1..12).map { Customer(it.toLong(), "زبون $it", phone = "05000000$it") })
        val (vm, flows) = fake<CustomersViewModel>(mapOf("uiState" to s, "deleteConfirm" to null))
        open { CustomersScreen({}, {}, {}, viewModel = vm) }
        val idle = idle()
        val change = after { flows.getValue("uiState").value = s.copy(pendingSyncCount = 1) }
        guard("customers", idle, change, maxApp = 4)
    }

    @Test fun invoice() {
        val s = InvoiceItemsUiState(allProducts = (1..8).map(::product), isLoading = false)
        val (vm, flows) = fake<InvoiceItemsViewModel>(mapOf("uiState" to s))
        open { InvoiceItemsScreen("أحمد علي", {}, onConfirm = { _, _ -> }, viewModel = vm) }
        val idle = idle()
        val change = after { flows.getValue("uiState").value = s.copy(searchQuery = "صنف") }
        guard("invoice", idle, change, maxApp = 8)
    }

    // Infinite animations must run in the draw phase only (fixed in Phase 6C).
    @Test fun animatedStatesDoNotRecomposeWhileIdle() {
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.Pending(3)))
        val (vm, _) = fake<HomeViewModel>(mapOf("uiState" to HomeUiState()))
        idleOnly("sync-pending") { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }
    @Test fun failedSyncIconDoesNotRecomposeWhileIdle() {
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.Failed(3)))
        val (vm, _) = fake<HomeViewModel>(mapOf("uiState" to HomeUiState()))
        idleOnly("sync-failed") { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }
    @Test fun pendingChatBubbleDoesNotRecomposeWhileIdle() {
        val (vm, _) = fake<com.trader.salesmanager.ui.chat.ChatViewModel>(mapOf("uiState" to com.trader.salesmanager.ui.chat.ChatUiState(
            isLoading = false, merchantId = "m", pendingMessages = listOf(com.trader.core.data.local.entity.PendingMessageEntity("t", "m", "رسالة", "التاجر")))))
        idleOnly("chat-pending") { com.trader.salesmanager.ui.chat.ChatScreen({}, vm) }
    }
    @Test fun exportProgressDoesNotRecomposeWhileIdle() = idleOnly("export-loading") {
        com.trader.salesmanager.util.export.ExportActionButton(com.trader.salesmanager.util.export.ExportTarget.INVOICE_PDF,
            com.trader.salesmanager.util.export.ExportState.Loading(), {}, {})
    }
    @Test fun splashDoesNotRecomposeWhileIdle() = idleOnly("splash") { com.trader.salesmanager.ui.navigation.SplashCheckScreen() }
    @Test fun activationDoesNotRecomposeWhileIdle() {
        val (vm, _) = fake<com.trader.salesmanager.ui.activation.ActivationViewModel>()
        idleOnly("activation") { com.trader.salesmanager.ui.activation.ActivationScreen({}, {}, vm) }
    }
}
