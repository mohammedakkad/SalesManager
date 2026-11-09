package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayReportUiTest:OfflineSellerUiTest() {
    private val vm=mockk<DayTransactionsViewModel>(relaxed=true)
    private val state=MutableStateFlow(DayTxUiState(all=listOf(Transaction(91,7,"نقدي",100.0,isPaid=true,paymentMethodName="نقد"),Transaction(92,8,"آجل",200.0,isPaid=false)),paymentMethods=listOf("نقد","بطاقة"),totalAmount=300.0,paidAmount=100.0,unpaidAmount=200.0,isLoading=false))
    @Before fun fixture() { every { vm.uiState } returns state }
    @Test fun dayCardsUseTransactionIdentityAndSearchIsDelegated() {
        var selected=0L;show { DayTransactionsScreen(1700000000000,{}, {selected=it},vm) }
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("آجل"));ui.onNodeWithText("آجل").performClick();assertEquals(92,selected)
        ui.onNode(hasSetTextAction()).performTextInput("نقدي");verify { vm.setQuery("نقدي") }
    }
    @Test fun activeFiltersCanBeClearedWithoutChangingDayTotals() {
        val repository=mockk<TransactionRepository>(relaxed=true)
        every { repository.getTransactionsByDate(any(),any()) } returns MutableStateFlow(state.value.all)
        val actual=owned(DayTransactionsViewModel(repository,1700000000000))
        show { DayTransactionsScreen(1700000000000,{},{},actual) }
        ui.onAllNodes(hasClickAction())[1].performClick()
        ui.onNodeWithText("الفلاتر").assertExists()
        val chip=SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Checkbox)
        ui.onNode(hasText("مدفوع") and chip).performClick()
        ui.onNode(hasText("نقد") and chip).performClick()
        ui.onNodeWithText("تطبيق").performClick()
        ui.onNodeWithText("نقدي").assertExists();ui.onNodeWithText("آجل").assertDoesNotExist()
        ui.onAllNodes(hasClickAction())[1].performClick()
        ui.onNodeWithText("مسح الكل").performClick();ui.onNodeWithText("تطبيق").performClick()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("آجل"))
        ui.onNodeWithText("آجل").assertExists()
        ui.runOnIdle { assertEquals(300.0,actual.uiState.value.totalAmount,0.0);assertNull(actual.uiState.value.filterPaid);assertEquals("",actual.uiState.value.filterPaymentMethod) }
    }
}
