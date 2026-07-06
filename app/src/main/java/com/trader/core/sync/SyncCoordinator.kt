package com.trader.core.sync

import android.util.Log
import com.trader.core.domain.repository.CashBoxRepository
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * SyncCoordinator — مُنسِّق المزامنة التلقائية (Fix 3).
 *
 * يستمع لتغييرات حالة الشبكة. عند استعادة الاتصال (false → true)،
 * يُشغِّل syncPendingMovements() و syncPendingItems() لمعالجة أي كتابات معلقة.
 *
 * - Single Responsibility: مسؤول عن التنسيق فقط، لا يعرف شيئاً عن Firebase أو Room مباشرة.
 * - Dependency Inversion: يعتمد على abstractions (interfaces) وليس على implementations.
 * - يجب أن يكون single() في Koin لضمان نسخة واحدة فقط طوال عمر التطبيق.
 */
class SyncCoordinator(
    private val networkMonitor: NetworkMonitor,
    private val stockRepository: StockRepository,
    private val invoiceItemRepository: InvoiceItemRepository,
    private val cashBoxRepository: CashBoxRepository
) {
    // SupervisorJob: فشل coroutine واحدة لا يُلغي الأخريات
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * يُشغَّل مرة واحدة عند بدء التطبيق في SalesManagerApp.
     * يراقب الاتصال ويُشغِّل المزامنة تلقائياً عند استعادته.
     */
    fun start() {
        scope.launch {
            networkMonitor.isOnlineFlow
                .distinctUntilChanged()
                .filter { isOnline -> isOnline }  // فقط عند الانتقال offline → online
                .collect {
                    Log.d("SyncCoordinator", "Network restored — syncing pending data")
                    syncAll()
                }
        }
    }

    /**
     * مزامنة جميع البيانات المعلقة.
     * يُستدعى تلقائياً عند استعادة الشبكة، أو يدوياً من أي ViewModel يحتاج "Force Sync".
     * كل مهمة في coroutine مستقلة — فشل إحداهما لا يوقف الأخرى.
     */
    suspend fun syncAll() {
        scope.launch {
            runCatching { stockRepository.syncPendingMovements() }
                .onFailure { Log.e("SyncCoordinator", "syncPendingMovements failed", it) }
        }
        scope.launch {
            runCatching { invoiceItemRepository.syncPendingItems() }
                .onFailure { Log.e("SyncCoordinator", "syncPendingItems failed", it) }
        }
        scope.launch {
            runCatching { cashBoxRepository.syncPendingBoxes() }
                .onFailure { Log.e("SyncCoordinator", "syncPendingBoxes failed", it) }
        }
    }
}
