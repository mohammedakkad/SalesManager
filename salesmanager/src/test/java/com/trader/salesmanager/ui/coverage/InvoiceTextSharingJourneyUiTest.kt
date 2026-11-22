package com.trader.salesmanager.ui.coverage

import android.content.Intent
import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.transactions.details.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InvoiceTextSharingJourneyUiTest:OfflineSellerUiTest() {
    private val vm=mockk<TransactionDetailsViewModel>(relaxed=true)
    @Test fun actualQuickShareActionPreservesPartialReturnIdentityAndNetAmount(){
        val t=Transaction(42,7,"زبون النص",60.0,originalAmount=100.0,isPaid=false,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED)
        every{vm.uiState} returns MutableStateFlow(TransactionDetailsUiState(transaction=t,isItemsLoaded=true,
            returnSummary=ReturnSummary(returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED,totalRefunded=40.0,returnedByUnit=mapOf("u" to 1.0)),
            invoiceItems=listOf(InvoiceItem("i",transactionId=42,productId="p",productName="صنف النص",unitId="u",unitLabel="حبة",quantity=2.0,pricePerUnit=50.0,totalPrice=100.0))))
        show{TransactionDetailsScreen(42,{},{},{},vm)}
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("مشاركة نصية سريعة"))
        ui.onNodeWithText("مشاركة نصية سريعة").performScrollTo().performClick()
        val intent=shadowOf(activity).nextStartedActivity!!;assertEquals(Intent.ACTION_SEND,intent.action);assertEquals("com.whatsapp",intent.`package`)
        val text=intent.getStringExtra(Intent.EXTRA_TEXT)!!;assertTrue(text.contains("*رقم الفاتورة:* 42"));assertTrue(text.contains("*المبلغ الأصلي:* ₪100.00"));assertTrue(text.contains("*المرتجع:* ₪40.00"));assertTrue(text.contains("*الإجمالي الصافي:* ₪60.00"))
        verify(exactly=0){vm.delete()}
    }
}
