package com.trader.salesmanager.sales

import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.sync.SyncCoordinator
import kotlinx.coroutines.*

/** Best effort after-commit dispatch only. Durable deletion retries require the Stage 11 outbox. */
class SaleSync(
    private val remote: FirebaseSyncService,
    private val activation: ActivationRepository,
    private val coordinator: SyncCoordinator,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    fun deleted(merchant: String, transactionId: Long) {
        scope.launch {
            if (activation.getMerchantCode() != merchant) return@launch
            try {
                remote.deleteInvoiceItemsForTransaction(merchant, transactionId)
                remote.deleteTransaction(merchant, transactionId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("SaleSync", "Sale deletion remains unsynchronized", error)
            }
            coordinator.syncAll()
        }
    }
}
