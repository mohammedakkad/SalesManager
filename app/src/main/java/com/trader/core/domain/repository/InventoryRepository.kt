package com.trader.core.domain.repository

import com.trader.core.domain.model.InventorySession
import com.trader.core.domain.model.InventorySessionItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getActiveSession(): Flow<InventorySession?>
    fun getSessionItems(sessionId: String): Flow<List<InventorySessionItem>>
    fun getAllSessions(): Flow<List<InventorySession>>

    suspend fun startNewSession(merchantId: String): String
    suspend fun updateSessionItem(item: InventorySessionItem)
    suspend fun finishSession(sessionId: String)  // يطبق التعديلات ويسجل StockMovements
    suspend fun cancelSession(sessionId: String)
}
