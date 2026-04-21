package com.trader.core.data.repository

import com.trader.core.data.local.dao.InvoiceItemDao
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.repository.InvoiceItemRepository
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
    private val merchantId: String
) : InvoiceItemRepository {


    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync() // ✅ أضف
    }

    // ✅ مزامنة الأصناف من Firebase Realtime Database
    private fun startRealtimeSync() {
        syncScope.launch {
            try {
                sync.observeInvoiceItems(merchantId).collect {
                    items ->
                    items.forEach {
                        item ->
                        try {
                            dao.insertAll(listOf(item.toEntity()))
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun saveItems(items: List<InvoiceItem>) {
        dao.insertAll(items.map {
            it.toEntity()
        })
        syncScope.launch {
            try {
                remote.uploadInvoiceItems(merchantId, items) // Firestore
                sync.pushInvoiceItems(merchantId, items) // ✅ Realtime Database
            } catch (_: Exception) {}
        }
    }

    override suspend fun deleteItemsForTransaction(transactionId: Long) {
        dao.deleteForTransaction(transactionId)
        syncScope.launch {
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
        val pending = dao.getPending()
        if (pending.isEmpty()) return
        try {
            remote.uploadInvoiceItems(merchantId, pending.map {
                it.toDomain()
            })
            pending.forEach {
                dao.markSynced(it.id)
            }
        } catch (_: Exception) {}
    }
}