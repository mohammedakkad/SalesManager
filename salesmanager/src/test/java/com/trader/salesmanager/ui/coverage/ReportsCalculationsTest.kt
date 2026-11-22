package com.trader.salesmanager.ui.coverage

import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsCalculationsTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private val reports=mockk<ReportsRepository>()
    private val tx=mockk<TransactionRepository>()
    private lateinit var vm:ReportsViewModel
    private lateinit var originalZone:TimeZone
    private val rows=MutableStateFlow<List<Transaction>>(emptyList())
    private fun time(hour:Int)=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,hour);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis
    @Before fun setup() {
        originalZone=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Dispatchers.setMain(dispatcher)
        rows.value=listOf(Transaction(1,1,"نقد",100.0,isPaid=true,date=time(9),paymentMethodName="نقد"),
            Transaction(2,2,"دين",50.0,isPaid=false,date=time(15)),Transaction(3,1,"نقد",150.0,isPaid=true,date=time(20),paymentMethodName="نقد"))
        every { tx.getAllTransactions() } returns rows
        every { reports.observeFinancialReportTotals(any(),any()) } answers {
            val start=firstArg<Long>();val end=secondArg<Long>()
            rows.map { list -> val sum=list.filter { it.date in start..end }.sumOf { it.amount };FinancialReportTotals(sum,sum/4) }
        }
        every { reports.observeDailySalesProfit(any(),any(),any()) } returns flowOf(emptyList())
        every { reports.observeInventoryValue() } returns flowOf(InventoryValue(200.0,300.0))
        every { reports.observeDebtAging(any(),any(),any()) } returns flowOf(DebtAging(lessThanWeekAmount=50.0,lessThanWeekCount=1))
        vm=ReportsViewModel(tx,mockk<CustomerRepository>(),reports);store.put("vm",vm)
    }
    @After fun close() { store.clear();Dispatchers.resetMain();TimeZone.setDefault(originalZone) }
    @Test fun paidDebtProfitAndCustomerRanksRemainConsistent()=runTest(dispatcher) {
        advanceUntilIdle();val s=vm.uiState.value
        assertEquals(300.0,s.totalAmount,0.0);assertEquals(250.0,s.paidAmount,0.0);assertEquals(50.0,s.unpaidAmount,0.0)
        assertEquals(75.0,s.netProfit,0.0);assertEquals("نقد",s.topSpenders.first().name)
        assertEquals(50.0,s.topDebtors.single().amount,0.0);assertEquals(7,s.salesProfitLast7Days.size)
        assertEquals(50.0,s.debtAging.totalAmount,0.0)
    }
    @Test fun selectingDayBuildsCorrectPaidAndUnpaidBreakdownAndCanDeselect()=runTest(dispatcher) {
        advanceUntilIdle();val day=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        vm.selectDay(day);advanceUntilIdle();assertEquals(Triple(300.0,250.0,50.0),vm.uiState.value.selectedDaySummary)
        assertEquals(3,vm.uiState.value.selectedDayTransactions.size)
        vm.selectDay(day);advanceUntilIdle();assertNull(vm.uiState.value.selectedDay)
        assertTrue(vm.uiState.value.selectedDayTransactions.isEmpty())
    }
    @Test fun changingMonthClearsSelectionAndProducesReadyMonthlyExport()=runTest(dispatcher) {
        advanceUntilIdle();vm.selectDay(1);vm.prevMonth();advanceUntilIdle()
        assertNull(vm.uiState.value.selectedDay);assertEquals(0.0,vm.uiState.value.monthlyReportTotals.totalSales,0.0)
        vm.nextMonth();advanceUntilIdle();val report=vm.buildMonthlyReportData("متجر")
        assertEquals("متجر",report.storeName);assertEquals(300.0,report.totalSales,0.0);assertEquals(75.0,report.netProfit,0.0)
    }
    @Test fun periodChangesAndLiveRowsRecomputeTotalsWithoutLosingAnalytics()=runTest(dispatcher) {
        advanceUntilIdle()
        for(period in ReportPeriod.values()) { vm.setPeriod(period);advanceUntilIdle();assertEquals(period,vm.uiState.value.period) }
        rows.value=rows.value+Transaction(4,3,"جديد",25.0,isPaid=false,date=time(10))
        advanceUntilIdle();assertEquals(325.0,vm.uiState.value.totalAmount,0.0);assertEquals(75.0,vm.uiState.value.unpaidAmount,0.0)
        assertEquals(200.0,vm.uiState.value.inventoryCostValue,0.0)
    }
}
