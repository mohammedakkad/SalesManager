package com.trader.core.domain.repository

import com.trader.core.domain.model.MerchantStatus
import kotlinx.coroutines.flow.Flow

interface MerchantStatusRepository {
    fun observeMerchantStatus(code: String): Flow<MerchantStatus?>
    fun observeExpiryDays(code: String): Flow<Long?>  // days remaining, null = permanent
}