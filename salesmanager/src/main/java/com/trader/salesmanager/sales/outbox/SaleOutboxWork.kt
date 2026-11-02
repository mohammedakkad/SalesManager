package com.trader.salesmanager.sales.outbox

import android.content.Context
import androidx.work.*
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.TimeUnit

class SaleOutboxWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters),KoinComponent {
    override suspend fun doWork():Result {
        val merchant=get<ActivationRepository>().getMerchantCode()
        if(merchant.isBlank()) return Result.success()
        val dispatcher=get<OutboxDispatcher>()
        repeat(20) {
            when(dispatcher.once(merchant)) {
                DispatchResult.ACKNOWLEDGED -> Unit
                DispatchResult.RETRY,DispatchResult.STALE -> return Result.retry()
                DispatchResult.PAUSED,DispatchResult.CONFLICT -> return Result.success()
                DispatchResult.IDLE -> {
                    val first=get<AppDatabase>().saleOutboxDao().firstUnacknowledged(merchant)
                    return if(first!=null && first.state!="CONFLICTING") Result.retry() else Result.success()
                }
            }
        }
        return Result.retry()
    }
}

object SaleOutboxWork {
    private const val NAME="seller-sale-outbox"
    private fun constraints()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun enqueue(context:Context,replace:Boolean=false) {
        WorkManager.getInstance(context).enqueueUniqueWork(NAME,
            if(replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<SaleOutboxWorker>().setConstraints(constraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
    }
    fun periodic(context:Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("$NAME-recovery",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SaleOutboxWorker>(15,TimeUnit.MINUTES).setConstraints(constraints()).build())
    }
    fun cancel(context:Context) { WorkManager.getInstance(context).cancelUniqueWork(NAME) }
}

/** Startup, committed event insertion and account changes wake persisted WorkManager jobs. */
class SaleOutboxLifecycle(private val context:Context,private val db:AppDatabase,private val activation:ActivationRepository,
    private val scope:CoroutineScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)) {
    fun start() {
        SaleOutboxWork.periodic(context)
        scope.launch {
            activation.observeMerchantCode().distinctUntilChanged().collectLatest { merchant ->
                SaleOutboxWork.cancel(context)
                if(merchant.isNotBlank()) {
                    SaleOutboxWork.enqueue(context,true)
                    db.saleOutboxDao().observeLatestSequence().distinctUntilChanged().collect { SaleOutboxWork.enqueue(context) }
                }
            }
        }
    }
}
