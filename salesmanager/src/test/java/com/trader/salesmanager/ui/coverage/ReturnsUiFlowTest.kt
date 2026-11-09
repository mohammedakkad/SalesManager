package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.returns.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReturnsUiFlowTest:OfflineSellerUiTest() {
    private val vm=mockk<ReturnViewModel>(relaxed=true)
    private val state=MutableStateFlow(ReturnScreenState(isLoading=false))
    @Before fun data() { every { vm.state } returns state }
    @Test fun partialReturnShowsRemainingQuantityAndCorrectRefund() {
        val item=InvoiceItem(id="line",transactionId=1,productId="p",productName="صنف مرتجع",unitId="u",quantity=4.0,pricePerUnit=25.0,totalPrice=100.0)
        state.value=ReturnScreenState(isLoading=false,lines=listOf(ReturnLineState(item,"صنف مرتجع","حبة",costPrice=15.0,alreadyReturned=1.0,returnQty=2.0,isSelected=true)))
        every { vm.showConfirmSheet() } answers { state.value=state.value.copy(showConfirmSheet=true) }
        show { ReturnProcessScreen(1,{}, {},viewModel=vm) }
        ui.onNodeWithText("صنف مرتجع").assertExists()
        ui.onNodeWithText("تأكيد الإرجاع · ₪50.00").performClick()
        ui.onNodeWithText("إجمالي الاسترداد").assertExists()
        Assert.assertEquals(50.0,state.value.totalRefund,0.0);Assert.assertEquals(20.0,state.value.totalLostProfit,0.0)
    }
    @Test fun alreadyReturnedLineCannotOfferAnotherRefund() {
        val item=InvoiceItem(id="line",transactionId=1,quantity=2.0,pricePerUnit=25.0)
        state.value=ReturnScreenState(isLoading=false,lines=listOf(ReturnLineState(item,"مرتجع كامل","حبة",alreadyReturned=2.0)))
        show { ReturnProcessScreen(1,{}, {},viewModel=vm) }
        ui.onNodeWithText("تم إرجاع كامل الكمية مسبقاً").assertExists()
        Assert.assertFalse(state.value.canConfirm)
    }
}
