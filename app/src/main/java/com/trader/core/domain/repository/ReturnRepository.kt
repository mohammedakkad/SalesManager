package com.trader.core.domain.repository

import com.trader.core.domain.model.ReturnInvoice
import com.trader.core.domain.model.ReturnItem
import kotlinx.coroutines.flow.Flow

interface ReturnRepository {
    // ── معالجة الإرجاع ─────────────────────────────────────────
    suspend fun processReturn(
        returnInvoice: ReturnInvoice,
        items: List<ReturnItem>
    ): ReturnInvoice

    // ── استعلامات ───────────────────────────────────────────────
    fun getReturnsByTransaction(transactionId: Long): Flow<List<ReturnInvoice>>
    suspend fun getReturnItems(returnInvoiceId: String): List<ReturnItem>
    fun getAllReturns(): Flow<List<ReturnInvoice>>

    // ── Feature Flag ────────────────────────────────────────────
    suspend fun isPartialReturnEnabled(): Boolean
}
