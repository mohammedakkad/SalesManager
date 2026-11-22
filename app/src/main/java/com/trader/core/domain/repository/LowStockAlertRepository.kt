package com.trader.core.domain.repository

import com.trader.core.domain.model.StockLevel

data class LowStockAlertCandidate(
    val unitId: String,
    val productId: String,
    val status: StockLevel
)

interface LowStockAlertRepository {
    suspend fun getNotificationCandidates(
        nowMillis: Long,
        cooldownMillis: Long
    ): List<LowStockAlertCandidate>

    suspend fun markNotified(
        candidates: List<LowStockAlertCandidate>,
        notifiedAt: Long
    )
}
