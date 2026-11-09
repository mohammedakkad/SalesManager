package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class ReportsUiFlowTest:OfflineSellerUiTest() {
    private val vm=mockk<ReportsViewModel>(relaxed=true)
    private val state=MutableStateFlow(ReportsUiState())
    @Before fun reportData() { every { vm.uiState } returns state }
    @Test fun pendingMonthlyTotalsDisablePdfUntilReady() {
        state.value=ReportsUiState(isLoading=false)
        show { ReportsScreen({},viewModel=vm) }
        ui.onNodeWithText("التقارير").assertExists()
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled()
        ui.onNodeWithText("طباعة").assertIsNotEnabled()
    }
    @Test fun financialSectionsPresentAllSalesProfitAndDebtAnalytics() {
        state.value=ReportsUiState(isLoading=false,totalAmount=300.0,paidAmount=200.0,unpaidAmount=100.0,txCount=3,
            monthlyReportTotals=FinancialReportTotals(300.0,75.0),
            debtAging=DebtAging(lessThanWeekAmount=100.0,lessThanWeekCount=1),
            netProfit=75.0,inventoryCostValue=250.0,inventorySaleValue=400.0,
            dailySales=listOf(DaySalesEntry("01/01",100.0,50.0),DaySalesEntry("02/01",200.0,150.0)),
            paymentShares=listOf(PaymentShare("نقد",200.0),PaymentShare("دين",100.0)),
            topSpenders=listOf(CustomerRank("زبون نشط",300.0)),topDebtors=listOf(CustomerRank("زبون مدين",100.0)),
            salesProfitLast7Days=listOf(SalesProfitDayEntry("01/01",100.0,25.0),SalesProfitDayEntry("02/01",200.0,50.0)),
            dayTotals=mapOf(1 to 100.0,2 to 200.0),monthlyReportMonth=0,monthlyReportYear=2026,
            calendarMonth=0,calendarYear=2026,isMonthlyReportLoading=false)
        show { ReportsScreen({},viewModel=vm) }
        ui.onNodeWithText("مشاركة PDF").assertIsEnabled()
        for(title in listOf("إجمالي المبيعات","المبيعات مقابل الربح - آخر 7 أيام","منحنى المبيعات اليومية","مدفوع مقابل غير مدفوع","توزيع طرق الدفع")) {
            ui.onNode(hasScrollAction()).performScrollToNode(hasText(title))
            ui.onNodeWithText(title).assertExists()
        }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("زبون نشط"))
        ui.onNodeWithText("زبون نشط").assertExists()
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("زبون مدين"))
        ui.onNodeWithText("زبون مدين").assertExists()
    }
}
