package com.trader.salesmanager.sales

import androidx.room.withTransaction
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.security.MessageDigest
import java.util.UUID

/** Seller aggregate writer. All collaborators here are local DAOs; no remote repository is used. */
class SaleCommands(private val db: AppDatabase) {
    suspend fun create(operationId: String, merchantId: String, sale: Transaction, items: List<InvoiceItem>): Long {
        require(sale.id == 0L)
        val hash = hash(listOf(sale, items))
        return mutation(operationId, merchantId, "CREATE", hash) {
            require(sale.amount.isFinite() && sale.amount > 0)
            val id = db.transactionDao().insertTransaction(TransactionEntity.fromDomain(
                sale.copy(hasItems = items.isNotEmpty(), syncStatus = SyncStatus.PENDING)))
            val lines = items.map { it.copy(transactionId = id, merchantId = merchantId, syncStatus = SyncStatus.PENDING).toEntity() }
            db.invoiceItemDao().insertAll(lines)
            lines.forEach { stock(it, -it.quantity, id, merchantId) }
            cash(null, sale.copy(id = id), merchantId)
            id
        }
    }

    private suspend fun mutation(op: String, merchant: String, kind: String, hash: String, body: suspend () -> Long): Long = db.withTransaction {
        require(op.isNotBlank() && merchant.isNotBlank())
        db.saleOperationDao().get(merchant, op)?.let {
            check(it.kind == kind && it.requestHash == hash) { "Operation ID reused for another request" }
            return@withTransaction it.transactionId
        }
        val id = body()
        currentCoroutineContext().ensureActive()
        db.saleOperationDao().insert(SaleOperationEntity(merchant, op, kind, hash, id))
        id
    }

    private suspend fun stock(line: InvoiceItemEntity, delta: Double, id: Long, merchant: String) {
        require(line.quantity.isFinite() && line.quantity > 0)
        val unit = db.productDao().getUnitsForProduct(line.productId).singleOrNull { it.id == line.unitId }
            ?: error("Invoice unit no longer exists")
        require(db.productDao().getById(line.productId)?.product?.merchantId == merchant) { "Wrong merchant" }
        val next = unit.quantityInStock + delta
        require(next.isFinite() && next >= 0) { "Insufficient stock" }
        db.productDao().updateQuantity(line.unitId, next)
        db.stockMovementDao().insert(StockMovementEntity(UUID.randomUUID().toString(), line.productId,
            line.productName, line.unitId, line.unitLabel,
            if (delta < 0) MovementType.SALE_OUT.name else MovementType.RETURN_IN.name,
            delta, unit.quantityInStock, next, id, "", merchant, System.currentTimeMillis(), SyncStatus.PENDING.name))
    }

    private suspend fun cash(old: Transaction?, new: Transaction?, merchant: String) {
        val deltas = mutableMapOf<Long, Double>()
        if (old?.isPaid == true) old.paymentMethodId?.let { deltas[it] = -old.amount }
        if (new?.isPaid == true) new.paymentMethodId?.let { deltas[it] = (deltas[it] ?: 0.0) + new.amount }
        deltas.filterValues { it != 0.0 }.forEach { (method, delta) ->
            val box = db.cashBoxDao().getByPaymentMethodId(method) ?: error("Cash box is not provisioned locally")
            require(box.merchantId == merchant) { "Wrong cash merchant" }
            require((box.currentBalance + delta).isFinite())
            val now = System.currentTimeMillis()
            db.cashBoxDao().applyDelta(box.id, delta, now)
            val balance = db.cashBoxDao().getById(box.id)!!.currentBalance
            db.cashBoxMovementDao().insert(CashBoxMovementEntity(UUID.randomUUID().toString(), box.id,
                box.paymentMethodName, CashBoxMovementType.TRANSACTION_EFFECT.name, delta, balance,
                relatedTransactionId = new?.id ?: old?.id, createdAt = now, merchantId = merchant))
        }
    }

    private fun hash(value: Any): String = MessageDigest.getInstance("SHA-256")
        .digest(Gson().toJson(value).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
}
