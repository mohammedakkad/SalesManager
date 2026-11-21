package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class StockFailureBoundaryTest {
    private lateinit var db: AppDatabase
    private val remote=mockk<ProductFirestoreService>(relaxed=true)
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        db.productDao().insertProduct(Product(id="p",name="أرز",merchantId="m1").toEntity())
        db.productDao().insertUnit(ProductUnit("u","p",unitLabel="كيس",quantityInStock=10.0).toEntity())
    }
    @After fun close(){db.close()}
    private fun repo(actor:ActivationRepository?=null)=StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,"m1",db,actor)
    private suspend fun adjust(quantity:Double)=repo().manualAdjust("p","u",quantity,MovementType.INVENTORY_ADJUST,"أرز","كيس","جرد")
    private suspend fun assertUnchanged(expected:Double=10.0){
        assertEquals(expected,db.productDao().getQuantity("u")!!,0.0)
        assertTrue(db.stockMovementDao().getAllOnce().isEmpty())
        coVerify(exactly=0){remote.uploadMovement(any(),any());remote.updateRemoteQuantity(any(),any(),any(),any())}
    }
    @Test fun infiniteAdjustmentIsRejectedBeforeAnyWrite()=runBlocking {
        try {db.withTransaction{adjust(Double.POSITIVE_INFINITY)};fail("Nonfinite stock accepted")}catch(expected:IllegalArgumentException){}
        assertUnchanged()
    }
    @Test fun finiteOperandsWhichOverflowAreRejectedBeforeAnyWrite()=runBlocking {
        db.productDao().updateQuantity("u",Double.MAX_VALUE)
        try {db.withTransaction{adjust(Double.MAX_VALUE)};fail("Overflow accepted")}catch(expected:IllegalArgumentException){}
        assertUnchanged(Double.MAX_VALUE)
    }
    @Test fun ledgerInsertFaultRollsBackStandaloneQuantityChange()=runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_stock BEFORE INSERT ON stock_movements BEGIN SELECT RAISE(ABORT, 'fixture ledger fault'); END")
        try {adjust(2.0);fail("Ledger fault lost")}catch(expected:android.database.sqlite.SQLiteException){}
        assertUnchanged()
    }
    private suspend fun pending(id:String="row",time:Long=1L){
        db.stockMovementDao().insert(StockMovement(id=id,productId="p",unitId="u",quantity=1.0,quantityBefore=9.0,quantityAfter=10.0,merchantId="m1",createdAt=time).toEntity())
    }
    @Test fun movementUploadCancellationPropagatesAndPreservesPendingLedger()=runBlocking {
        pending();val cancellation=CancellationException("provider cancelled movement")
        coEvery{remote.uploadMovement(any(),any())} throws cancellation
        try {repo().syncPendingMovements();fail("Cancellation swallowed")}catch(actual:CancellationException){assertSame(cancellation,actual)}
        assertEquals("PENDING",db.stockMovementDao().getAllOnce().single().syncStatus)
        coVerify(exactly=0){remote.updateRemoteQuantity(any(),any(),any(),any())}
    }
    @Test fun quantityUploadCancellationPropagatesAndKeepsDirtyUnit()=runBlocking {
        val cancellation=CancellationException("provider cancelled quantity")
        coEvery{remote.updateRemoteQuantity(any(),any(),any(),any())} throws cancellation
        try {repo().syncPendingMovements();fail("Cancellation swallowed")}catch(actual:CancellationException){assertSame(cancellation,actual)}
        assertEquals(listOf("u"),db.productDao().getPendingUnits().map{it.id})
        assertTrue(db.stockMovementDao().getAllOnce().isEmpty())
    }
    @Test fun oneOrdinaryUploadFailureDoesNotBlockNextPersistedMovement()=runBlocking {
        pending("first",1);pending("second",2)
        coEvery{remote.uploadMovement("m1",match{it.id=="first"})} throws java.io.IOException("offline first")
        repo().syncPendingMovements()
        assertEquals(mapOf("first" to "FAILED","second" to "SYNCED"),db.stockMovementDao().getAllOnce().associate{it.id to it.syncStatus})
        coVerify(exactly=1){remote.uploadMovement("m1",match{it.id=="second"})}
        assertTrue(db.productDao().getPendingUnits().isEmpty())
    }
    @Test fun blankSessionSkipsAllUploadsAndLeavesPendingRows()=runBlocking {
        pending();val actor=mockk<ActivationRepository>();coEvery{actor.getMerchantCode()} returns ""
        repo(actor).syncPendingMovements()
        assertEquals("PENDING",db.stockMovementDao().getAllOnce().single().syncStatus)
        coVerify(exactly=0){remote.uploadMovement(any(),any());remote.updateRemoteQuantity(any(),any(),any(),any())}
    }
    @Test fun sessionSwitchDuringStartupCheckSkipsAllUploads()=runBlocking {
        pending();val actor=mockk<ActivationRepository>();coEvery{actor.getMerchantCode()} returnsMany listOf("m1","m2")
        repo(actor).syncPendingMovements()
        assertEquals("PENDING",db.stockMovementDao().getAllOnce().single().syncStatus)
        coVerify(exactly=0){remote.uploadMovement(any(),any());remote.updateRemoteQuantity(any(),any(),any(),any())}
    }
}
