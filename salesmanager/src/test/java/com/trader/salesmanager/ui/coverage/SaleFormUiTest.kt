package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.salesmanager.ui.transactions.addedit.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

@RunWith(RobolectricTestRunner::class)
class SaleFormUiTest:OfflineSellerUiTest() {
    private val vm=mockk<AddEditTransactionViewModel>(relaxed=true)
    private val state=MutableStateFlow(AddEditTransactionUiState(customers=listOf(Customer(7,"زبون معروف")),amount="125.0",selectedCustomer=Customer(7,"زبون معروف")))
    @Before fun fixture() {
        every { vm.uiState } returns state
        val payment=mockk<PaymentMethodRepository>(relaxed=true)
        loadKoinModules(module { single<PaymentMethodRepository> { payment } })
    }
    @Test fun amountNoteAndDebtSelectionReachApplicationModelBeforeSave() {
        show { AddEditTransactionScreen(null,7,{},viewModel=vm) }
        verify { vm.loadTransaction(null);vm.preselect(7) }
        ui.onNodeWithText("المبلغ الإجمالي ₪ *").performTextReplacement("250")
        verify { vm.updateAmount("250") }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("دين"));ui.onNodeWithText("دين").performClick()
        verify { vm.updatePaymentType(PaymentType.DEBT) }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("غير مدفوع"));ui.onNodeWithText("غير مدفوع").performClick();verify { vm.updateIsPaid(false) }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("ملاحظة (اختياري)"))
        ui.onNodeWithText("ملاحظة (اختياري)").performTextInput("مراجعة")
        verify { vm.updateNote("مراجعة") }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("حفظ العملية"));ui.onNodeWithText("حفظ العملية").performClick();verify { vm.save() }
    }
    @Test fun invoiceEditorReceivesSelectedCustomerAndExistingLines() {
        every { vm.serializePendingLines() } returns "[]"
        var name="";var lines:String?=null
        show { AddEditTransactionScreen(41,7,{}, {n,l -> name=n;lines=l},vm) }
        ui.onNodeWithText("إضافة أصناف من المخزن (اختياري)").performClick()
        assertEquals("زبون معروف",name);assertEquals("[]",lines)
    }
}
