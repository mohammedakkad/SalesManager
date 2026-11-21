package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.transactions.details.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class InvoiceReturnRefreshUiTest:AppThemeUiFixture(){
    private val vm=mockk<TransactionDetailsViewModel>(relaxed=true)
    private val tx=Transaction(41,7,"زبون فاتورة",200.0,originalAmount=200.0,hasItems=true,isPaid=false)
    private val line=InvoiceItem("line",41,"p","صنف فاتورة","u","حبة",4.0,50.0,200.0)
    private val state=MutableStateFlow(TransactionDetailsUiState(transaction=tx,invoiceItems=listOf(line),isItemsLoaded=true))
    @Before fun fixture(){every{vm.uiState} returns state}
    private fun reveal(text:String){ui.onNode(hasScrollAction()).performScrollToNode(hasText(text))}
    @Test fun partialThenFullReturnReplacesTheDisplayedQuantitiesRefundAndHeaderBadge(){
        showApp{TransactionDetailsScreen(41,{},{},{},vm)}
        reveal(line.productName);dark(true)
        ui.runOnIdle{state.value=state.value.copy(transaction=tx.copy(amount=150.0,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED),returnSummary=ReturnSummary(TransactionReturnStatus.PARTIALLY_RETURNED,50.0,mapOf("u" to 1.0)))}
        reveal("أُرجع 1");ui.onNodeWithText("3 حبة").assertExists()
        reveal("- ₪50.00");ui.onNodeWithText("- ₪50.00").assertExists()
        dark(false)
        ui.runOnIdle{state.value=state.value.copy(transaction=tx.copy(amount=0.0,returnStatus=TransactionReturnStatus.FULLY_RETURNED),returnSummary=ReturnSummary(TransactionReturnStatus.FULLY_RETURNED,200.0,mapOf("u" to 4.0)))}
        reveal("مرتجع كامل");ui.onNodeWithText("أُرجع 1").assertDoesNotExist();ui.onNodeWithText("3 حبة").assertDoesNotExist()
        reveal("- ₪200.00");ui.onNodeWithText("- ₪50.00").assertDoesNotExist()
        reveal("↩ مرتجع كامل");ui.onNodeWithText("↩ مرتجع كامل").assertExists();ui.onNodeWithText("↩ مرتجع جزئي").assertDoesNotExist()
    }
    @Test fun returnSummaryUsesUnitIdentityWhenTwoUnitsBelongToTheSameProduct(){
        val first=line.copy(id="a",unitId="a",unitLabel="حبة أ",quantity=2.0,totalPrice=100.0)
        val second=line.copy(id="b",unitId="b",unitLabel="حبة ب",quantity=2.0,totalPrice=100.0)
        state.value=state.value.copy(invoiceItems=listOf(first,second))
        showApp{TransactionDetailsScreen(41,{},{},{},vm)};dark(true)
        ui.runOnIdle{state.value=state.value.copy(transaction=tx.copy(amount=175.0,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED),returnSummary=ReturnSummary(TransactionReturnStatus.PARTIALLY_RETURNED,25.0,mapOf("b" to .5)))}
        reveal("1.5 حبة ب");ui.onNodeWithText("1.5 حبة ب").assertExists()
        ui.onNodeWithText("2 حبة أ  ×  ₪50.00").assertExists()
        ui.onNodeWithText("1.5 حبة أ").assertDoesNotExist()
        dark(false);verify(exactly=0){vm.delete()}
    }
    @Test fun exportControlsFollowDelayedItemReadinessAcrossThemeWithoutCreatingAFile(){
        state.value=state.value.copy(isItemsLoaded=false)
        showApp{TransactionDetailsScreen(41,{},{},{},vm)}
        reveal("مشاركة PDF");ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled();ui.onNodeWithText("طباعة").assertIsNotEnabled()
        dark(true);ui.runOnIdle{state.value=state.value.copy(isItemsLoaded=true)}
        ui.onNodeWithText("مشاركة PDF").assertIsEnabled();ui.onNodeWithText("طباعة").assertIsEnabled()
        dark(false);ui.runOnIdle{state.value=state.value.copy(isItemsLoaded=false)}
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled()
        verify(exactly=1){vm.refreshReturnSummary()}
    }
    @Test fun realLoadedItemsExposeReturnNavigationDespiteALegacyFalseHasItemsFlag(){
        state.value=state.value.copy(transaction=tx.copy(hasItems=false),invoiceItems=emptyList(),isItemsLoaded=false)
        var returned=0L
        showApp{TransactionDetailsScreen(41,{},{},{returned=it},vm)}
        ui.onNodeWithContentDescription("مرتجع").assertDoesNotExist();dark(true)
        ui.runOnIdle{state.value=state.value.copy(invoiceItems=listOf(line),isItemsLoaded=true)}
        ui.onNodeWithContentDescription("مرتجع").performClick();assertEquals(41L,returned)
        dark(false);verify(exactly=0){vm.delete()}
    }
    @Test fun changedPaymentDetailsReplaceTheOldNoteAndKeepAllFivePaymentTypesDistinct(){
        state.value=state.value.copy(transaction=tx.copy(note="ملاحظة قديمة",paymentMethodName="طريقة قديمة",paymentType=PaymentType.CASH))
        showApp{TransactionDetailsScreen(41,{},{},{},vm)}
        reveal("ملاحظة قديمة");dark(true)
        for((type,label) in listOf(PaymentType.DEBT to "دين 📋",PaymentType.BANK to "بنك 🏦",PaymentType.WALLET to "محفظة 💳",PaymentType.OTHER to "أخرى 💰")){
            ui.runOnIdle{state.value=state.value.copy(transaction=state.value.transaction!!.copy(paymentType=type,paymentMethodName="طريقة محدثة",note=""))}
            reveal(label);ui.onNodeWithText(label).assertExists()
            ui.onNodeWithText("طريقة قديمة").assertDoesNotExist();ui.onNodeWithText("ملاحظة قديمة").assertDoesNotExist()
        }
        dark(false);ui.onNodeWithText("أخرى 💰").assertExists()
    }
}
