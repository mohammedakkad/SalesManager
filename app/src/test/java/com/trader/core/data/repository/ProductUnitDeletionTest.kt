package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.util.NetworkMonitor
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ProductUnitDeletionTest {
    private lateinit var db:AppDatabase
    private val remote=mockk<ProductFirestoreService>(relaxed=true)
    private val activation=mockk<ActivationRepository>(relaxed=true);private val network=mockk<NetworkMonitor>(relaxed=true)
    @Before fun setup(){
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        every{activation.observeMerchantCode()} returns emptyFlow();coEvery{activation.getMerchantCode()} returns "m1";every{network.isOnlineFlow} returns flowOf(false)
    }
    @After fun close(){db.close()}
    @Test fun deletingUnitDispatchesItsActualParentProductId()=runBlocking {
        db.productDao().insertProduct(Product(id="parent",name="أرز",merchantId="m1").toEntity())
        db.productDao().insertUnit(ProductUnit("unit","parent",unitLabel="كيس").toEntity())
        val delivered=CompletableDeferred<Triple<String,String,String>>()
        coEvery{remote.deleteUnit(any(),any(),any())} coAnswers {delivered.complete(Triple(firstArg(),secondArg(),thirdArg()));Unit}
        val repo=ProductRepositoryImpl(db.productDao(),db,remote,activation,network)
        repo.deleteUnit("unit");assertTrue(db.productDao().getUnitsForProduct("parent").isEmpty())
        val call=withTimeoutOrNull(3000){delivered.await()}
        assertEquals("Deleting a unit must dispatch deletion with the parent captured before local removal",Triple("m1","unit","parent"),call)
    }
}
