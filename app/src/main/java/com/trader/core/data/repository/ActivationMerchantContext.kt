package com.trader.core.data.repository

import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.MerchantContext
import kotlinx.coroutines.flow.Flow

/** MerchantContext backed by the persisted activation (DataStore), so it always reflects the live code. */
class ActivationMerchantContext(private val activation: ActivationRepository) : MerchantContext {
    override suspend fun currentCode(): String = activation.getMerchantCode()
    override fun observeCode(): Flow<String> = activation.observeMerchantCode()
}
