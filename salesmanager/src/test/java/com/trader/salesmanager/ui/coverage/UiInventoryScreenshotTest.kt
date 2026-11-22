package com.trader.salesmanager.ui.coverage

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.TopDebtorCustomer
import com.trader.core.domain.model.TopSellingProduct
import com.trader.core.domain.model.Transaction
import com.trader.core.sync.GlobalSyncState
import com.trader.core.sync.UnsyncedItem
import com.trader.salesmanager.ui.components.EmptyState
import com.trader.salesmanager.ui.components.ModernSearchBar
import com.trader.salesmanager.ui.components.StatCard
import com.trader.salesmanager.ui.components.StatusChip
import com.trader.salesmanager.ui.home.DashboardUiState
import com.trader.salesmanager.ui.home.HomeScreen
import com.trader.salesmanager.ui.home.HomeUiState
import com.trader.salesmanager.ui.home.HomeViewModel
import com.trader.salesmanager.ui.home.SyncStatusViewModel
import com.trader.salesmanager.ui.reports.DayTransactionsScreen
import com.trader.salesmanager.ui.reports.DayTransactionsViewModel
import com.trader.salesmanager.ui.reports.DayTxUiState
import com.trader.salesmanager.ui.reports.DaySalesEntry
import com.trader.salesmanager.ui.reports.PaymentShare
import com.trader.salesmanager.ui.reports.ReportsScreen
import com.trader.salesmanager.ui.reports.ReportsUiState
import com.trader.salesmanager.ui.reports.ReportsViewModel
import com.trader.salesmanager.ui.reports.SalesProfitDayEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * UI inventory: renders main screens with the real app theme in an Arabic (RTL) locale and saves
 * PNGs to build/ui-inventory/<stage> (stage from env UI_INVENTORY_STAGE, default "current").
 * Configurations: phone light/dark, phone with 1.3x font scale, tablet. It asserts nothing about
 * pixels; it exists so layout changes can be reviewed before and after.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiInventoryScreenshotTest : AppThemeUiFixture() {
    private val stage = System.getenv("UI_INVENTORY_STAGE") ?: "current"
    private val out = File("build/ui-inventory/$stage").apply { mkdirs() }

    private fun capture(name: String, dark: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        // Screens contain infinite animations; drive the clock manually so idling terminates.
        ui.mainClock.autoAdvance = false
        showApp { content() }
        if (dark) dark(true)
        ui.mainClock.advanceTimeBy(2000)
        // Same technique as ReportCanvasRenderingUiTest: draw the decor view into a native bitmap.
        lateinit var image: Bitmap
        ui.runOnIdle {
            val view = activity.window.decorView
            image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
        }
        FileOutputStream(File(out, "$name.png")).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    private fun home(): @Composable () -> Unit {
        val vm = mockk<HomeViewModel>(relaxed = true)
        val sync = mockk<SyncStatusViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(
            HomeUiState(
                todayTotal = 1250.5, todayPaid = 900.0, todayUnpaid = 350.5, unreadChatCount = 3,
                recentTransactions = listOf(
                    Transaction(41, 7, "أحمد علي", 120.0, isPaid = true, date = 1767225600000),
                    Transaction(42, 8, "سارة محمود", 75.25, isPaid = false, date = 1767225600000)
                ),
                dashboard = DashboardUiState(
                    todaySales = 1250.5, todayInvoiceCount = 14, totalOutstandingDebt = 2340.0,
                    topSellingProducts = listOf(TopSellingProduct("p", "أرز بسمتي", 32.0), TopSellingProduct("q", "زيت زيتون", 12.5)),
                    topDebtorCustomers = listOf(TopDebtorCustomer(7, "محمد خالد", 540.0))
                )
            )
        )
        every { sync.syncState } returns MutableStateFlow<GlobalSyncState>(GlobalSyncState.AllSynced)
        every { sync.unsyncedItems } returns MutableStateFlow(emptyList<UnsyncedItem>())
        return { HomeScreen({}, {}, {}, {}, {}, {}, vm, sync) }
    }

    private fun reports(): @Composable () -> Unit {
        val vm = mockk<ReportsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(
            ReportsUiState(
                isLoading = false, totalAmount = 5400.0, paidAmount = 4100.0, unpaidAmount = 1300.0, txCount = 48, netProfit = 980.75,
                dailySales = listOf(DaySalesEntry("01/01", 300.0, 200.0), DaySalesEntry("02/01", 120.0, 120.0), DaySalesEntry("03/01", 420.0, 260.0)),
                paymentShares = listOf(PaymentShare("نقد", 70.0), PaymentShare("دين", 30.0)),
                salesProfitLast7Days = listOf(SalesProfitDayEntry("01/01", 300.0, 60.0), SalesProfitDayEntry("02/01", 120.0, -15.0))
            )
        )
        return { ReportsScreen({}, viewModel = vm) }
    }

    private fun day(): @Composable () -> Unit {
        val vm = mockk<DayTransactionsViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(
            DayTxUiState(
                all = listOf(Transaction(91, 7, "نقدي", 100.0, isPaid = true, paymentMethodName = "نقد"), Transaction(92, 8, "آجل", 200.0, isPaid = false)),
                paymentMethods = listOf("نقد", "بطاقة"), totalAmount = 300.0, paidAmount = 100.0, unpaidAmount = 200.0, isLoading = false
            )
        )
        return { DayTransactionsScreen(1767225600000, {}, {}, vm) }
    }

    private fun components(): @Composable () -> Unit = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernSearchBar(query = "", onQueryChange = {}, placeholder = "بحث بالاسم أو رقم الهاتف...")
                StatCard(label = "مبيعات اليوم", value = 1250.5, icon = Icons.Rounded.Payments, color = MaterialTheme.colorScheme.primary)
                StatusChip(isPaid = true); StatusChip(isPaid = false)
                EmptyState(icon = Icons.Rounded.Inventory2, title = "لا توجد منتجات", subtitle = "أضف أول منتج للبدء")
            }
        }
    }

    // ── phone (360x800dp), Arabic ──────────────────────────────────────────
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun homePhoneLight() = capture("home-phone-light", content = home())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun homePhoneDark() = capture("home-phone-dark", dark = true, content = home())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun homePhoneLargeFont() = capture("home-phone-font130", fontScale = 1.3f, content = home())
    @Config(qualifiers = "ar-w840dp-h1200dp") @Test fun homeTablet() = capture("home-tablet-light", content = home())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun reportsPhoneLight() = capture("reports-phone-light", content = reports())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun reportsPhoneDark() = capture("reports-phone-dark", dark = true, content = reports())
    @Config(qualifiers = "ar-w840dp-h1200dp") @Test fun reportsTablet() = capture("reports-tablet-light", content = reports())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun dayPhoneLight() = capture("day-phone-light", content = day())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun dayPhoneLargeFont() = capture("day-phone-font130", fontScale = 1.3f, content = day())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun componentsPhoneLight() = capture("components-phone-light", content = components())
    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun componentsPhoneDark() = capture("components-phone-dark", dark = true, content = components())
}
