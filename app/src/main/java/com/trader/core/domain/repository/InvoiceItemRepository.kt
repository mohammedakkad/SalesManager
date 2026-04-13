package com.trader.core.domain.repository

import com.trader.core.domain.model.InvoiceItem
import kotlinx.coroutines.flow.Flow

interface InvoiceItemRepository {
    fun getItemsForTransaction(transactionId: Long): Flow<List<InvoiceItem>>
    suspend fun saveItems(items: List<InvoiceItem>)
    suspend fun deleteItemsForTransaction(transactionId: Long)
    suspend fun syncPendingItems()
}
