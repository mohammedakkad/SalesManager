package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class StockStandaloneUploadTest {
    private lateinit var db:AppDatabase
    private val remote=mockk<ProductFirestoreService>(relaxed=true)
    private var scopeJob:Job?=null
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        db.productDao().insertProduct(Product(id="p",name="أرز",merchantId="m1").toEntity())
        db.productDao().insertUnit(ProductUnit("u","p",unitLabel="كيس",quantityInStock=10.0).toEntity())
    }
    @After fun close(){scopeJob?.cancel();db.close()}
    private suspend fun TestScope.adjustAndAwait():Job {
        val owner=SupervisorJob();scopeJob=owner
        val repo=StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,"m1",db,syncScope=CoroutineScope(owner+StandardTestDispatcher(testScheduler)))
        repo.manualAdjust("p","u",2.0,MovementType.MANUAL_IN,"أرز","كيس","جرد")
        val upload=owner.children.single();upload.join();return upload
    }
    @Test fun standaloneUploadSeesCommittedQuantityAndLedgerThenAcknowledgesBoth()=runTest {
        coEvery{remote.uploadMovement("m1",any())} coAnswers {
            assertFalse(db.inTransaction());assertEquals(12.0,db.productDao().getQuantity("u")!!,0.0)
            val row=db.stockMovementDao().getAllOnce().single();assertEquals(12.0,row.quantityAfter,0.0);assertEquals("PENDING",row.syncStatus)
        }
        val job=adjustAndAwait();assertFalse(job.isCancelled)
        assertEquals("SYNCED",db.stockMovementDao().getAllOnce().single().syncStatus);assertTrue(db.productDao().getPendingUnits().isEmpty())
        coVerifyOrder{remote.uploadMovement("m1",match{it.quantity==2.0&&it.quantityAfter==12.0});remote.updateRemoteQuantity("m1","u",12.0,"p")}
    }
    @Test fun ordinaryMovementFailureKeepsItsRetryRowWhileQuantityUploadCanSucceed()=runTest {
        coEvery{remote.uploadMovement(any(),any())} throws java.io.IOException("offline movement")
        val job=adjustAndAwait();assertFalse(job.isCancelled)
        assertEquals("FAILED",db.stockMovementDao().getAllOnce().single().syncStatus);assertEquals(12.0,db.productDao().getQuantity("u")!!,0.0);assertTrue(db.productDao().getPendingUnits().isEmpty())
        coVerify(exactly=1){remote.updateRemoteQuantity("m1","u",12.0,"p")}
    }
    @Test fun ordinaryQuantityFailureKeepsTheUnitDirtyAfterTheLedgerAcknowledgment()=runTest {
        coEvery{remote.updateRemoteQuantity(any(),any(),any(),any())} throws java.io.IOException("offline quantity")
        val job=adjustAndAwait();assertFalse(job.isCancelled)
        assertEquals("SYNCED",db.stockMovementDao().getAllOnce().single().syncStatus);assertEquals(listOf("u"),db.productDao().getPendingUnits().map{it.id});assertEquals(12.0,db.productDao().getQuantity("u")!!,0.0)
    }
    @Test fun backgroundMovementCancellationKeepsTheRetryRowPendingAndStopsQuantityUpload()=runTest {
        coEvery{remote.uploadMovement(any(),any())} throws CancellationException("cancelled movement")
        val job=adjustAndAwait();assertTrue("Provider cancellation must cancel its upload coroutine",job.isCancelled)
        assertEquals("PENDING",db.stockMovementDao().getAllOnce().single().syncStatus);assertEquals(listOf("u"),db.productDao().getPendingUnits().map{it.id})
        coVerify(exactly=0){remote.updateRemoteQuantity(any(),any(),any(),any())}
    }
    @Test fun backgroundQuantityCancellationPreservesTheCompletedLedgerAndCancelsItsCoroutine()=runTest {
        coEvery{remote.updateRemoteQuantity(any(),any(),any(),any())} throws CancellationException("cancelled quantity")
        val job=adjustAndAwait();assertTrue("Quantity cancellation was swallowed",job.isCancelled)
        assertEquals("SYNCED",db.stockMovementDao().getAllOnce().single().syncStatus);assertEquals(listOf("u"),db.productDao().getPendingUnits().map{it.id})
    }
}
