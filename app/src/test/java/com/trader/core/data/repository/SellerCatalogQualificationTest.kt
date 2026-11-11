package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
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
class SellerCatalogQualificationTest {
    private lateinit var db:AppDatabase;private val remote=mockk<ProductFirestoreService>(relaxed=true)
    private val activation=mockk<ActivationRepository>(relaxed=true);private val network=mockk<NetworkMonitor>(relaxed=true)
    @Before fun setup(){
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        every{activation.observeMerchantCode()} returns emptyFlow();coEvery{activation.getMerchantCode()} returns "m1";every{network.isOnlineFlow} returns flowOf(false)
        coEvery{remote.uploadProduct(any(),any())} throws java.io.IOException("offline fixture");coEvery{remote.uploadUnit(any(),any())} throws java.io.IOException("offline fixture")
    }
    @After fun close(){db.close()}
    private fun repo()=ProductRepositoryImpl(db.productDao(),db,remote,activation,network)
    @Test fun offlineProductCreationAssignsParentDefaultUnitAndPendingIntent()=runBlocking {
        val repo=repo();val id=repo.saveProduct(Product(id="",name="أرز",barcode="111"),listOf(ProductUnit(id="",productId="",unitLabel="كيس",price=20.0,quantityInStock=2.0)),emptySet())
        val loaded=repo.getProductById(id)!!;assertTrue(id.isNotBlank());assertEquals("m1",loaded.product.merchantId);assertEquals(SyncStatus.PENDING,loaded.product.syncStatus)
        assertEquals(id,loaded.units.single().productId);assertTrue(loaded.units.single().isDefault);assertTrue(loaded.units.single().id.isNotBlank())
        assertEquals("أرز",repo.getBarcodeConflict("111",null));assertNull(repo.getBarcodeConflict("111",id));assertNull(repo.getBarcodeConflict("",null));assertNull(repo.getBarcodeConflict("absent",null))
        assertEquals(id,repo.getProductByBarcode("111")!!.product.id);assertNull(repo.getProductByBarcode("missing"))
    }
    @Test fun stockFiltersAndSearchUseActualUnitsWithoutMixingProducts()=runBlocking {
        val dao=db.productDao()
        for((id,qty) in listOf("low" to 1.0,"out" to 0.0,"ok" to 10.0)) {
            dao.insertProduct(Product(id=id,name=id,merchantId="m1").toEntity());dao.insertUnit(ProductUnit("u$id",id,unitLabel="كيس",quantityInStock=qty,lowStockThreshold=2.0).toEntity())
        }
        val repo=repo();assertEquals(3,repo.getAllProducts().first().size);assertEquals(listOf("low"),repo.getLowStockProducts().first().map{it.product.id})
        assertEquals(listOf("out"),repo.getOutOfStockProducts().first().map{it.product.id});assertEquals(listOf("ok"),repo.searchProducts("ok").first().map{it.product.id})
    }
    @Test fun unitAndProductEditsPersistLocallyWhenTransportFails()=runBlocking {
        val product=Product(id="p",name="قديم",merchantId="m1");db.productDao().insertProduct(product.toEntity());val repo=repo()
        repo.saveUnit(ProductUnit("u","p",unitLabel="كيس",price=10.0));repo.updateUnit(ProductUnit("u","p",unitLabel="كيس",price=12.0));repo.updateProduct(product.copy(name="جديد"))
        val loaded=repo.getProductById("p")!!;assertEquals("جديد",loaded.product.name);assertEquals(12.0,loaded.units.single().price,0.0);assertEquals(SyncStatus.PENDING,loaded.units.single().syncStatus)
        repo.deleteProduct("p");assertNull(repo.getProductById("p"));assertTrue(db.productDao().getUnitsForProduct("p").isEmpty())
    }
    @Test fun pendingUploadFailureKeepsRowsAndLaterSuccessAcknowledgesEachRecord()=runBlocking {
        db.productDao().insertProduct(Product(id="p",name="أرز",merchantId="m1").toEntity());db.productDao().insertUnit(ProductUnit("u","p",unitLabel="كيس").toEntity())
        val repo=repo();repo.syncPendingProducts();assertEquals(1,db.productDao().getPendingProducts().size);assertEquals(1,db.productDao().getPendingUnits().size)
        coEvery{remote.uploadProduct(any(),any())} just Runs;coEvery{remote.uploadUnit(any(),any())} just Runs
        repo.syncPendingProducts();assertTrue(db.productDao().getPendingProducts().isEmpty());assertTrue(db.productDao().getPendingUnits().isEmpty())
        coVerify{remote.uploadProduct("m1",match{it.id=="p"})};coVerify{remote.uploadUnit("m1",match{it.id=="u"})}
    }
    @Test fun lowStockCooldownEscalationAndRestockRecoveryFollowPersistedState()=runBlocking {
        val dao=db.productDao();dao.insertProduct(Product(id="p",name="أرز",merchantId="m1").toEntity());dao.insertUnit(ProductUnit("u","p",unitLabel="كيس",quantityInStock=1.0,lowStockThreshold=2.0).toEntity())
        val alerts=LowStockAlertRepositoryImpl(db,dao,db.lowStockAlertDao())
        val initial=alerts.getNotificationCandidates(100,1000);assertEquals(StockLevel.LOW,initial.single().status);alerts.markNotified(initial,100)
        assertTrue(alerts.getNotificationCandidates(101,1000).isEmpty());dao.updateQuantity("u",0.0,102)
        val escalated=alerts.getNotificationCandidates(102,1000);assertEquals(StockLevel.OUT,escalated.single().status);alerts.markNotified(escalated,102)
        assertTrue(alerts.getNotificationCandidates(103,1000).isEmpty());assertEquals(1,alerts.getNotificationCandidates(1102,1000).size)
        dao.updateQuantity("u",10.0,1103);assertTrue(alerts.getNotificationCandidates(1103,1000).isEmpty());assertNull(db.lowStockAlertDao().getAll().single().lastNotifiedAt)
        dao.updateQuantity("u",1.0,1104);assertEquals(1,alerts.getNotificationCandidates(1104,1000).size)
    }
}
