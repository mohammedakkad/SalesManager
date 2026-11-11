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
    @Test fun calendarMonthActionsAreAccessibleAndInvokeTheirActualCallbacks() {
        state.value=ReportsUiState(isLoading=false,calendarMonth=0,calendarYear=2026)
        show { ReportsScreen({},viewModel=vm) }
        ui.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("الشهر السابق"))
        ui.onNodeWithContentDescription("الشهر السابق").performClick();verify(exactly=1){vm.prevMonth()}
        ui.onNodeWithContentDescription("الشهر التالي").performClick();verify(exactly=1){vm.nextMonth()}
    }

    @Test fun selectedEmptyDayExplainsThatNoTransactionsExist() {
        state.value=ReportsUiState(isLoading=false,calendarMonth=0,calendarYear=2026,selectedDay=3)
        show { ReportsScreen({},viewModel=vm) }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("لا توجد عمليات في هذا اليوم"))
        ui.onNodeWithText("لا توجد عمليات في هذا اليوم").assertIsDisplayed()
    }
    @Test fun selectedBusyDayShowsPaidAndDebtRowsAndNavigatesToItsMidnight() {
        val rows=(1L..7L).map{Transaction(it,1,if(it==1L)"" else "DAY-$it",it*10.0,isPaid=it%2==0L)}
        state.value=ReportsUiState(isLoading=false,calendarMonth=0,calendarYear=2026,selectedDay=3,
            selectedDayTransactions=rows,selectedDaySummary=Triple(280.0,120.0,160.0))
        var selected:Long?=null
        show { ReportsScreen({},onViewDayTransactions={selected=it},viewModel=vm) }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("+ 2 عمليات أخرى"))
        ui.onNodeWithText("+ 2 عمليات أخرى").assertExists()
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("عرض كل العمليات (7)"))
        ui.onNodeWithText("عرض كل العمليات (7)").performScrollTo().assertIsDisplayed().performClick()
        val expected=java.util.Calendar.getInstance().apply{clear();set(2026,0,3)}.timeInMillis
        Assert.assertEquals(expected,selected)
    }
    @Test fun periodButtonsDispatchTheirOwnPeriodAndDoNotChangeTheCalendarDirectly() {
        state.value=ReportsUiState(isLoading=false)
        show { ReportsScreen({},viewModel=vm) }
        for((text,period) in listOf("اليوم" to ReportPeriod.TODAY,"الأسبوع" to ReportPeriod.WEEK,"الشهر" to ReportPeriod.MONTH)){
            ui.onNodeWithText(text).performClick();verify(exactly=1){vm.setPeriod(period)}
        }
        verify(exactly=0){vm.prevMonth()};verify(exactly=0){vm.nextMonth()}
    }
    @Test fun monthReadinessMismatchKeepsBothFinancialExportActionsDisabled() {
        state.value=ReportsUiState(isLoading=false,calendarMonth=0,calendarYear=2026,
            monthlyReportMonth=11,monthlyReportYear=2025,isMonthlyReportLoading=false)
        show { ReportsScreen({},viewModel=vm) }
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled();ui.onNodeWithText("طباعة").assertIsNotEnabled()
    }

}
