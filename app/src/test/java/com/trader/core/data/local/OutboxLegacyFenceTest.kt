package com.trader.core.data.local

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.*
import com.trader.core.data.repository.*
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class OutboxLegacyFenceTest {
    private lateinit var db:AppDatabase
    private lateinit var scope:CoroutineScope
    private val sync=mockk<FirebaseSyncService>(relaxed=true)
    private val remote=mockk<ProductFirestoreService>(relaxed=true)
    private val activation=mockk<ActivationRepository>()
    private var actor="fixture"
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        coEvery { activation.getMerchantCode() } answers { actor }
        every { activation.observeMerchantCode() } returns emptyFlow()
        db.productDao().insertProduct(ProductEntity("product",null,"fixture","",null,"fixture",0,0,"PENDING"))
        db.productDao().insertUnit(ProductUnitEntity("unit","product","PIECE","piece",25.0,quantityInStock=4.0,
            itemsPerCarton=null,lowStockThreshold=0.0,isDefault=true,createdAt=0,updatedAt=0,syncStatus="PENDING"))
        db.stockMovementDao().insert(StockMovementEntity("movement","product","fixture","unit","piece","SALE_OUT",-1.0,5.0,4.0,8,"","fixture",0,"PENDING"))
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"cash",currentBalance=100.0,merchantId="fixture",updatedAt=0))
        db.cashBoxMovementDao().insert(CashBoxMovementEntity("cash-movement","1","cash","TRANSACTION_EFFECT",10.0,100.0,
            relatedTransactionId=8,createdAt=0,merchantId="fixture"))
    }
    @After fun close()=runBlocking { scope.coroutineContext.job.cancelAndJoin();db.close() }
    private suspend fun enqueue() { db.saleOutboxDao().insert(SaleOutboxEntity(eventId="event",operationId="operation",merchantId="fixture",
        aggregateId=8,aggregateRevision=1,eventType="CREATE",payloadVersion=1,payload="{}",createdAt=0,updatedAt=0)) }
    @Test fun pendingOutboxPreventsIndependentLegacyStockUpload()=runBlocking {
        enqueue()
        StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,"fixture",db,activation).syncPendingMovements()
        coVerify(exactly=0) { remote.uploadMovement(any(),any()) };coVerify(exactly=0) { remote.updateRemoteQuantity(any(),any(),any(),any()) }
        assertEquals("PENDING",db.stockMovementDao().getAllOnce().single().syncStatus)
    }
    @Test fun foreignStockAndUnitsAreNotUploadedUnderTheNextMerchant()=runBlocking {
        actor="other"
        StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,"fixture",db,activation).syncPendingMovements()
        coVerify(exactly=0) { remote.uploadMovement(any(),any()) };coVerify(exactly=0) { remote.updateRemoteQuantity(any(),any(),any(),any()) }
        assertEquals(4.0,db.productDao().getQuantity("unit")!!,0.0)
        assertEquals("PENDING",db.productDao().getUnitsForProduct("product").single().syncStatus)
    }
    @Test fun foreignCashRowsAreNotRetargetedOnAccountSwitch()=runBlocking {
        actor="other"
        CashBoxRepositoryImpl(db,db.cashBoxDao(),db.cashBoxMovementDao(),db.paymentMethodDao(),sync,activation,scope).syncPendingBoxes()
        coVerify(exactly=0) { sync.pushCashBox(any(),any()) };coVerify(exactly=0) { sync.pushCashBoxMovement(any(),any()) }
        assertEquals("fixture",db.cashBoxDao().getById("1")!!.merchantId)
    }
    @Test fun foreignPendingInvoiceIsNotRetargetedOnAccountSwitch()=runBlocking {
        actor="other"
        db.invoiceItemDao().insertInvoiceItem(InvoiceItemEntity("line",8,"product","fixture","unit","piece",1.0,25.0,25.0,"fixture","PENDING"))
        InvoiceItemRepositoryImpl(db.invoiceItemDao(),remote,sync,"fixture",db,activation).syncPendingItems()
        coVerify(exactly=0) { remote.uploadInvoiceItems(any(),any()) };coVerify(exactly=0) { sync.pushInvoiceItems(any(),any()) }
        assertEquals("PENDING",db.invoiceItemDao().getById("line")!!.syncStatus)
    }
    @Test fun nestedCashEffectRemainsPendingForItsOwningOutbox()=runBlocking {
        val repo=CashBoxRepositoryImpl(db,db.cashBoxDao(),db.cashBoxMovementDao(),db.paymentMethodDao(),sync,activation,scope)
        db.withTransaction { repo.applyTransactionEffect(8,null,0.0,false,1,50.0,true);enqueue() }
        repo.syncPendingBoxes()
        assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        coVerify(exactly=0) { sync.pushCashBox(any(),any()) };coVerify(exactly=0) { sync.pushCashBoxMovement(any(),any()) }
    }
    @Test fun standaloneCashEffectUploadsAfterCommitAndSeesCommittedBalance()=runBlocking {
        val box=CompletableDeferred<Unit>();val movement=CompletableDeferred<Unit>()
        coEvery { sync.pushCashBox(any(),any()) } coAnswers {
            assertFalse(db.inTransaction());assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0);box.complete(Unit);Unit
        }
        coEvery { sync.pushCashBoxMovement(any(),any()) } coAnswers {
            assertFalse(db.inTransaction());assertEquals(150.0,secondArg<com.trader.core.domain.model.CashBoxMovement>().balanceAfter,0.0);movement.complete(Unit);Unit
        }
        val repo=CashBoxRepositoryImpl(db,db.cashBoxDao(),db.cashBoxMovementDao(),db.paymentMethodDao(),sync,activation,scope)
        repo.applyTransactionEffect(8,null,0.0,false,1,50.0,true)
        withTimeout(10000) { box.await();movement.await() }
    }
}
