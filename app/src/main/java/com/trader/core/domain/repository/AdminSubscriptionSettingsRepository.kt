package com.trader.core.domain.repository

import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow

interface AdminSubscriptionSettingsRepository {
    fun getSubscriptionConfig(): Flow<Pair<List<SubscriptionPlan>, List<SubscriptionPaymentMethod>>>
    suspend fun savePlans(plans: List<SubscriptionPlan>)
    suspend fun savePaymentMethods(methods: List<SubscriptionPaymentMethod>)
}