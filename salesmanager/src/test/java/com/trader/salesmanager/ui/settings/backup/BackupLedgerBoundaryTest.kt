package com.trader.salesmanager.ui.settings.backup

import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import org.junit.Test
import org.junit.Assert.*

/** Payload compatibility only; financial records remain untouched by these tests. */
class BackupLedgerBoundaryTest {
    private val sale=TransactionEntity(id=8,customerId=7,amount=40.0,isPaid=false,date=1)
    private fun sold(id:String="sold",unit:String="u",quantity:Double=2.0)=InvoiceItem(id=id,transactionId=8,unitId=unit,quantity=quantity,pricePerUnit=25.0,totalPrice=quantity*25).toEntity()
    private fun invoice(id:String="r",saleId:Long=8,refund:Double=10.0)=ReturnInvoice(id=id,originalTransactionId=saleId,totalRefund=refund).toEntity()
    private fun returned(id:String="returned",invoiceId:String="r",unit:String="u",quantity:Double=1.0)=ReturnItem(id=id,returnInvoiceId=invoiceId,unitId=unit,originalQuantity=2.0,returnedQuantity=quantity,pricePerUnit=25.0).toEntity()
    private fun migrated(tx:TransactionEntity=sale, sold:List<InvoiceItemEntity> = listOf(sold()), invoices:List<ReturnInvoiceEntity> = listOf(invoice()), returns:List<ReturnItemEntity> = listOf(returned()))=
        BackupMigrator.migrateToCurrent(BackupPayload(transactions=listOf(tx),invoiceItems=sold,returnInvoices=invoices,returnItems=returns)).transactions.single()
    @Test fun fullReturnSumsSplitSoldAndReturnedLinesForTheSameUnit() {
        val result=migrated(sold=listOf(sold("a",quantity=1.0),sold("b",quantity=2.0)),returns=listOf(returned("x",quantity=1.0),returned("y",quantity=2.0)))
        assertEquals("FULLY_RETURNED",result.returnStatus);assertEquals(50.0,result.originalAmount!!,0.0)
        assertEquals(40.0,result.amount,0.0)
    }
    @Test fun missingReturnForOneSoldUnitKeepsSalePartial() {
        val result=migrated(sold=listOf(sold(),sold("second","other")),returns=listOf(returned(quantity=2.0)))
        assertEquals("PARTIALLY_RETURNED",result.returnStatus)
    }
    @Test fun returnsWithoutSoldLinesNeverInferFullReturn() {
        assertEquals("PARTIALLY_RETURNED",migrated(sold=emptyList()).returnStatus)
    }
    @Test fun returnInvoiceWithoutItemsReconstructsAmountButKeepsPartialStatus() {
        val result=migrated(returns=emptyList())
        assertEquals("PARTIALLY_RETURNED",result.returnStatus);assertEquals(50.0,result.originalAmount!!,0.0)
    }
    @Test fun explicitCurrentReturnStateAndOriginalAmountArePreserved() {
        val current=sale.copy(originalAmount=100.0,returnStatus="FULLY_RETURNED")
        assertEquals(current,migrated(tx=current))
    }
    @Test fun missingStatusIsReconstructedWithoutReplacingExplicitOriginalAmount() {
        val result=migrated(tx=sale.copy(originalAmount=80.0))
        assertEquals(80.0,result.originalAmount!!,0.0);assertEquals("PARTIALLY_RETURNED",result.returnStatus)
    }
    @Test fun missingOriginalAmountIsReconstructedWithoutReplacingExplicitStatus() {
        val result=migrated(tx=sale.copy(returnStatus="FULLY_RETURNED"))
        assertEquals(50.0,result.originalAmount!!,0.0);assertEquals("FULLY_RETURNED",result.returnStatus)
    }
    @Test fun unrelatedReturnInvoicesDoNotAlterSaleOrInflateOriginalAmount() {
        assertEquals(sale,migrated(invoices=listOf(invoice(saleId=99))))
    }
    @Test fun multipleReturnInvoicesSumRefundsWithoutChangingNetAmount() {
        val result=migrated(invoices=listOf(invoice(),invoice("r2",refund=15.0)),returns=listOf(returned(),returned("second","r2")))
        assertEquals(65.0,result.originalAmount!!,0.0);assertEquals("FULLY_RETURNED",result.returnStatus)
        assertEquals(40.0,result.amount,0.0)
    }
}
