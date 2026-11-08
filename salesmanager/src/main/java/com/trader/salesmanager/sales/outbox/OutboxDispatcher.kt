package com.trader.salesmanager.sales.outbox

import androidx.room.withTransaction
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

sealed class RemoteDelivery {
    data class Applied(val receipt: String): RemoteDelivery()
    data class Conflict(val reason: String): RemoteDelivery()
}
fun interface OutboxTransport { suspend fun deliver(event: SaleOutboxEntity): RemoteDelivery }
enum class DispatchResult { IDLE, ACKNOWLEDGED, RETRY, CONFLICT, PAUSED, STALE }

class OutboxDispatcher(
    private val db: AppDatabase,
    private val transport: OutboxTransport,
    private val sessions: Flow<String>,
    private val currentMerchant: suspend () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val leaseMillis: Long = 60000,
    private val timeoutMillis: Long = 20000
) {
    suspend fun once(merchant: String): DispatchResult {
        if (merchant.isBlank() || currentMerchant()!=merchant) return DispatchResult.PAUSED
        val token=UUID.randomUUID().toString()
        val event=db.saleOutboxDao().claim(merchant,token,clock(),leaseMillis)
        if(event==null) {
            if(currentMerchant()!=merchant) return DispatchResult.PAUSED
            // Recover status rows left by an older worker, even when no new sale is submitted.
            db.withTransaction {
                if(!db.saleOutboxDao().hasOutstandingFor(merchant)) acknowledgeDrainedQueue(merchant)
                currentCoroutineContext().ensureActive()
            }
            return DispatchResult.IDLE
        }
        try {
            if (currentMerchant()!=merchant) throw SessionChanged()
            val result=coroutineScope {
                val send=async { withTimeout(timeoutMillis) { transport.deliver(event) } }
                val guard=launch { sessions.first { it!=merchant };send.cancel(SessionChanged()) }
                try { send.await() } finally { guard.cancelAndJoin() }
            }
            currentCoroutineContext().ensureActive()
            if (currentMerchant()!=merchant) throw SessionChanged()
            return when(result) {
                is RemoteDelivery.Conflict -> {
                    val changed=db.saleOutboxDao().finishAttempt(event.eventId,event.aggregateRevision,token,"CONFLICTING",0,clock(),result.reason.take(160))
                    if(changed==1) DispatchResult.CONFLICT else DispatchResult.STALE
                }
                is RemoteDelivery.Applied -> db.withTransaction {
                    val changed=db.saleOutboxDao().acknowledge(event.eventId,event.aggregateRevision,token,clock(),result.receipt.take(200))
                    if(changed==1) {
                        if(!db.saleOutboxDao().hasOutstandingFor(merchant)) acknowledgeDrainedQueue(merchant)
                    }
                    currentCoroutineContext().ensureActive()
                    if(changed==1) DispatchResult.ACKNOWLEDGED else DispatchResult.STALE
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            retry(event,token,"TIMEOUT_UNCERTAIN")
            return DispatchResult.RETRY
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { retry(event,token,"CANCELLED_UNCERTAIN") }
            if(cancelled is SessionChanged || currentMerchant()!=merchant) return DispatchResult.PAUSED
            throw cancelled
        } catch (error: Exception) {
            retry(event,token,error.javaClass.simpleName)
            return DispatchResult.RETRY
        }
    }
    private suspend fun retry(event:SaleOutboxEntity,token:String,error:String) {
        val delay=minOf(6*60*60*1000L,1000L shl minOf(16,maxOf(0,event.retryCount-1)))
        db.saleOutboxDao().finishAttempt(event.eventId,event.aggregateRevision,token,"FAILED",clock()+delay,clock(),error)
    }
    /** Called inside the receipt transaction: failures roll back the receipt and every status update. */
    private suspend fun acknowledgeDrainedQueue(merchant: String) {
        var after=0L
        while(true) {
            currentCoroutineContext().ensureActive()
            val page=db.saleOutboxDao().latestAcknowledgedPage(merchant,after,64)
            if(page.isEmpty()) return
            page.forEach { acknowledgeMatchingRows(it) }
            after=page.last().sequence
        }
    }
    private suspend fun acknowledgeMatchingRows(event:SaleOutboxEntity) {
        val local=Gson().fromJson(event.payload,SalePayload::class.java).local
        local.transaction?.let { expected ->
            val current=db.transactionDao().getTransactionById(expected.id)
            if(current?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING") &&
                db.saleOperationDao().forTransaction(expected.id).all { it.merchantId==event.merchantId }) db.transactionDao().markSynced(expected.id)
        }
        local.invoices.forEach { expected ->
            if(db.invoiceItemDao().getById(expected.id)?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING")) db.invoiceItemDao().markSynced(expected.id)
        }
        local.units.forEach { expected ->
            if(db.productDao().getById(expected.productId)?.product?.merchantId==event.merchantId && db.productDao().getUnitsForProduct(expected.productId).find { it.id==expected.id }?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING")) db.productDao().markUnitSynced(expected.id)
        }
        local.boxes.forEach { expected ->
            if(db.cashBoxDao().getById(expected.id)?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING")) db.cashBoxDao().markSynced(expected.id)
        }
        val stock=db.stockMovementDao().forTransactionOnce(event.aggregateId).associateBy { it.id }
        local.stock.forEach { expected -> if(stock[expected.id]?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING")) db.stockMovementDao().markSynced(expected.id) }
        local.returns.orEmpty().forEach { expected ->
            if(db.returnDao().getById(expected.id)?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING")) db.returnDao().markSynced(expected.id)
        }
        local.cash.forEach { expected -> if(db.cashBoxMovementDao().getById(expected.id)?.copy(syncStatus="PENDING")==expected.copy(syncStatus="PENDING")) db.cashBoxMovementDao().markSynced(expected.id) }
    }
    private class SessionChanged: CancellationException("Merchant session changed")
}
