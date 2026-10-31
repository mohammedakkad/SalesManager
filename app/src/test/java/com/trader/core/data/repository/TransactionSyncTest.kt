package com.trader.core.data.repository
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Test
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TransactionSyncTest {
    @Test fun remoteAbsenceMustNotDeleteUnsyncedSales() = runTest {
        val dao=mockk<TransactionDao>(relaxed=true)
        val sync=mockk<FirebaseSyncService>()
        val activation=mockk<ActivationRepository>()
        val items=mockk<InvoiceItemRepository>(relaxed=true)
        val statuses=listOf(SyncStatus.PENDING,SyncStatus.FAILED,SyncStatus.CONFLICT,SyncStatus.SYNCED)
        every { activation.observeMerchantCode() } returns flowOf("test-merchant")
        every { sync.observeTransactions("test-merchant") } returns flowOf(emptyList())
        coEvery { dao.getAllIds() } returns listOf(1L,2L,3L,4L)
        statuses.forEachIndexed { i,status -> coEvery { dao.getTransactionById(i+1L) } returns TransactionEntity(id=i+1L,customerId=7,amount=20.0,isPaid=false,syncStatus=status.name) }
        coEvery { items.getItemsForTransactionOnce(any()) } returns emptyList()
        TransactionRepositoryImpl(dao,mockk(relaxed=true),mockk(relaxed=true),sync,activation,items,mockk(relaxed=true),mockk(relaxed=true),backgroundScope)
        runCurrent()
        coVerify(exactly=1) { dao.deleteTransaction(match { it.id==4L }) }
        for(id in 1L..3L) coVerify(exactly=0) { dao.deleteTransaction(match { it.id==id }) }
    }
}
