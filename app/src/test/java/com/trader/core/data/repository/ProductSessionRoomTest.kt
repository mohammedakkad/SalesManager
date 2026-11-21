package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.util.NetworkMonitor
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProductSessionRoomTest {
    private lateinit var db:AppDatabase
    private val remote=mockk<ProductFirestoreService>()
    private val activation=mockk<ActivationRepository>()
    private val network=mockk<NetworkMonitor>()
    private val actor=MutableStateFlow("m1")
    private val snapshots=MutableSharedFlow<List<Product>>()
    private val product=Product("p",name="offline fixture",merchantId="m1",updatedAt=1,syncStatus=SyncStatus.SYNCED)
    private val unit=ProductUnit("u","p",unitLabel="piece",price=25.0,costPrice=12.0,quantityInStock=4.0,syncStatus=SyncStatus.SYNCED)
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        every{activation.observeMerchantCode()} returns actor;coEvery{activation.getMerchantCode()} answers {actor.value}
        every{network.isOnlineFlow} returns flowOf(false);every{remote.observeProducts(any())} returns snapshots
    }
    @After fun cleanup(){db.close()}
    private fun TestScope.repo()=ProductRepositoryImpl(db.productDao(),db,remote,activation,network,useOutboxFence=true,syncScope=backgroundScope)
    @Test fun logoutCancelsTheCatalogListenerInsteadOfFilteringOutTheSignal()=runTest {
        repo();runCurrent();assertEquals(1,snapshots.subscriptionCount.value)
        actor.value="";runCurrent();assertEquals(0,snapshots.subscriptionCount.value)
    }
    @Test fun lateNonCooperativeUnitReadCannotWriteAfterLogout()=runTest {
        val gate=CompletableDeferred<Unit>();val started=CompletableDeferred<Job>()
        coEvery{remote.fetchUnitsForProduct("m1","p")} coAnswers {
            started.complete(currentCoroutineContext().job)
            withContext(NonCancellable){gate.await()};listOf(unit)
        }
        try {
            repo();runCurrent();snapshots.emit(listOf(product));runCurrent();val work=started.await()
            actor.value="";runCurrent();gate.complete(Unit);work.join();runCurrent()
            assertTrue("A stale account callback must not create a product",db.productDao().getAllWithUnits().first().isEmpty())
        } finally {gate.complete(Unit)}
    }
    @Test fun failedUnitReadCannotCommitAProductWithoutItsRequestedUnits()=runTest {
        val started=CompletableDeferred<Job>()
        coEvery{remote.fetchUnitsForProduct("m1","p")} coAnswers {started.complete(currentCoroutineContext().job);throw IOException("offline fixture")}
        repo();runCurrent();snapshots.emit(listOf(product));runCurrent();started.await().join();runCurrent()
        assertTrue("Network failure is not a valid partial catalog snapshot",db.productDao().getAllWithUnits().first().isEmpty())
    }
    @Test fun successfulSnapshotKeepsTheCostStockAndParentInOneDatabase()=runTest {
        val started=CompletableDeferred<Job>()
        coEvery{remote.fetchUnitsForProduct("m1","p")} coAnswers {started.complete(currentCoroutineContext().job);listOf(unit)}
        repo();runCurrent();snapshots.emit(listOf(product));runCurrent();started.await().join();runCurrent()
        val row=db.productDao().getAllWithUnits().first().single();assertEquals("p",row.product.id)
        assertEquals(12.0,row.units.single().costPrice,0.0);assertEquals(4.0,row.units.single().quantityInStock,0.0)
    }
}
