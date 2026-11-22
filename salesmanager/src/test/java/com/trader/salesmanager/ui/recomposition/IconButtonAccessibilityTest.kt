package com.trader.salesmanager.ui.recomposition

import androidx.compose.runtime.Composable
import com.trader.core.domain.model.*
import com.trader.core.sync.GlobalSyncState
import com.trader.salesmanager.ui.coverage.AppThemeUiFixture
import com.trader.salesmanager.ui.customers.list.CustomersScreen
import com.trader.salesmanager.ui.customers.list.CustomersUiState
import com.trader.salesmanager.ui.customers.list.CustomersViewModel
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.inventory.list.InventoryListScreen
import com.trader.salesmanager.ui.inventory.list.InventoryListUiState
import com.trader.salesmanager.ui.inventory.list.InventoryListViewModel
import com.trader.salesmanager.ui.payments.PaymentMethodsScreen
import com.trader.salesmanager.ui.payments.PaymentMethodsUiState
import com.trader.salesmanager.ui.payments.PaymentMethodsViewModel
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsScreen
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsUiState
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsViewModel
import com.trader.salesmanager.util.export.ExportActionButton
import com.trader.salesmanager.util.export.ExportState
import com.trader.salesmanager.util.export.ExportTarget
import com.trader.testing.recomposition.ClickableAccessibility
import com.trader.testing.recomposition.FakeViewModels.fake
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Icon-only buttons are announced by TalkBack and every clickable target is at least 48 dp. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ar-w360dp-h800dp")
class IconButtonAccessibilityTest : AppThemeUiFixture() {
    private val day = 1767225600000L
    private fun product(i: Int) = ProductWithUnits(Product("p$i", "62900$i", "صنف $i", "عام"),
        listOf(ProductUnit("u$i", "p$i", UnitType.PIECE, "قطعة", 2.5 + i, 1.0, 20.0, isDefault = true)))

    private fun check(name: String, content: @Composable () -> Unit) {
        showApp { content() }
        ui.waitForIdle()
        val problems = ClickableAccessibility.violations(ui)
        println("A11Y $name ${problems.size}: $problems")
        assertTrue("$name:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    @Test fun home() {
        val (vm, _) = fake<HomeViewModel>(mapOf("uiState" to HomeUiState(todayTotal = 1250.5, todayPaid = 900.0, todayUnpaid = 350.5, unreadChatCount = 3,
            recentTransactions = listOf(Transaction(41, 7, "أحمد", 10.0, isPaid = true, date = day)),
            dashboard = DashboardUiState(1250.5, 14, 2340.0, listOf(TopSellingProduct("p", "أرز", 32.0)), listOf(TopDebtorCustomer(7, "محمد", 540.0))))))
        val (sync, _) = fake<SyncStatusViewModel>(mapOf("syncState" to GlobalSyncState.Pending(2)))
        check("home") { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }
    @Test fun customers() {
        val (vm, _) = fake<CustomersViewModel>(mapOf("uiState" to CustomersUiState(customers = listOf(Customer(7, "أحمد"))), "deleteConfirm" to null))
        check("customers") { CustomersScreen({}, {}, {}, viewModel = vm) }
    }
    @Test fun inventorySearch() {
        val (vm, _) = fake<InventoryListViewModel>(mapOf("uiState" to InventoryListUiState(products = listOf(product(1)), query = "صنف", isLoading = false)))
        check("inventory") { InventoryListScreen({}, {}, {}, {}, viewModel = vm) }
    }
    @Test fun dayTransactions() {
        val (vm, _) = fake<DayTransactionsViewModel>(mapOf("uiState" to DayTxUiState(all = listOf(Transaction(41, 7, "أحمد", 10.0, isPaid = true)), query = "أ", isLoading = false)))
        check("day") { DayTransactionsScreen(day, {}, {}, vm) }
    }
    @Test fun transactionDetails() {
        val (vm, _) = fake<TransactionDetailsViewModel>(mapOf("uiState" to TransactionDetailsUiState(Transaction(41, 7, "أحمد", 10.0, isPaid = true, date = day), isItemsLoaded = true)))
        check("transaction-details") { TransactionDetailsScreen(41, {}, {}, {}, vm) }
    }
    @Test fun paymentMethods() {
        val (vm, _) = fake<PaymentMethodsViewModel>(mapOf("uiState" to PaymentMethodsUiState(listOf(PaymentMethod(1, "بنك فلسطين", PaymentType.BANK)))))
        check("payments") { PaymentMethodsScreen({}, vm) }
    }
    @Test fun exportError() = check("export-error") {
        ExportActionButton(ExportTarget.INVOICE_PDF, ExportState.Error("تعذّر التصدير"), {}, {})
    }
}
