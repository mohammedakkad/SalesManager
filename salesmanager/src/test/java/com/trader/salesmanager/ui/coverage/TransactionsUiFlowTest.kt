package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.sales.SaleCommands
import com.trader.salesmanager.ui.transactions.list.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionsUiFlowTest:OfflineSellerUiTest() {
    @Test fun paidFilterAndRowNavigationPreserveTransactionIdentity() {
        val repo=mockk<TransactionRepository>(relaxed=true)
        val paid=Transaction(id=1,customerId=1,customerName="مدفوع الاختبار",amount=50.0,isPaid=true)
        val unpaid=Transaction(id=2,customerId=2,customerName="دين الاختبار",amount=75.0,isPaid=false,syncStatus=SyncStatus.PENDING)
        every { repo.getAllTransactions() } returns MutableStateFlow(listOf(paid,unpaid))
        every { repo.getUnpaidTransactions() } returns MutableStateFlow(listOf(unpaid))
        val vm=owned(TransactionsViewModel(repo,mockk<SaleCommands>(),mockk<ActivationRepository>()));var selected=0L
        show { TransactionsScreen({}, {selected=it},{},viewModel=vm) }
        ui.onNode(hasText("مدفوع") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Checkbox)).performClick()
        ui.onNodeWithText("مدفوع الاختبار").assertExists();ui.onNodeWithText("دين الاختبار").assertDoesNotExist()
        ui.onNode(hasText("غير مدفوع") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Checkbox)).performClick()
        ui.onNodeWithText("دين الاختبار").performClick();Assert.assertEquals(2L,selected)
    }
    @Test fun emptyTransactionsRemainVisibleWithoutInventingARecord() {
        val repo=mockk<TransactionRepository>(relaxed=true)
        every { repo.getAllTransactions() } returns MutableStateFlow(emptyList())
        val vm=owned(TransactionsViewModel(repo,mockk<SaleCommands>(),mockk<ActivationRepository>()))
        show { TransactionsScreen({}, {},{},viewModel=vm) }
        ui.onNodeWithText("لا توجد عمليات").assertExists()
    }
}
