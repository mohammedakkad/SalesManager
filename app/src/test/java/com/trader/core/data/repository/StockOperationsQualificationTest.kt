package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class StockOperationsQualificationTest {
    private lateinit var db:AppDatabase;private val remote=mockk<ProductFirestoreService>(relaxed=true)
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        db.productDao().insertProduct(Product(id="p",name="أرز",merchantId="m1").toEntity());db.productDao().insertUnit(ProductUnit("u","p",unitLabel="كيس",quantityInStock=10.0).toEntity())
    }
    @After fun close(){db.close()}
    private fun repo()=StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,"m1",db)
    @Test fun localMovementHelpersParticipateInCallerTransactionWithoutUploading()=runBlocking {
        val repo=repo()
        db.withTransaction{repo.deductStock("p","u",2.0,41,"أرز","كيس");repo.returnStock("p","u",1.0,41,"أرز","كيس");repo.manualAdjust("p","u",3.0,MovementType.MANUAL_IN,"أرز","كيس","إضافة")}
        assertEquals(12.0,db.productDao().getQuantity("u")!!,0.0)
        val rows=repo.getMovementsForProduct("p","u").first();assertEquals(3,rows.size);assertEquals(setOf(-2.0,1.0,3.0),rows.map{it.quantity}.toSet())
        assertEquals(2,repo.getMovementsForTransaction(41).first().size);assertTrue(rows.all{it.merchantId=="m1"})
        coVerify(exactly=0){remote.uploadMovement(any(),any());remote.updateRemoteQuantity(any(),any(),any(),any())}
    }
    @Test fun callerRollbackRestoresStockAndLedgerTogether()=runBlocking {
        try{db.withTransaction{repo().deductStock("p","u",2.0,41,"أرز","كيس");throw java.io.IOException("fixture rollback")};fail("Injected failure lost")}catch(expected:java.io.IOException){}
        assertEquals(10.0,db.productDao().getQuantity("u")!!,0.0);assertTrue(db.stockMovementDao().getAllOnce().isEmpty())
    }
    @Test fun failedStockLedgerRetriesWithoutRewritingItsPendingState()=runBlocking {
        val movement=StockMovement(id="m",productId="p",productName="أرز",unitId="u",unitLabel="كيس",quantity=-1.0,quantityBefore=11.0,quantityAfter=10.0,merchantId="m1")
        db.stockMovementDao().insert(movement.toEntity());coEvery{remote.uploadMovement(any(),any())} throws java.io.IOException("offline")
        coEvery{remote.updateRemoteQuantity(any(),any(),any(),any())} throws java.io.IOException("offline")
        val repo=repo();repo.syncPendingMovements();assertEquals("FAILED",db.stockMovementDao().getAllOnce().single().syncStatus);assertEquals(1,db.productDao().getPendingUnits().size)
        // Retry the same persisted FAILED record, without resetting it in the fixture.
        coEvery{remote.uploadMovement(any(),any())} just Runs;coEvery{remote.updateRemoteQuantity(any(),any(),any(),any())} just Runs
        repo.syncPendingMovements();assertEquals("SYNCED",db.stockMovementDao().getAllOnce().single().syncStatus);assertTrue(db.productDao().getPendingUnits().isEmpty())
    }
    @Test fun conflictsAreReportedAndOnlyExplicitChoiceChangesLocalOrRemoteQuantity()=runBlocking {
        val repo=repo();coEvery{remote.getRemoteQuantity("m1","u","p")} returns 7.0
        val conflict=repo.detectConflicts().single();assertEquals(10.0,conflict.localQuantity,0.0);assertEquals(7.0,conflict.remoteQuantity,0.0)
        assertEquals(10.0,db.productDao().getQuantity("u")!!,0.0);coVerify(exactly=0){remote.updateRemoteQuantity(any(),any(),any(),any())}
        repo.resolveConflictWithLocal("u");coVerify{remote.updateRemoteQuantity("m1","u",10.0,"p")}
        repo.resolveConflictWithServer("u");assertEquals(7.0,db.productDao().getQuantity("u")!!,0.0);assertTrue(repo.detectConflicts().isEmpty())
        repo.resolveConflictWithServer("missing");repo.resolveConflictWithLocal("missing");assertNull(db.productDao().getQuantity("missing"))
    }
    @Test fun failedStockRetryDoesNotUploadAnotherMerchantsLedger()=runBlocking {
        val actor=mockk<com.trader.core.domain.repository.ActivationRepository>()
        coEvery{actor.getMerchantCode()} returns "m1"
        val own=StockMovement(id="own",productId="p",unitId="u",unitLabel="كيس",quantity=1.0,merchantId="m1",syncStatus=SyncStatus.FAILED)
        db.stockMovementDao().insertAll(listOf(own.toEntity(),own.copy(id="foreign",merchantId="m2").toEntity()))
        val guarded=StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,"m1",db,actor)
        guarded.syncPendingMovements()
        assertEquals("SYNCED",db.stockMovementDao().getAllOnce().first{it.id=="own"}.syncStatus)
        assertEquals("FAILED",db.stockMovementDao().getAllOnce().first{it.id=="foreign"}.syncStatus)
        coVerify(exactly=1){remote.uploadMovement("m1",match{it.id=="own"})}
        coVerify(exactly=0){remote.uploadMovement(any(),match{it.id=="foreign"})}
    }

}
