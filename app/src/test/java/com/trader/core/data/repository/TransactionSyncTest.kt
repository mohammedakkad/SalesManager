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
        TransactionRepositoryImpl(dao,mockk(relaxed=true),mockk(relaxed=true),sync,activation,items,mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),backgroundScope)
        runCurrent()
        coVerify(exactly=1) { dao.deleteTransaction(match { it.id==4L }) }
        for(id in 1L..3L) coVerify(exactly=0) { dao.deleteTransaction(match { it.id==id }) }
    }

    @Test fun deletingPartiallyReturnedSaleRestoresOnlyRemainingUnitsAndCash()=runTest {
        val dao=mockk<TransactionDao>(relaxed=true)
        val sync=mockk<FirebaseSyncService>(relaxed=true)
        val activation=mockk<ActivationRepository>()
        every { activation.observeMerchantCode() } returns kotlinx.coroutines.flow.emptyFlow()
        coEvery { activation.getMerchantCode() } returns "fixture"
        val items=mockk<InvoiceItemRepository>(relaxed=true)
        coEvery { items.getItemsForTransactionOnce(8) } returns listOf(com.trader.core.domain.model.InvoiceItem(id="line",transactionId=8,productId="p",unitId="u",quantity=4.0))
        val returns=mockk<ReturnDao>(relaxed=true)
        coEvery { returns.getReturnedByUnit(8) } returns listOf(UnitReturnSummary("u",1.0))
        val stock=mockk<StockRepository>(relaxed=true)
        val cash=mockk<CashBoxRepository>(relaxed=true)
        val repo=TransactionRepositoryImpl(dao,mockk(relaxed=true),mockk(relaxed=true),sync,activation,items,stock,cash,returns,backgroundScope)
        repo.deleteTransaction(com.trader.core.domain.model.Transaction(id=8,customerId=7,amount=75.0,originalAmount=100.0,isPaid=true,paymentMethodId=1,returnStatus=com.trader.core.domain.model.TransactionReturnStatus.PARTIALLY_RETURNED))
        coVerify(exactly=1) { stock.returnStock("p","u",3.0,8,any(),any()) }
        coVerify(exactly=1) { returns.deleteForTransaction(8) }
        coVerify { cash.applyTransactionEffect(8,1,75.0,true,null,0.0,false) }
    }
    @Test fun deletingFullyReturnedSaleDoesNotRestoreStockAgain()=runTest {
        val sync=mockk<FirebaseSyncService>(relaxed=true)
        val activation=mockk<ActivationRepository>()
        every { activation.observeMerchantCode() } returns kotlinx.coroutines.flow.emptyFlow()
        coEvery { activation.getMerchantCode() } returns "fixture"
        val items=mockk<InvoiceItemRepository>(relaxed=true)
        coEvery { items.getItemsForTransactionOnce(8) } returns listOf(com.trader.core.domain.model.InvoiceItem(id="line",transactionId=8,productId="p",unitId="u",quantity=4.0))
        val returns=mockk<ReturnDao>(relaxed=true)
        coEvery { returns.getReturnedByUnit(8) } returns listOf(UnitReturnSummary("u",4.0))
        val stock=mockk<StockRepository>(relaxed=true)
        val repo=TransactionRepositoryImpl(mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),sync,activation,items,stock,mockk(relaxed=true),returns,backgroundScope)
        repo.deleteTransaction(com.trader.core.domain.model.Transaction(id=8,customerId=7,amount=0.0,isPaid=false))
        coVerify(exactly=0) { stock.returnStock(any(),any(),any(),any(),any(),any()) }
    }

    private class Fixture(val scope:kotlinx.coroutines.CoroutineScope) {
        val dao=mockk<TransactionDao>(relaxed=true)
        val sync=mockk<FirebaseSyncService>(relaxed=true)
        val activation=mockk<ActivationRepository>()
        val codes=MutableStateFlow("fixture")
        val events=kotlinx.coroutines.flow.MutableSharedFlow<List<com.trader.core.domain.model.Transaction>>()
        init {
            every { activation.observeMerchantCode() } returns codes
            coEvery { activation.getMerchantCode() } answers { codes.value }
            every { sync.observeTransactions(any()) } returns events
            coEvery { dao.getAllIds() } returns emptyList()
        }
        fun repo()=TransactionRepositoryImpl(dao,mockk(relaxed=true),mockk(relaxed=true),sync,activation,mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),scope)
        fun queue() {
            coEvery { dao.getAllOnce() } returns SyncStatus.entries.mapIndexed { i,s -> TransactionEntity(id=i+1L,customerId=7,amount=10.0,isPaid=false,syncStatus=s.name) }
        }
    }
    @Test fun reconnectRetriesFailedAndPendingButNeverConflicts()=runTest {
        val f=Fixture(backgroundScope);f.queue();f.repo().syncPendingTransactions()
        coVerify(exactly=2) { f.sync.pushTransaction("fixture",any()) }
        coVerify { f.dao.finishUpload(match { it.id==1L && it.syncStatus=="PENDING" },true) };coVerify { f.dao.finishUpload(match { it.id==3L && it.syncStatus=="FAILED" },true) }
        coVerify(exactly=0) { f.dao.finishUpload(match { it.id==4L },true) }
    }
    @Test fun interruptedUploadPropagatesCancellationWithoutFalseFailureAck()=runTest {
        val f=Fixture(backgroundScope);f.queue()
        coEvery { f.sync.pushTransaction(any(),any()) } throws kotlinx.coroutines.CancellationException("fixture interruption")
        try { f.repo().syncPendingTransactions();org.junit.Assert.fail("Cancellation swallowed") } catch(expected:kotlinx.coroutines.CancellationException) { }
        coVerify(exactly=0) { f.dao.finishUpload(any(),false) };coVerify(exactly=0) { f.dao.finishUpload(any(),true) }
    }
    @Test fun transactionListenerStopsOnLogout()=runTest {
        val f=Fixture(backgroundScope);f.repo();runCurrent()
        org.junit.Assert.assertEquals(1,f.events.subscriptionCount.value)
        f.codes.value="";runCurrent()
        org.junit.Assert.assertEquals(0,f.events.subscriptionCount.value)
    }
    @Test fun failedFirstUploadDoesNotPreventLaterPendingUpload()=runTest {
        val f=Fixture(backgroundScope)
        coEvery { f.dao.getAllOnce() } returns listOf(1L,2L).map { TransactionEntity(id=it,customerId=7,amount=10.0,isPaid=false,syncStatus="PENDING") }
        coEvery { f.sync.pushTransaction(any(),match { it.id==1L }) } throws java.io.IOException("offline fixture")
        f.repo().syncPendingTransactions()
        coVerify { f.dao.finishUpload(match { it.id==1L },false) };coVerify { f.dao.finishUpload(match { it.id==2L },true) }
    }
}
