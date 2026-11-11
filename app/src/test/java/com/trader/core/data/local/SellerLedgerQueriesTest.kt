package com.trader.core.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.repository.DashboardAnalyticsRepositoryImpl
import com.trader.core.domain.model.*
import com.trader.core.sync.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class SellerLedgerQueriesTest {
    private lateinit var db:AppDatabase
    @Before fun open(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()}
    @After fun close(){db.close()}
    private fun stock(id:String,time:Long=1)=StockMovementEntity(id,"p","أرز","u","كيس","SALE_OUT",1.5,5.0,3.5,41,"fixture","m1",time,"PENDING")
    private fun cash(id:String,box:String="box",time:Long=1)=CashBoxMovementEntity(id,box,"نقد","TRANSACTION_EFFECT",20.0,120.0,relatedTransactionId=41,createdAt=time,merchantId="m1")
    @Test fun stockLedgerQueriesKeepChronologyTransactionAndUnitIdentity()=runTest {
        val d=db.stockMovementDao();d.insert(stock("first"));d.insertAll(listOf(stock("later",2),stock("other",3).copy(unitId="v",relatedTransactionId=42)))
        assertEquals(listOf("later","first"),d.getForProductUnit("p","u").first().map{it.id})
        assertEquals(listOf("later","first"),d.getForTransaction(41).first().map{it.id})
        assertEquals(setOf("first","later"),d.forTransactionOnce(41).map{it.id}.toSet())
        assertEquals(listOf("first","later","other"),d.getAllOnce().map{it.id});assertEquals(3,d.getPending().size)
        d.markFailed("later");d.markSynced("first");assertEquals(2,d.observeUnsyncedCount().first());assertEquals(listOf("other","later"),d.observeUnsynced().first().map{it.id})
        assertEquals(listOf("later","other"),d.getPending().map{it.id});d.deleteAll();assertTrue(d.getAllOnce().isEmpty())
    }
    @Test fun cashLedgerFailedRowsRemainRetryableAndBoxDeletionIsScoped()=runTest {
        val d=db.cashBoxMovementDao();d.insert(cash("a"));d.insertAll(listOf(cash("b",time=2),cash("c","other",3)))
        assertEquals(listOf("c","b","a"),d.getAll().first().map{it.id});assertEquals(listOf("b","a"),d.getByCashBoxId("box").first().map{it.id})
        assertEquals(3,d.forTransactionOnce(41).size);assertEquals(20.0,d.getById("a")!!.amountDelta,0.0);assertNull(d.getById("missing"))
        d.markFailed("b");d.markSynced("a");assertEquals(setOf("b","c"),d.getPending().map{it.id}.toSet());assertEquals(2,d.observeUnsyncedCount().first())
        assertEquals(listOf("c","b"),d.observeUnsynced().first().map{it.id});d.deleteByCashBoxId("box");assertEquals(listOf("c"),d.getAllOnce().map{it.id})
        d.deleteAll();assertTrue(d.getAllOnce().isEmpty())
    }
    @Test fun cashBoxInitialBalanceDeltaAndSyncStatusDoNotChangeOtherBox()=runTest {
        val d=db.cashBoxDao();d.upsertAll(listOf(CashBoxEntity("a",1,"نقد",merchantId="m1"),CashBoxEntity("b",2,"بطاقة",merchantId="m1")))
        d.setInitialBalanceFields("a",100.0,1,1);d.applyDelta("a",-25.0,2);assertEquals(75.0,d.getById("a")!!.currentBalance,0.0)
        assertEquals(100.0,d.getById("a")!!.initialBalance,0.0);assertEquals(0.0,d.getById("b")!!.currentBalance,0.0)
        assertEquals("a",d.getByPaymentMethodId(1)!!.id);assertNull(d.getByPaymentMethodId(999));assertEquals(2,d.getAll().first().size)
        d.markFailed("a");assertEquals(2,d.observeUnsyncedCount().first());assertEquals(2,d.observeUnsynced().first().size);assertEquals(2,d.getPending().size)
        d.markSynced("a");assertEquals(listOf("b"),d.getPending().map{it.id});d.updateBalance("b",15.0,3);assertEquals(15.0,d.getById("b")!!.currentBalance,0.0)
        d.deleteById("a");assertNull(d.getById("a"));d.deleteAll();assertTrue(d.getAllOnce().isEmpty())
    }
    @Test fun physicalCountSessionsKeepItemsScopedAndCompletionStopsActiveSession()=runTest {
        val d=db.inventoryDao();val a=InventorySession(id="s",merchantId="m1",startedAt=1).toEntity()
        d.insertAllSessions(listOf(a));d.insertSession(a.copy(id="old",status="FINISHED",startedAt=0))
        val item=InventorySessionItemEntity("i","s","p","أرز","u","كيس",10.0,null)
        d.insertSessionItems(listOf(item,item.copy(id="j",sessionId="old",actualQuantity=9.0)))
        assertEquals("s",d.getActiveSession().first()!!.id);assertEquals(listOf("i"),d.getSessionItems("s").first().map{it.id});assertEquals(1,d.getSessionItemsOnce("old").size)
        d.updateSessionItem(item.copy(actualQuantity=8.0));assertEquals(8.0,d.getSessionItemsOnce("s").single().actualQuantity!!,0.0)
        d.updateSession(a.copy(status="FINISHED",finishedAt=2,totalAdjustments=1));assertNull(d.getActiveSession().first());assertEquals(1,d.getSessionById("s")!!.totalAdjustments)
        assertEquals(listOf("s","old"),d.getAllSessions().first().map{it.id});assertEquals(2,d.getAllSessionsOnce().size);assertEquals(2,d.getAllSessionItemsOnce().size)
        d.deleteAllSessionItems();d.deleteAllSessions();assertTrue(d.getAllSessionItemsOnce().isEmpty());assertNull(d.getSessionById("s"))
    }
    @Test fun invoiceQueriesRetainSnapshotValuesAndReportRankLimits()=runTest {
        db.customerDao().insertCustomer(CustomerEntity(1,"زبون"));db.transactionDao().insertTransaction(TransactionEntity(41,1,50.0,true,date=100))
        val d=db.invoiceItemDao();val a=InvoiceItemEntity("i",41,"p","أرز","u","كيس",2.0,20.0,40.0,"m1","PENDING")
        d.insertInvoiceItem(a);d.insertAll(listOf(a.copy(id="j",productId="q",productName="سكر",quantity=1.0,pricePerUnit=10.0,totalPrice=10.0)))
        assertEquals(listOf("i","j"),d.getForTransaction(41).first().map{it.id});assertEquals(2,d.getForTransactionOnce(41).size);assertEquals(40.0,d.getById("i")!!.totalPrice,0.0)
        assertNull(d.getById("missing"));val repo=DashboardAnalyticsRepositoryImpl(db.transactionDao(),d)
        assertEquals("p",repo.observeTopSellingProducts(100,100,1).first().single().productId)
        assertTrue(repo.observeTopSellingProducts(101,200,5).first().isEmpty())
        d.markFailed("j");d.markSynced("i");assertEquals(1,d.observeUnsyncedCount().first());assertTrue(d.getPending().isEmpty())
        d.deleteForTransaction(41);assertTrue(d.getAllOnce().isEmpty());d.insertInvoiceItem(a);d.deleteAll();assertTrue(d.getAllOnce().isEmpty())
    }
    @Test fun lowStockNotificationsRememberAcknowledgmentAndDropOnlyOrphans()=runTest {
        db.productDao().insertProduct(Product(id="p",name="أرز",merchantId="m1").toEntity());db.productDao().insertUnit(ProductUnit("u","p",unitLabel="كيس").toEntity())
        val d=db.lowStockAlertDao();d.upsertAll(listOf(LowStockAlertStateEntity("u","LOW",null,null),LowStockAlertStateEntity("orphan","OUT",null,null)))
        d.markNotified("u","LOW",100);d.deleteOrphans();val row=d.getAll().single()
        assertEquals("u",row.unitId);assertEquals("LOW",row.lastNotifiedStatus);assertEquals(100L,row.lastNotifiedAt)
    }
    @Test fun syncObserverIncludesLedgerFailuresAndReturnsSyncedOnlyAfterAcknowledgment()=runTest {
        val observer=SyncStatusObserver(db.transactionDao(),db.cashBoxDao(),db.cashBoxMovementDao(),db.stockMovementDao(),db.invoiceItemDao(),db.saleOutboxDao())
        assertEquals(GlobalSyncState.AllSynced,observer.globalSyncState.first())
        db.stockMovementDao().insert(stock("a"));db.cashBoxDao().upsert(CashBoxEntity("box",1,"نقد",syncStatus="PENDING"));db.cashBoxMovementDao().insert(cash("cash"))
        assertEquals(GlobalSyncState.Pending(3),observer.globalSyncState.first());db.stockMovementDao().markFailed("a")
        assertEquals(GlobalSyncState.Failed(3),observer.globalSyncState.first());val items=observer.unsyncedItems.first();assertEquals("a",items.first().id);assertTrue(items.first().label.contains("أرز"))
        db.stockMovementDao().markSynced("a");db.cashBoxDao().markSynced("box");db.cashBoxMovementDao().markSynced("cash")
        assertEquals(GlobalSyncState.AllSynced,observer.globalSyncState.first())
    }
}
