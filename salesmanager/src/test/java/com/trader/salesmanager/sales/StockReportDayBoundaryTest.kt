package com.trader.salesmanager.sales

import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.inventory.reports.StockReportsViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class StockReportDayBoundaryTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun todayIncludesLastMillisecondAndExcludesAdjacentDays() = runTest {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
        }.timeInMillis
        val next = Calendar.getInstance().apply { timeInMillis=start; add(Calendar.DAY_OF_MONTH,1) }.timeInMillis
        val products = mockk<ProductRepository>()
        val transactions = mockk<TransactionRepository>()
        every { products.getAllProducts() } returns flowOf(emptyList())
        every { transactions.getAllTransactions() } returns flowOf(listOf(
            Transaction(id=1,customerId=1,amount=1.0,isPaid=true,paymentType=PaymentType.CASH,date=start),
            Transaction(id=2,customerId=1,amount=2.0,isPaid=true,paymentType=PaymentType.CASH,date=next-1),
            Transaction(id=3,customerId=1,amount=100.0,isPaid=true,paymentType=PaymentType.CASH,date=start-1),
            Transaction(id=4,customerId=1,amount=200.0,isPaid=true,paymentType=PaymentType.CASH,date=next),
            Transaction(id=5,customerId=1,amount=400.0,isPaid=false,paymentType=PaymentType.CASH,date=start),
            Transaction(id=6,customerId=1,amount=800.0,isPaid=false,paymentType=PaymentType.DEBT,date=start)))
        val vm = StockReportsViewModel(products,transactions)
        val subscription = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        try {
            runCurrent()
            assertFalse(vm.uiState.value.isLoading)
            assertEquals(3.0,vm.uiState.value.cashSummary.todayCash,0.0)
            assertEquals(303.0,vm.uiState.value.cashSummary.totalCash,0.0)
            assertEquals(800.0,vm.uiState.value.cashSummary.totalDebt,0.0)
        } finally { subscription.cancel(); vm.viewModelScope.cancel() }
    }
}
