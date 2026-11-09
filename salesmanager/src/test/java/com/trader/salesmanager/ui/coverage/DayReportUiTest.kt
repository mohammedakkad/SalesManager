package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
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
        state.value=state.value.copy(filterPaid=true,filterPaymentMethod="نقد")
        show { DayTransactionsScreen(1700000000000,{},{},vm) }
        // The only header icon after back opens the filter sheet.
        ui.onAllNodes(hasClickAction()).onLast().assertExists()
        ui.onNodeWithText("نقدي").assertExists();ui.onNodeWithText("آجل").assertDoesNotExist()
        ui.runOnIdle { state.value=state.value.copy(filterPaid=false,filterPaymentMethod="") }
        ui.onNodeWithText("آجل").assertExists();ui.onNodeWithText("نقدي").assertDoesNotExist()
    }
}
