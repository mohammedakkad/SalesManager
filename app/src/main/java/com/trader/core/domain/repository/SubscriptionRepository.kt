package com.trader.core.domain.repository

import com.trader.core.domain.model.SubscriptionRequest
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getPendingRequests(): Flow<List<SubscriptionRequest>>
    suspend fun approveRequest(request: SubscriptionRequest, planDurationDays: Int)
    suspend fun rejectRequest(request: SubscriptionRequest)
}
