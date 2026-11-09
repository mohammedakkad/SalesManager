package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.transactions.details.*
import com.trader.salesmanager.ui.customers.details.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DetailUiFlowTest:OfflineSellerUiTest() {
    @Test fun partiallyReturnedInvoiceKeepsNetItemsAndCustomerDebtVisible() {
        val vm=mockk<TransactionDetailsViewModel>(relaxed=true)
        val tx=Transaction(42,1,"عميل الفاتورة",150.0,originalAmount=200.0,isPaid=false,hasItems=true,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED)
        val state=MutableStateFlow(TransactionDetailsUiState(transaction=tx,customer=Customer(1,"عميل الفاتورة"),
            invoiceItems=listOf(InvoiceItem("line",42,"p","صنف التفاصيل","u","حبة",4.0,50.0,200.0)),
            isItemsLoaded=true,priorDebtBalance=50.0,currentDebtBalance=200.0,
            returnSummary=ReturnSummary(TransactionReturnStatus.PARTIALLY_RETURNED,50.0,mapOf("u" to 1.0))))
        every { vm.uiState } returns state
        show { TransactionDetailsScreen(42,{}, {},{},viewModel=vm) }
        ui.onAllNodesWithText("عميل الفاتورة",substring=true).onFirst().assertExists()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("صنف التفاصيل"))
        ui.onNodeWithText("صنف التفاصيل").assertExists()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("مشاركة PDF"))
        ui.onNodeWithText("مشاركة PDF").assertIsEnabled()
        verify { vm.refreshReturnSummary() }
    }
    @Test fun deletionNavigatesBackOnlyAfterStateConfirmsDeletion() {
        val vm=mockk<TransactionDetailsViewModel>(relaxed=true)
        val state=MutableStateFlow(TransactionDetailsUiState(transaction=Transaction(1,1,amount=25.0,isPaid=true),isItemsLoaded=true))
        every { vm.uiState } returns state;var backs=0
        show { TransactionDetailsScreen(1,{backs++}, {},{},viewModel=vm) }
        Assert.assertEquals(0,backs)
        ui.runOnIdle { state.value=state.value.copy(isDeleted=true) }
        ui.runOnIdle { Assert.assertEquals(1,backs) }
    }
    @Test fun customerDetailsShowsDerivedDebtAndRealTransactionNavigation() {
        val customers=mockk<CustomerRepository>();val transactions=mockk<TransactionRepository>()
        coEvery { customers.getCustomerById(7) } returns Customer(7,"زبون التفاصيل","0599123456")
        every { transactions.getTransactionsByCustomer(7) } returns MutableStateFlow(listOf(Transaction(81,7,"زبون التفاصيل",75.0,isPaid=false,paymentMethodName="طريقة اختبار")))
        coEvery { transactions.getUnpaidAmountByCustomer(7) } returns 75.0
        val vm=owned(CustomerDetailsViewModel(customers,transactions,7));var selected=0L
        show { CustomerDetailsScreen(7,{}, {},{}, {selected=it},viewModel=vm) }
        ui.onAllNodesWithText("زبون التفاصيل",substring=true).onFirst().assertExists()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("طريقة اختبار",substring=true))
        ui.onNodeWithText("طريقة اختبار",substring=true).performClick()
        Assert.assertEquals(81L,selected);Assert.assertEquals(75.0,vm.uiState.value.totalDebt,0.0)
    }
}
