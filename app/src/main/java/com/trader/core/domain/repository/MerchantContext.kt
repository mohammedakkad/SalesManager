package com.trader.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The merchant that is active *now*. Merchant-scoped code must read the code through this
 * instead of capturing a String once: the app can deactivate merchant A and activate merchant B
 * in the same process (ActivationViewModel.deactivate(), MerchantWatcherViewModel).
 */
interface MerchantContext {
    /** Current merchant code; "" when no merchant is active. */
    suspend fun currentCode(): String

    /** Current merchant code and every later change. */
    fun observeCode(): Flow<String>

    companion object {
        /** A context that never changes (tests, previews). */
        fun fixed(code: String): MerchantContext = object : MerchantContext {
            override suspend fun currentCode() = code
            override fun observeCode(): Flow<String> = flowOf(code)
        }
    }
}
