package com.trader.core.data.repository

import com.trader.core.data.local.dao.InvoiceItemDao
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.repository.InvoiceItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InvoiceItemRepositoryImpl(
    private val dao: InvoiceItemDao,
    private val remote: ProductFirestoreService,
    private val merchantId: String
) : InvoiceItemRepository {

    override fun getItemsForTransaction(transactionId: Long): Flow<List<InvoiceItem>> =
    dao.getForTransaction(transactionId).map {
        it.map {
            e -> e.toDomain()
        }
    }

    override suspend fun saveItems(items: List<InvoiceItem>) {
        dao.insertAll(items.map {
            it.toEntity()
        })
        try {
            remote.uploadInvoiceItems(merchantId, items)
        } catch (_: Exception) {}
    }

    override suspend fun getItemsForTransactionOnce(transactionId: Long): List<InvoiceItem> =
    dao.getForTransactionOnce(transactionId).map {
        it.toDomain()
    }

    override suspend fun deleteItemsForTransaction(transactionId: Long) {
        dao.deleteForTransaction(transactionId)
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