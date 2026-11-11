package com.trader.salesmanager.util

import android.app.Application
import android.content.*
import com.trader.core.domain.model.*
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class InvoiceTextContractsTest {
    private val transaction=Transaction(42,7,"زبون الاختبار",60.0,isPaid=false,paymentType=PaymentType.DEBT,date=0)
    private val item=InvoiceItem(id="line",transactionId=42,productId="p",productName="أرز",unitId="u",unitLabel="كيلو",quantity=2.5,pricePerUnit=40.0,totalPrice=100.0)
    private fun text(t:Transaction=transaction,items:List<InvoiceItem> = emptyList(),name:String="متجر الاختبار")=InvoiceSharer.buildInvoiceText(t,items,name)
    @Test fun ordinaryInvoicePreservesCustomerAmountPaymentStatusAndStore(){
        val s=text();assertTrue(s.contains("متجر الاختبار"));assertTrue(s.contains("زبون الاختبار"));assertTrue(s.contains("₪60.00"));assertTrue(s.contains("📋 دين"));assertTrue(s.contains("غير مدفوع"));assertFalse(s.contains("*الأصناف:*"))
    }
    @Test fun blankStoreUsesTheExistingGenericName(){assertTrue(text(name="  ").contains("فاتورة من المتجر"))}
    @Test fun itemQuantityTotalAndNoteArePreservedWithoutRecomputingLedgerValues(){
        val s=text(transaction.copy(note="تسليم مساءً"),listOf(item));assertTrue(s.contains("أرز (كيلو) × 2.5 = ₪100.00"));assertTrue(s.contains("تسليم مساءً"))
    }
    @Test fun eachPaymentKindUsesItsOwnArabicLabel(){
        for((kind,label) in listOf(PaymentType.CASH to "كاش",PaymentType.BANK to "بنك",PaymentType.WALLET to "محفظة",PaymentType.OTHER to "أخرى"))assertTrue(text(transaction.copy(paymentType=kind,isPaid=true)).contains(label))
        assertTrue(text(transaction.copy(isPaid=true)).contains("*الحالة:* مدفوع"))
    }
    @Test fun unsavedTransactionDoesNotInventAPersistedInvoiceNumber(){assertFalse(text(transaction.copy(id=0)).contains("رقم الفاتورة"))}
    @Test fun sharedTextIdentifiesTheActualPersistedInvoice(){assertTrue(text().contains("*رقم الفاتورة:* 42"))}
    @Test fun partialReturnDistinguishesOriginalItemsRefundAndNet(){
        val s=text(transaction.copy(originalAmount=100.0,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED),listOf(item))
        assertTrue(s.contains("*الأصناف قبل الإرجاع:*"));assertTrue(s.contains("*المبلغ الأصلي:* ₪100.00"));assertTrue(s.contains("*المرتجع:* ₪40.00"));assertTrue(s.contains("*الإجمالي الصافي:* ₪60.00"))
    }
    @Test fun fullReturnKeepsTheOriginalAmountAndShowsZeroNet(){
        val s=text(transaction.copy(amount=0.0,originalAmount=100.0,returnStatus=TransactionReturnStatus.FULLY_RETURNED),listOf(item))
        assertTrue(s.contains("*المرتجع:* ₪100.00"));assertTrue(s.contains("*الإجمالي الصافي:* ₪0.00"));assertTrue(s.contains("*المبلغ الأصلي:* ₪100.00"))
    }
    @Test fun whatsappIntentContainsExactlyTheBuiltTextAndNewTaskFlag(){
        val c=mockk<Context>();val intent=slot<Intent>();every{c.startActivity(capture(intent))} just Runs
        InvoiceSharer.shareInvoice(c,transaction,listOf(item),"متجر الاختبار")
        assertEquals("com.whatsapp",intent.captured.`package`);assertEquals("text/plain",intent.captured.type);assertEquals(Intent.ACTION_SEND,intent.captured.action)
        assertEquals(text(items=listOf(item)),intent.captured.getStringExtra(Intent.EXTRA_TEXT));assertTrue(intent.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK !=0)
    }
    @Test fun absentWhatsappUsesAnUnrestrictedChooserWithTheSameText(){
        val c=mockk<Context>();val calls=mutableListOf<Intent>()
        every{c.startActivity(any())} answers{val i=firstArg<Intent>();calls+=i;if(calls.size==1)throw ActivityNotFoundException("offline no whatsapp")}
        InvoiceSharer.shareInvoice(c,transaction,emptyList(),"متجر الاختبار")
        assertEquals(2,calls.size);assertEquals(Intent.ACTION_CHOOSER,calls[1].action)
        @Suppress("DEPRECATION") val share=calls[1].getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertNull(share.`package`);assertEquals(text(),share.getStringExtra(Intent.EXTRA_TEXT));assertEquals("text/plain",share.type)
    }
    @Test fun absentShareHandlersDoNotCrashTheSeller(){
        val c=mockk<Context>();every{c.startActivity(any())} throws ActivityNotFoundException("offline no handler")
        InvoiceSharer.shareInvoice(c,transaction,emptyList(),"متجر الاختبار");verify(exactly=2){c.startActivity(any())}
    }
}
