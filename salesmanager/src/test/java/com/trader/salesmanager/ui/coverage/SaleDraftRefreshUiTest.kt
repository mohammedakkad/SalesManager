package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.salesmanager.ui.transactions.addedit.*
import com.trader.salesmanager.ui.inventory.invoice.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Sale draft state is observable; recomposition must not save, navigate or substitute old values. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class SaleDraftRefreshUiTest:AppThemeUiFixture(){
    private val vm=mockk<AddEditTransactionViewModel>(relaxed=true)
    private val customer=Customer(7,"زبون المسودة")
    private val state=MutableStateFlow(AddEditTransactionUiState(selectedCustomer=customer,customers=listOf(customer),amount="100",note="ملاحظة محفوظة"))
    @Before fun model(){
        every{vm.uiState} returns state
        val repo=mockk<PaymentMethodRepository>(relaxed=true)
        loadKoinModules(module{single<PaymentMethodRepository>{repo}})
    }
    private fun reveal(text:String){ui.onNode(hasScrollAction()).performScrollToNode(hasText(text))}
    @Test fun invoiceEditorReceivesTheUpdatedCustomerAndCurrentSerializedDraftAfterThemeChange(){
        every{vm.serializePendingLines()} returns "current-lines"
        var name="";var lines:String?=null
        showApp{AddEditTransactionScreen(null,7,{}, {n,l->name=n;lines=l},vm)};dark(true)
        ui.runOnIdle{state.value=state.value.copy(selectedCustomer=Customer(9,"زبون بديل"))}
        dark(false);ui.onNodeWithText("إضافة أصناف من المخزن (اختياري)").performClick()
        assertEquals("زبون بديل",name);assertEquals("current-lines",lines)
        verify(exactly=0){vm.save()}
    }
    @Test fun pendingInvoiceRestorationKeepsSubmissionBlockedAcrossThemeAndRecoversExplicitly(){
        state.value=state.value.copy(isRestoringLines=true)
        showApp{AddEditTransactionScreen(null,7,{},viewModel=vm)}
        reveal("جارٍ تجهيز الأصناف");dark(true)
        ui.onNodeWithText("جارٍ تجهيز الأصناف").assertIsNotEnabled()
        ui.runOnIdle{state.value=state.value.copy(isRestoringLines=false,error="تعذر تجهيز سطر")}
        ui.onNodeWithText("تعذر تجهيز سطر").assertExists();dark(false)
        verify(exactly=0){vm.save()}
        ui.onNodeWithText("حفظ العملية").assertIsEnabled().performClick();verify(exactly=1){vm.save()}
    }
    @Test fun delayedSaveCompletionNavigatesOnceAndThemeChangesCannotRepeatIt(){
        var backs=0
        showApp{AddEditTransactionScreen(null,7,{backs++},viewModel=vm)}
        dark(true);ui.runOnIdle{state.value=state.value.copy(isLoading=true)}
        verify(exactly=0){vm.save()};assertEquals(0,backs)
        ui.runOnIdle{state.value=state.value.copy(isLoading=false,isSaved=true,savedTransactionId=41)}
        ui.runOnIdle{assertEquals(1,backs)};dark(false)
        ui.runOnIdle{state.value=state.value.copy(note="تحديث بعد الحفظ")}
        ui.runOnIdle{assertEquals(1,backs)}
    }
    @Test fun replacingInvoiceLinesReplacesTheirPreviewAndKeepsTheAmountLinkedToTheNewTotal(){
        val product=ProductWithUnits(Product(id="p",name="صنف قديم"),listOf(ProductUnit(id="u",productId="p",price=25.0,isDefault=true)))
        val line=InvoiceLineItem(product=product,selectedUnit=product.units.single(),displayQty=2.0)
        state.value=state.value.copy(hasItems=true,pendingLines=listOf(line),amount="50.0")
        showApp{AddEditTransactionScreen(null,7,{},viewModel=vm)}
        ui.onNodeWithText("صنف قديم × 2").assertExists();dark(true)
        val nextUnit=product.units.single().copy(id="v",productId="q",price=10.0)
        val nextProduct=ProductWithUnits(product.product.copy(id="q",name="صنف بديل"),listOf(nextUnit))
        val next=InvoiceLineItem(product=nextProduct,selectedUnit=nextUnit,displayQty=3.0,customPrice=10.0)
        ui.runOnIdle{state.value=state.value.copy(pendingLines=listOf(next),amount="30.0")}
        ui.onNodeWithText("صنف قديم × 2").assertDoesNotExist();ui.onNodeWithText("صنف بديل × 3").assertExists()
        dark(false)
        ui.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText) and hasText("المبلغ الإجمالي ₪ *")).assertTextContains("30.0")
        verify(exactly=0){vm.save()}
    }
    @Test fun paymentSelectionChangesAcrossThemeWithoutResettingTheAmountOrNote(){
        state.value=state.value.copy(paymentType=PaymentType.CASH,isPaid=true)
        every{vm.updatePaymentType(any())} answers{state.value=state.value.copy(paymentType=firstArg())}
        every{vm.updateIsPaid(any())} answers{state.value=state.value.copy(isPaid=firstArg())}
        showApp{AddEditTransactionScreen(null,7,{},viewModel=vm)}
        reveal("دين");ui.onNodeWithText("دين").performClick();dark(true)
        ui.onNodeWithText("دين").assertIsSelected()
        reveal("غير مدفوع");ui.onNodeWithText("غير مدفوع").performClick();dark(false)
        ui.onNodeWithText("غير مدفوع").assertIsSelected()
        ui.runOnIdle{assertEquals("100",state.value.amount);assertEquals("ملاحظة محفوظة",state.value.note)}
        verify(exactly=1){vm.updatePaymentType(PaymentType.DEBT)};verify(exactly=1){vm.updateIsPaid(false)};verify(exactly=0){vm.save()}
    }
}
