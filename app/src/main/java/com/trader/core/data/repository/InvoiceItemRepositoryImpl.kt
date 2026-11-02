package com.trader.core.data.repository

import androidx.room.withTransaction
import com.trader.core.data.local.dao.InvoiceItemDao
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.repository.InvoiceItemRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class InvoiceItemRepositoryImpl(
    private val dao: InvoiceItemDao,
    private val remote: ProductFirestoreService,
    private val sync: FirebaseSyncService,
    private val merchantId: String,
    private val outboxDatabase: com.trader.core.data.local.db.AppDatabase? = null,
    private val activation: com.trader.core.domain.repository.ActivationRepository? = null
) : InvoiceItemRepository {


    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync() // ✅ أضف
    }

    // ✅ مزامنة الأصناف من Firebase Realtime Database
    private fun startRealtimeSync() {
        syncScope.launch {
          try {
            (activation?.observeMerchantCode() ?: kotlinx.coroutines.flow.flowOf(merchantId))
                .collectLatest { code ->
                    if (code.isBlank() || (activation == null && code != merchantId)) return@collectLatest
                    sync.observeInvoiceItems(code).collect { items ->
                        items.forEach { item ->
                            if (activation != null && activation.getMerchantCode() != code) return@forEach
                            if (outboxDatabase == null) dao.insertAll(listOf(item.toEntity()))
                            else outboxDatabase.withTransaction {
                                if (!outboxDatabase.saleOutboxDao().manages(item.transactionId)) dao.insertAll(listOf(item.toEntity()))
                            }
                        }
                    }
                }
          } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
          catch (error: Exception) { android.util.Log.e("InvoiceSync", "Legacy invoice listener stopped: ${error.javaClass.simpleName}") }
        }
    }

    override suspend fun saveItems(items: List<InvoiceItem>) {
        val merchantId = activation?.getMerchantCode() ?: this.merchantId
        dao.insertAll(items.map { it.toEntity() })
        if (outboxDatabase?.inTransaction() != true) syncScope.launch {
            if (activation != null && activation.getMerchantCode() != merchantId) return@launch
            if (items.any { outboxDatabase?.saleOutboxDao()?.manages(it.transactionId) == true }) return@launch
            var firestoreOk = false
            var realtimeOk = false
            runCatching { remote.uploadInvoiceItems(merchantId, items) }
                .onSuccess { firestoreOk = true }
            runCatching { sync.pushInvoiceItems(merchantId, items) }
                .onSuccess { realtimeOk = true }
            if (firestoreOk && realtimeOk) {
                items.forEach { dao.markSynced(it.id) }
            }
        }
    }

    override suspend fun deleteItemsForTransaction(transactionId: Long) {
        val merchantId = activation?.getMerchantCode() ?: this.merchantId
        dao.deleteForTransaction(transactionId)
        if (outboxDatabase?.inTransaction() != true) syncScope.launch {
            if (activation != null && activation.getMerchantCode() != merchantId) return@launch
            if (outboxDatabase?.saleOutboxDao()?.manages(transactionId) == true) return@launch
            try {
                sync.deleteInvoiceItemsForTransaction(merchantId, transactionId)
            } catch (_: Exception) {}
        }
    }



    override fun getItemsForTransaction(transactionId: Long): Flow<List<InvoiceItem>> =
    dao.getForTransaction(transactionId).map {
        it.map {
            e -> e.toDomain()
        }
    }


    override suspend fun getItemsForTransactionOnce(transactionId: Long): List<InvoiceItem> =
    dao.getForTransactionOnce(transactionId).map {
        it.toDomain()
    }


    override suspend fun syncPendingItems() {
        val merchantId = activation?.getMerchantCode() ?: this.merchantId
        if (activation != null && activation.getMerchantCode() != merchantId) return
        val pending = dao.getPending().filter { (activation == null || it.merchantId == merchantId) && outboxDatabase?.saleOutboxDao()?.manages(it.transactionId) != true }
        if (pending.isEmpty()) return

        val items = pending.map { it.toDomain() }

        var firestoreSynced = false
        var realtimeSynced = false

        runCatching { remote.uploadInvoiceItems(merchantId, items) }
            .onSuccess { firestoreSynced = true }
        runCatching { sync.pushInvoiceItems(merchantId, items) }
            .onSuccess { realtimeSynced = true }

        if (firestoreSynced && realtimeSynced) {
            pending.forEach { dao.markSynced(it.id) }
        } else {
            pending.forEach { dao.markFailed(it.id) }
        }
    }
}