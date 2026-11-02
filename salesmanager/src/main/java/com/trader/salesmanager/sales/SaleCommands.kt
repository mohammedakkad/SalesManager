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
class SaleCommands internal constructor(private val db: AppDatabase, private val beforeCommit: suspend () -> Unit = {}) {
    suspend fun create(operationId: String, merchantId: String, sale: Transaction, items: List<InvoiceItem>): Long {
        require(sale.id == 0L)
        val hash = hash(listOf(sale, items))
        return mutation(operationId, merchantId, "CREATE", hash, 0, items, sale.paymentMethodId) {
            require(sale.amount.isFinite() && sale.amount > 0)
            validateLines(items, 0, sale.amount)
            val id = db.transactionDao().insertTransaction(TransactionEntity.fromDomain(
                sale.copy(hasItems = items.isNotEmpty(), syncStatus = SyncStatus.PENDING)))
            val lines = items.map { it.copy(transactionId = id, merchantId = merchantId, syncStatus = SyncStatus.PENDING).toEntity() }
            db.invoiceItemDao().insertAll(lines)
            lines.forEach { stock(it, -it.quantity, id, merchantId) }
            cash(null, sale.copy(id = id), merchantId)
            id
        }
    }

    /** sale.amount is the gross invoice value; recorded refunds remain deducted. Null lines preserve the invoice. */
    suspend fun edit(operationId: String, merchantId: String, expected: Transaction, sale: Transaction, items: List<InvoiceItem>?): Long {
        val hash = hash(listOf(expected, sale, items))
        return mutation(operationId, merchantId, "EDIT", hash, expected.id, items.orEmpty(), sale.paymentMethodId) {
            editLocal(merchantId, expected, sale, items)
        }
    }

    private suspend fun editLocal(merchant: String, expected: Transaction, sale: Transaction, items: List<InvoiceItem>?): Long {
        val current = db.transactionDao().getTransactionById(expected.id) ?: error("Sale no longer exists")
        check(sameSale(current.toDomain(), expected)) { "Sale changed; reload before editing" }
        require(sale.id == expected.id && sale.amount.isFinite() && sale.amount >= 0)
        require(db.saleOperationDao().forTransaction(expected.id).all { it.merchantId == merchant }) { "Wrong sale merchant" }
        val old = current.toDomain()
        val refunds = db.returnDao().getAllInvoicesOnce().filter { it.originalTransactionId == old.id }.sumOf { it.totalRefund }
        require(sale.amount >= refunds) { "Invoice value cannot be less than existing refunds" }
        val oldItems = db.invoiceItemDao().getForTransactionOnce(old.id)
        require(oldItems.all { it.merchantId == merchant }) { "Wrong invoice merchant" }
        val returned = db.returnDao().getReturnedByUnit(old.id).associate { it.unitId to it.total }
        if (items != null) {
            validateLines(items, old.id, sale.amount)
            val quantities = items.groupBy { it.unitId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
            require(returned.all { (unit, qty) -> (quantities[unit] ?: 0.0) >= qty }) { "Cannot remove already returned quantities" }
            remaining(oldItems, returned).forEach { stock(it, it.quantity, old.id, merchant) }
            db.invoiceItemDao().deleteForTransaction(old.id)
            val replacements = items.map { it.copy(transactionId = old.id, merchantId = merchant, syncStatus = SyncStatus.PENDING).toEntity() }
            db.invoiceItemDao().insertAll(replacements)
            remaining(replacements, returned).forEach { stock(it, -it.quantity, old.id, merchant) }
        }
        val finalQuantities = (items?.map { it.toEntity() } ?: oldItems).groupBy { it.unitId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
        val status = when {
            returned.values.none { it > 0 } -> TransactionReturnStatus.NONE
            finalQuantities.isNotEmpty() && finalQuantities.all { (unit, quantity) -> (returned[unit] ?: 0.0) >= quantity } -> TransactionReturnStatus.FULLY_RETURNED
            else -> TransactionReturnStatus.PARTIALLY_RETURNED
        }
        val new = sale.copy(amount = sale.amount - refunds, originalAmount = sale.amount,
            returnStatus = status, hasItems = items?.isNotEmpty() ?: oldItems.isNotEmpty(),
            syncStatus = SyncStatus.PENDING)
        db.transactionDao().updateTransaction(TransactionEntity.fromDomain(new))
        cash(old, new, merchant)
        return old.id
    }

    suspend fun delete(operationId: String, merchantId: String, expected: Transaction): Long {
        require(expected.id > 0)
        return mutation(operationId, merchantId, "DELETE", hash(expected), expected.id, emptyList(), expected.paymentMethodId) {
            deleteLocal(merchantId, expected)
        }
    }

    private suspend fun deleteLocal(merchant: String, expected: Transaction): Long {
        val current = db.transactionDao().getTransactionById(expected.id) ?: return expected.id
        require(db.saleOperationDao().forTransaction(expected.id).all { it.merchantId == merchant }) { "Wrong sale merchant" }
        val sale = current.toDomain()
        check(sameSale(sale, expected)) { "Sale changed; reload before deleting" }
        val lines = db.invoiceItemDao().getForTransactionOnce(sale.id)
        require(lines.all { it.merchantId == merchant }) { "Wrong invoice merchant" }
        val returned = db.returnDao().getReturnedByUnit(sale.id).associate { it.unitId to it.total }
        remaining(lines, returned).forEach { line ->
            // A physically deleted catalog unit cannot be restocked (same return policy).
            if (db.productDao().getProductIdForUnit(line.unitId) != null) stock(line, line.quantity, sale.id, merchant)
        }
        db.returnDao().deleteForTransaction(sale.id)
        db.invoiceItemDao().deleteForTransaction(sale.id)
        db.transactionDao().deleteById(sale.id)
        cash(sale, null, merchant)
        return sale.id
    }

    private suspend fun validateLines(lines: List<InvoiceItem>, transactionId: Long, gross: Double) {
        require(lines.map { it.id }.distinct().size == lines.size) { "Duplicate invoice line identity" }
        lines.forEach { line ->
            require(line.id.isNotBlank() && line.quantity.isFinite() && line.quantity > 0 &&
                line.pricePerUnit.isFinite() && line.pricePerUnit >= 0 && line.totalPrice.isFinite() &&
                kotlin.math.abs(line.quantity * line.pricePerUnit - line.totalPrice) <= 0.000001) { "Invalid invoice line" }
            val existing = db.invoiceItemDao().getById(line.id)
            require(existing == null || (transactionId > 0 && existing.transactionId == transactionId)) { "Invoice line belongs to another sale" }
        }
        require(lines.sumOf { it.totalPrice } <= gross + 0.000001) { "Invoice lines exceed sale value" }
    }

    private fun sameSale(left: Transaction, right: Transaction): Boolean {
        fun normalized(t: Transaction) = t.copy(customerName = "", paymentMethodName = "", syncStatus = SyncStatus.SYNCED)
        return normalized(left) == normalized(right)
    }

    /** Subtract returns once across all price lines for a unit, not once per line. */
    private fun remaining(lines: List<InvoiceItemEntity>, returned: Map<String, Double>): List<InvoiceItemEntity> {
        val toSubtract = returned.toMutableMap()
        return lines.mapNotNull { line ->
            val consumed = minOf(line.quantity, toSubtract[line.unitId] ?: 0.0)
            toSubtract[line.unitId] = (toSubtract[line.unitId] ?: 0.0) - consumed
            (line.quantity - consumed).takeIf { it > 0.0 }?.let { line.copy(quantity = it) }
        }
    }

    private suspend fun mutation(op: String, merchant: String, kind: String, hash: String, aggregateId: Long, items: List<InvoiceItem>, method: Long?, body: suspend () -> Long): Long = db.withTransaction {
        require(op.isNotBlank() && merchant.isNotBlank())
        db.saleOperationDao().get(merchant, op)?.let {
            check(it.kind == kind && it.requestHash == hash) { "Operation ID reused for another request" }
            return@withTransaction it.transactionId
        }
        val oldItems = db.invoiceItemDao().getForTransactionOnce(aggregateId)
        val unitRefs = (oldItems.map { it.productId to it.unitId } + items.map { it.productId to it.unitId }).toSet()
        val methods = listOfNotNull(method, db.transactionDao().getTransactionById(aggregateId)?.paymentMethodId).toSet()
        val before = com.trader.salesmanager.sales.outbox.SaleProjection.capture(db, merchant, aggregateId, unitRefs, methods)
        val id = body()
        val after = com.trader.salesmanager.sales.outbox.SaleProjection.capture(db, merchant, id, unitRefs, methods)
        val payload = com.trader.salesmanager.sales.outbox.SalePayload(changes = before.changesTo(after), local = after.local)
        val now = System.currentTimeMillis()
        db.saleOutboxDao().insert(SaleOutboxEntity(eventId = UUID.randomUUID().toString(), operationId = op,
            merchantId = merchant, aggregateId = id, aggregateRevision = db.saleOutboxDao().revision(merchant, id) + 1,
            eventType = kind, payloadVersion = 1, payload = Gson().toJson(payload), createdAt = now, updatedAt = now))
        beforeCommit()
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

    // Versioned primitive tuples avoid R8/reflection field-name changes in durable receipts.
    private fun canonical(value: Any?): Any? = when (value) {
        is Transaction -> listOf(value.id, value.customerId, value.amount, value.originalAmount, value.isPaid,
            value.paymentMethodId, value.note, value.date, value.paidAt, value.paymentType.name, value.hasItems,
            value.dueDate, value.reminderEnabled, value.returnStatus.name)
        is InvoiceItem -> listOf(value.id, value.transactionId, value.productId, value.productName, value.unitId,
            value.unitLabel, value.quantity, value.pricePerUnit, value.totalPrice, value.merchantId)
        is List<*> -> value.map(::canonical)
        else -> value
    }

    private fun hash(value: Any): String = MessageDigest.getInstance("SHA-256")
        .digest(("sale-command-v1:" + Gson().toJson(canonical(value))).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
