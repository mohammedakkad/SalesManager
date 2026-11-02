package com.trader.salesmanager.sales.outbox

import androidx.annotation.Keep
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*

@Keep
data class RemoteChange(val backend: String, val path: String, val before: Map<String, Any?>?, val after: Map<String, Any?>?)
@Keep
data class SalePayload(val version: Int = 1, val changes: List<RemoteChange>, val local: SaleLocalState)
@Keep
data class SaleLocalState(val transaction: TransactionEntity?, val invoices: List<InvoiceItemEntity>,
    val units: List<ProductUnitEntity>, val stock: List<StockMovementEntity>, val boxes: List<CashBoxEntity>,
    val cash: List<CashBoxMovementEntity>, val returns: List<ReturnInvoiceEntity> = emptyList())

/** Stable legacy projection shapes. Only changed rows are retained in the immutable event. */
internal class SaleProjection private constructor(private val values: Map<Pair<String,String>,Map<String,Any?>>, val local: SaleLocalState) {
    fun changesTo(after: SaleProjection): List<RemoteChange> = (values.keys + after.values.keys).sortedWith(compareBy({it.first},{it.second}))
        .mapNotNull { key -> if (values[key] == after.values[key]) null else RemoteChange(key.first,key.second,values[key],after.values[key]) }
    companion object {
        suspend fun capture(db: AppDatabase, merchant: String, id: Long,
            unitRefs: Set<Pair<String,String>>, cashMethods: Set<Long>): SaleProjection {
            val units=unitRefs.groupBy { it.first }.flatMap { (product, keys) -> db.productDao().getUnitsForProduct(product).filter { unit -> keys.any { it.second==unit.id } } }
            val local=SaleLocalState(db.transactionDao().getTransactionById(id),db.invoiceItemDao().getForTransactionOnce(id),units,
                db.stockMovementDao().forTransactionOnce(id).filter { it.merchantId==merchant },cashMethods.mapNotNull { db.cashBoxDao().getByPaymentMethodId(it) }.filter { it.merchantId==merchant },
                db.cashBoxMovementDao().forTransactionOnce(id).filter { it.merchantId==merchant }, db.returnDao().forTransactionOnce(id).filter { it.merchantId==merchant })
            val values = linkedMapOf<Pair<String,String>,Map<String,Any?>>()
            fun add(backend:String,path:String,value:Map<String,Any?>) { values[backend to path]=value }
            listOfNotNull(local.transaction).forEach { t -> add("RTDB","transactions/${t.id}",mapOf(
                "id" to t.id,"customerId" to t.customerId,"amount" to t.amount,"originalAmount" to (t.originalAmount ?: t.amount),
                "returnStatus" to (t.returnStatus ?: "NONE"),"isPaid" to t.isPaid,"paymentMethodId" to t.paymentMethodId,
                "note" to t.note,"date" to t.date,"paidAt" to t.paidAt,"hasItems" to t.hasItems,"dueDate" to t.dueDate,"reminderEnabled" to t.reminderEnabled)) }
            local.invoices.filter { it.merchantId==merchant }.forEach { i ->
                val map=mapOf("id" to i.id,"transactionId" to i.transactionId,"productId" to i.productId,"productName" to i.productName,
                    "unitId" to i.unitId,"unitLabel" to i.unitLabel,"quantity" to i.quantity,"pricePerUnit" to i.pricePerUnit,"totalPrice" to i.totalPrice)
                add("RTDB","invoice_items/${i.id}",map);add("FIRESTORE","invoice_items/${i.id}",map + ("merchantId" to merchant))
            }
            units.forEach { unit -> add("FIRESTORE","products/${unit.productId}/units/${unit.id}",mapOf("quantityInStock" to unit.quantityInStock)) }
            local.stock.filter { it.merchantId==merchant }.forEach { m -> add("FIRESTORE","stock_movements/${m.id}",mapOf(
                "id" to m.id,"productId" to m.productId,"productName" to m.productName,"unitId" to m.unitId,"unitLabel" to m.unitLabel,
                "movementType" to m.movementType,"quantity" to m.quantity,"quantityBefore" to m.quantityBefore,"quantityAfter" to m.quantityAfter,
                "relatedTransactionId" to m.relatedTransactionId,"note" to m.note,"merchantId" to merchant,"createdAt" to m.createdAt)) }
            local.boxes.filter { it.merchantId==merchant }.forEach { b -> add("RTDB","cash_boxes/${b.id}",mapOf(
                "id" to b.id,"paymentMethodId" to b.paymentMethodId,"paymentMethodName" to b.paymentMethodName,"currentBalance" to b.currentBalance,
                "initialBalance" to b.initialBalance,"initialBalanceSetAt" to b.initialBalanceSetAt,"updatedAt" to b.updatedAt)) }
            local.cash.filter { it.merchantId==merchant }.forEach { m -> add("RTDB","cash_box_movements/${m.id}",mapOf(
                "id" to m.id,"cashBoxId" to m.cashBoxId,"paymentMethodName" to m.paymentMethodName,"type" to m.type,
                "amountDelta" to m.amountDelta,"balanceAfter" to m.balanceAfter,"note" to m.note,"relatedTransactionId" to m.relatedTransactionId,"createdAt" to m.createdAt)) }
            local.returns.filter { it.merchantId==merchant }.forEach { r ->
                val items=db.returnDao().getReturnItems(r.id).associate { i -> i.id to mapOf("id" to i.id,"productId" to i.productId,"productName" to i.productName,
                    "unitId" to i.unitId,"unitLabel" to i.unitLabel,"originalQuantity" to i.originalQuantity,"returnedQty" to i.returnedQuantity,
                    "pricePerUnit" to i.pricePerUnit,"costPricePerUnit" to i.costPricePerUnit,"totalRefund" to i.totalRefund,"lostProfit" to i.lostProfit) }
                add("RTDB","return_invoices/${r.id}",mapOf("id" to r.id,"originalTransactionId" to r.originalTransactionId,"returnType" to r.returnType,
                    "totalRefund" to r.totalRefund,"note" to r.note,"createdAt" to r.createdAt,"merchantId" to merchant,"items" to items))
            }
            return SaleProjection(values, local)
        }
    }
}
