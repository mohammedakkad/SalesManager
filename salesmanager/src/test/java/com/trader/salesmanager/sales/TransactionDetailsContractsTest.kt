package com.trader.salesmanager.sales

import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailsContractsTest {
    private val transactions=mockk<TransactionRepository>()
    private val invoices=mockk<InvoiceItemRepository>()
    private val stock=mockk<StockRepository>()
    private val returns=mockk<ReturnRepository>()
    private val customers=mockk<CustomerRepository>()
    private val activation=mockk<ActivationRepository>()
    private val commands=mockk<SaleCommands>()
    private val sale=Transaction(id=7,customerId=1,amount=100.0,isPaid=false,hasItems=true,date=123)
    private val current=MutableStateFlow<Transaction?>(sale)
    private val lines=MutableStateFlow<List<InvoiceItem>>(emptyList())
    private val refunds=MutableStateFlow<List<ReturnInvoice>>(emptyList())
    private var summary=ReturnSummary.NONE
    private lateinit var vm:TransactionDetailsViewModel
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { transactions.observeTransactionById(7) } returns current
        every { invoices.getItemsForTransaction(7) } returns lines
        every { returns.getReturnsByTransaction(7) } returns refunds
        coEvery { returns.getReturnSummary(7,any()) } answers { summary }
        coEvery { customers.getCustomerById(1) } returns Customer(1,"fixture")
        coEvery { transactions.getUnpaidAmountBeforeTransaction(1,123,7) } returns 30.0
        coEvery { transactions.getUnpaidAmountByCustomer(1) } returns 130.0
        coEvery { activation.getMerchantCode() } returns "merchant"
        coEvery { commands.delete(any(),any(),any()) } returns 7
        vm=TransactionDetailsViewModel(7,transactions,invoices,stock,returns,customers,commands,activation)
    }
    @After fun close(){vm.viewModelScope.cancel();Dispatchers.resetMain()}
    private fun row(name:String)=InvoiceItem(id=name,productName=name,quantity=2.0,totalPrice=100.0)

    @Test fun liveTransactionRecomputesDerivedBalancesWithoutChangingDebtFormula()=runTest {
        runCurrent()
        assertEquals("fixture",vm.uiState.value.customer?.name)
        assertEquals(30.0,vm.uiState.value.priorDebtBalance!!,0.0)
        assertEquals(130.0,vm.uiState.value.currentDebtBalance!!,0.0)
        coEvery { transactions.getUnpaidAmountByCustomer(1) } returns 30.0
        current.value=sale.copy(isPaid=true);runCurrent()
        assertTrue(vm.uiState.value.transaction!!.isPaid)
        assertEquals(30.0,vm.uiState.value.currentDebtBalance!!,0.0)
        coVerify(exactly=2) { transactions.getUnpaidAmountBeforeTransaction(1,123,7) }
    }
    @Test fun walkInAndMissingTransactionsHaveNoCustomerDebtQueries()=runTest {
        runCurrent()
        clearMocks(customers,transactions,answers=false)
        current.value=sale.copy(customerId=0);runCurrent()
        assertNull(vm.uiState.value.customer)
        assertNull(vm.uiState.value.priorDebtBalance)
        assertNull(vm.uiState.value.currentDebtBalance)
        current.value=null;runCurrent()
        assertNull(vm.uiState.value.transaction)
        coVerify(exactly=0) { customers.getCustomerById(any());transactions.getUnpaidAmountByCustomer(any()) }
        vm.delete();runCurrent()
        coVerify(exactly=0) { commands.delete(any(),any(),any()) }
    }
    @Test fun invoiceSortAndTransientEmptyEmissionPreserveLoadedItemsUntilSaleHasNoItems()=runTest {
        lines.value=listOf(row("Z"),row("A"));runCurrent()
        assertTrue(vm.uiState.value.isItemsLoaded)
        assertEquals(listOf("A","Z"),vm.uiState.value.invoiceItems.map { it.productName })
        lines.value=emptyList();runCurrent()
        assertEquals(2,vm.uiState.value.invoiceItems.size)
        current.value=sale.copy(hasItems=false);lines.value=listOf(row("A"));runCurrent()
        lines.value=emptyList();runCurrent()
        assertTrue(vm.uiState.value.invoiceItems.isEmpty())
    }
    @Test fun refundStreamAndExplicitRefreshUseFreshRepositorySummary()=runTest {
        runCurrent();vm.refreshReturnSummary();runCurrent()
        coVerify(exactly=0) { returns.getReturnSummary(any(),any()) }
        lines.value=listOf(row("A"));runCurrent()
        summary=ReturnSummary(TransactionReturnStatus.PARTIALLY_RETURNED,25.0,mapOf("unit" to 1.0))
        refunds.value=listOf(ReturnInvoice(id="return",originalTransactionId=7,totalRefund=25.0));runCurrent()
        assertEquals(summary,vm.uiState.value.returnSummary)
        summary=ReturnSummary(TransactionReturnStatus.FULLY_RETURNED,100.0,mapOf("unit" to 2.0))
        vm.refreshReturnSummary();runCurrent()
        assertEquals(summary,vm.uiState.value.returnSummary)
        coVerify(exactly=3) { returns.getReturnSummary(7,any()) }
    }
    @Test fun deletionReportsSuccessOnlyAfterAggregateCommandCompletes()=runTest {
        val gate=CompletableDeferred<Long>()
        coEvery { commands.delete(any(),"merchant",sale) } coAnswers { gate.await() }
        runCurrent();vm.delete();runCurrent()
        assertFalse(vm.uiState.value.isDeleted)
        gate.complete(7);runCurrent()
        assertTrue(vm.uiState.value.isDeleted)
        coVerify(exactly=1) { commands.delete(match { java.util.UUID.fromString(it).toString()==it },"merchant",sale) }
        confirmVerified(stock)
    }
    @Test fun cancellationWhileDeletingDoesNotSetSuccessFlag()=runTest {
        val gate=CompletableDeferred<Long>()
        coEvery { commands.delete(any(),any(),any()) } coAnswers { gate.await() }
        runCurrent();vm.delete();runCurrent()
        vm.viewModelScope.cancel();gate.complete(7);runCurrent()
        assertFalse(vm.uiState.value.isDeleted)
    }
}
