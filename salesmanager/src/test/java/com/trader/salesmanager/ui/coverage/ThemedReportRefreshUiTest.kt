package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.TransactionRepository
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedReportRefreshUiTest:AppThemeUiFixture() {
    private val vm=mockk<ReportsViewModel>(relaxed=true)
    private val state=MutableStateFlow(ReportsUiState(isLoading=false,calendarYear=2026,calendarMonth=0,
        monthlyReportYear=2026,monthlyReportMonth=0,isMonthlyReportLoading=false,monthlyReportTotals=FinancialReportTotals(300.0,50.0)))
    @Before fun data(){every{vm.uiState} returns state}
    private fun reveal(text:String){ui.onNode(hasScrollAction()).performScrollToNode(hasText(text))}
    private fun chart(prefix:String,value:String){
        val selector=hasContentDescription(prefix,substring=true)
        ui.onNode(hasScrollAction()).performScrollToNode(selector)
        ui.onNode(selector).assert(hasContentDescription(value,substring=true))
    }
    @Test fun financialRefreshReplacesRankingsAndUpdatesAllAccessibleSeriesAfterThemeChange(){
        state.value=state.value.copy(topSpenders=listOf(CustomerRank("زبون قديم",100.0)),topDebtors=listOf(CustomerRank("مدين قديم",50.0)))
        showApp{ReportsScreen({},viewModel=vm)}
        reveal("زبون قديم");ui.onNodeWithText("زبون قديم").assertExists();dark(true)
        ui.runOnIdle{state.value=state.value.copy(totalAmount=200.75,paidAmount=75.25,unpaidAmount=125.5,txCount=2,
            netProfit=35.0,inventoryCostValue=500.0,inventorySaleValue=800.0,
            dailySales=listOf(DaySalesEntry("01/01",75.25,25.0),DaySalesEntry("02/01",125.5,50.25)),
            paymentShares=listOf(PaymentShare("نقد جديد",75.25),PaymentShare("دين جديد",125.5)),
            topSpenders=listOf(CustomerRank("زبون محدّث",200.75)),topDebtors=listOf(CustomerRank("مدين محدّث",125.5)),
            salesProfitLast7Days=listOf(SalesProfitDayEntry("01/01",75.25,-25.0),SalesProfitDayEntry("02/01",125.5,60.0)))}
        chart("المبيعات اليومية:","125.50")
        chart("المدفوع وغير المدفوع:","50.25")
        chart("المبيعات والربح:","-25.00")
        chart("طرق الدفع:","نقد جديد")
        reveal("زبون محدّث");ui.onNodeWithText("زبون محدّث").assertExists();ui.onNodeWithText("زبون قديم").assertDoesNotExist()
        reveal("مدين محدّث");ui.onNodeWithText("مدين محدّث").assertExists();ui.onNodeWithText("مدين قديم").assertDoesNotExist()
        dark(false);ui.onNodeWithText("مدين محدّث").assertExists()
        verify(exactly=0){vm.setPeriod(any())}
    }
    @Test fun monthlyExportReadinessFollowsTheDisplayedMonthAcrossThemeAndDelayedTotals(){
        showApp{ReportsScreen({},viewModel=vm)}
        ui.onNodeWithText("مشاركة PDF").assertIsEnabled();ui.onNodeWithText("طباعة").assertIsEnabled()
        dark(true)
        ui.runOnIdle{state.value=state.value.copy(calendarMonth=1,isMonthlyReportLoading=true)}
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled();ui.onNodeWithText("طباعة").assertIsNotEnabled()
        ui.runOnIdle{state.value=state.value.copy(isMonthlyReportLoading=false)}
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled()
        ui.runOnIdle{state.value=state.value.copy(monthlyReportMonth=1,monthlyReportTotals=FinancialReportTotals(500.0,75.0))}
        ui.onNodeWithText("مشاركة PDF").assertIsEnabled();ui.onNodeWithText("طباعة").assertIsEnabled()
        dark(false)
        ui.runOnIdle{state.value=state.value.copy(monthlyReportYear=2025)}
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled();ui.onNodeWithText("طباعة").assertIsNotEnabled()
    }
    @Test fun changingSelectedDayReplacesRowsAndNavigatesToTheCurrentDaysMidnight(){
        val old=(1L..6L).map{Transaction(it,1,"يوم قديم $it",it*10.0,isPaid=it%2==0L)}
        state.value=state.value.copy(selectedDay=3,selectedDayTransactions=old,selectedDaySummary=Triple(210.0,120.0,90.0))
        var selected:Long?=null
        showApp{ReportsScreen({},onViewDayTransactions={selected=it},viewModel=vm)}
        reveal("+ 1 عمليات أخرى");dark(true)
        ui.runOnIdle{state.value=state.value.copy(selectedDay=1,selectedDayTransactions=listOf(Transaction(90,1,"يوم جديد",75.0,isPaid=false),Transaction(91,1,"ثان جديد",25.0,isPaid=true)),selectedDaySummary=Triple(100.0,25.0,75.0))}
        reveal("يوم جديد");ui.onNodeWithText("يوم جديد").assertExists();ui.onNodeWithText("يوم قديم 1").assertDoesNotExist()
        reveal("عرض كل العمليات (2)");dark(false)
        assertNull(selected)
        ui.onNodeWithText("عرض كل العمليات (2)").performClick()
        assertEquals(Calendar.getInstance().apply{clear();set(2026,0,1)}.timeInMillis,selected)
    }
    @Test fun daySearchKeepsItsQueryWhileLiveDataChangesWhichTransactionMatches(){
        val old=Transaction(91,7,"نتيجة بحث",100.0,isPaid=true)
        val other=Transaction(92,8,"خارج البحث",200.0,isPaid=false)
        val rows=MutableStateFlow(listOf(old,other));val repo=mockk<TransactionRepository>(relaxed=true)
        every{repo.getTransactionsByDate(any(),any())} returns rows
        val actual=owned(DayTransactionsViewModel(repo,1700000000000));var selected=0L
        showApp{DayTransactionsScreen(1700000000000,{}, {selected=it},actual)}
        ui.onNode(hasSetTextAction()).performTextInput("نتيجة")
        ui.onNodeWithText("نتيجة بحث").assertExists();ui.onNodeWithText("خارج البحث").assertDoesNotExist();dark(true)
        ui.runOnIdle{rows.value=listOf(old.copy(customerName="أصبح خارجاً"),other.copy(customerName="نتيجة جديدة"))}
        ui.onNodeWithText("نتيجة بحث").assertDoesNotExist();ui.onNodeWithText("نتيجة جديدة").assertExists()
        dark(false);ui.onNode(hasSetTextAction()).assertTextContains("نتيجة")
        ui.onNodeWithText("نتيجة جديدة").performClick();assertEquals(92L,selected)
        ui.runOnIdle{assertEquals(300.0,actual.uiState.value.totalAmount,0.0)}
    }
}
