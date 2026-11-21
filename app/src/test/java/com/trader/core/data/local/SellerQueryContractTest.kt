package com.trader.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.repository.DashboardAnalyticsRepositoryImpl
import com.trader.core.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real SQLite query contracts used by Seller; no production Firebase. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=android.app.Application::class)
class SellerQueryContractTest {
    private lateinit var db:AppDatabase
    @Before fun open() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() { db.close() }
    private suspend fun sales():List<TransactionEntity> {
        db.customerDao().insertAll(listOf(CustomerEntity(1,"ليلى"),CustomerEntity(2,"سامر")))
        val rows=listOf(TransactionEntity(11,1,10.0,false,date=100),TransactionEntity(12,1,20.0,true,date=200),TransactionEntity(13,2,30.0,false,date=200),TransactionEntity(14,1,40.0,false,date=300))
        db.transactionDao().insertAll(rows);return rows
    }
    @Test fun transactionListsRespectRangePaidStatusCustomerAndLimit()=runTest {
        sales();val d=db.transactionDao()
        assertEquals(listOf(14L,12L,13L,11L).toSet(),d.getAllTransactions().first().map{it.id}.toSet())
        assertEquals(1,d.observeRecentTransactions(1).first().size);assertEquals(14,d.observeRecentTransactions(1).first().single().id)
        assertEquals(setOf(11L,12L,14L),d.getTransactionsByCustomer(1).first().map{it.id}.toSet())
        assertEquals(setOf(12L,13L),d.getTransactionsByDate(200,200).first().map{it.id}.toSet())
        assertEquals(listOf(11L,13L,14L),d.getUnpaidTransactions().first().map{it.id})
        assertEquals(listOf(11L,13L),d.getUnpaidOlderThan(200).map{it.id})
        assertEquals(30.0,d.observeTransactionById(13).first()!!.amount,0.0)
        assertNull(d.observeTransactionById(999).first())
    }
    @Test fun reportTotalsDebtAndHistoricalBalanceDoNotDoubleCountPaidRows()=runTest {
        sales();val d=db.transactionDao()
        assertEquals(50.0,d.getTotalAmountByDate(200,200),0.0);assertEquals(20.0,d.getPaidAmountByDate(200,200),0.0)
        assertEquals(50.0,d.getUnpaidAmountByCustomer(1),0.0)
        assertEquals(10.0,d.getUnpaidAmountBeforeTransaction(1,300,14),0.0)
        assertEquals(0.0,d.getUnpaidAmountBeforeTransaction(2,200,13),0.0)
        assertEquals(0.0,d.getTotalAmountByDate(400,500),0.0)
        val repo=DashboardAnalyticsRepositoryImpl(d,db.invoiceItemDao())
        val summary=repo.observeTodaySalesSummary(100,200).first()
        assertEquals(TodaySalesSummary(60.0,3,20.0,40.0),summary)
        assertEquals(80.0,repo.observeTotalOutstandingDebt().first(),0.0)
        val debtors=repo.observeTopDebtorCustomers(1).first();assertEquals(1,debtors.size);assertEquals(1,debtors.single().customerId);assertEquals(50.0,debtors.single().totalDebt,0.0)
    }
    @Test fun sevenDayBucketsUseHalfOpenBoundariesAndIncludeEmptyDays()=runTest {
        sales();val repo=DashboardAnalyticsRepositoryImpl(db.transactionDao(),db.invoiceItemDao())
        val values=repo.observeLastSevenDaysSales(listOf(0,100,200,300,400,500,600,700).map{it.toLong()}).first()
        assertEquals(listOf(0.0,10.0,50.0,40.0,0.0,0.0,0.0),values.map{it.totalSales})
        assertEquals(listOf(0L,100L,200L,300L,400L,500L,600L),values.map{it.dayStartMillis})
        try { repo.observeLastSevenDaysSales(listOf(0));fail("Missing calendar boundaries accepted") } catch(expected:IllegalArgumentException) {}
    }
    @Test fun debtAgingBoundariesAndCountsAreDisjoint()=runTest {
        sales();val aging=db.transactionDao().observeDebtAging(250,150,50).first()
        assertEquals(40.0,aging.lessThanWeekAmount,0.0);assertEquals(30.0,aging.oneWeekToOneMonthAmount,0.0)
        assertEquals(10.0,aging.oneMonthToThreeMonthsAmount,0.0);assertEquals(0.0,aging.moreThanThreeMonthsAmount,0.0)
        assertEquals(listOf(1,1,1,0),listOf(aging.lessThanWeekCount,aging.oneWeekToOneMonthCount,aging.oneMonthToThreeMonthsCount,aging.moreThanThreeMonthsCount))
    }
    @Test fun transactionStatusQueriesTrackConflictsAndRemoval()=runTest {
        val rows=sales();val d=db.transactionDao();d.markFailed(11);d.markConflicting(12)
        assertEquals(2,d.observeUnsyncedCount().first());assertEquals(setOf(11L,12L),d.observeUnsynced().first().map{it.id}.toSet())
        d.markSynced(11);assertEquals(1,d.observeUnsyncedCount().first())
        d.updateTransaction(rows[3].copy(amount=45.0));assertEquals(45.0,d.getTransactionById(14)!!.amount,0.0)
        d.deleteTransaction(rows[2]);d.deleteById(12);assertEquals(setOf(11L,14L),d.getAllIds().toSet())
        d.recalculateHasItems();assertFalse(d.getTransactionById(14)!!.hasItems)
        d.deleteAll();assertTrue(d.getAllOnce().isEmpty())
    }
    @Test fun productRelationsSearchBarcodeAndValuationKeepUnitIdentity()=runTest {
        val d=db.productDao()
        val p=Product(id="p",name="أرز",barcode="111",merchantId="fixture").toEntity()
        val units=listOf(ProductUnit("u","p",unitLabel="كيس",price=20.0,costPrice=10.0,quantityInStock=3.0),ProductUnit("v","p",unitLabel="علبة",price=5.0,costPrice=2.0,quantityInStock=0.0)).map{it.toEntity()}
        d.upsertProductWithUnits(p,units)
        assertEquals("p",d.getByBarcode("111")!!.product.id);assertNull(d.getByBarcode("absent"))
        assertEquals(listOf("p"),d.searchWithUnits("رز").first().map{it.product.id});assertTrue(d.searchWithUnits("غير موجود").first().isEmpty())
        assertEquals(2,d.getAllWithUnits().first().single().units.size)
        val value=d.observeInventoryValue().first();assertEquals(30.0,value.costValue,0.0);assertEquals(60.0,value.saleValue,0.0)
        assertEquals("p",d.getProductIdForUnit("u"));assertNull(d.getProductIdForUnit("missing"));assertNull(d.getQuantity("missing"))
    }
    @Test fun catalogEditsAndPendingAcknowledgmentsPreserveRetainedUnits()=runTest {
        val d=db.productDao();val p=Product(id="p",name="سكر",merchantId="fixture").toEntity()
        d.insertProducts(listOf(p));val a=ProductUnit("u","p",unitLabel="كيس",price=10.0,quantityInStock=2.0).toEntity()
        d.insertUnit(a);d.insertUnit(a.copy(id="removed"));d.updateProduct(p.copy(category="غذاء"));d.updateUnit(a.copy(price=12.0))
        assertEquals(12.0,d.getUnitsForProduct("p").first{it.id=="u"}.price,0.0)
        assertEquals(1,d.getPendingProducts().size);assertEquals(2,d.getPendingUnits().size)
        d.markProductSynced("p");d.markUnitSynced("u");assertTrue(d.getPendingProducts().isEmpty());assertEquals(1,d.getPendingUnits().size)
        d.deleteRemovedUnits("p",listOf("u"));assertEquals(listOf("u"),d.getUnitsForProduct("p").map{it.id})
        d.deleteUnit("u");assertTrue(d.getUnitsForProduct("p").isEmpty());d.deleteProduct("p");assertNull(d.getById("p"))
        d.insertProduct(p);d.deleteAllProducts();assertTrue(d.getAllWithUnitsOnce().isEmpty())
    }
    @Test fun employeeQueriesPartitionMerchantPinsAndAcknowledgments()=runTest {
        val d=db.employeeDao();val a=EmployeeEntity("a","m1","سارة","1234","CASHIER",1)
        d.insertAll(listOf(a,a.copy(id="b",merchantId="m2",createdAt=2)))
        assertEquals(1,d.countByMerchant("m1"));assertEquals("a",d.getByPin("m1","1234")!!.id);assertEquals("b",d.getByPin("m2","1234")!!.id)
        assertNull(d.getByPin("m1","9999"));assertEquals(listOf("a"),d.getAllByMerchant("m1").first().map{it.id});assertEquals(listOf("b"),d.getAllOnce("m2").map{it.id})
        d.insertOrUpdate(a.copy(name="سارة جديدة"));assertEquals("سارة جديدة",d.getById("a")!!.name)
        d.markSynced("a");assertEquals(listOf("b"),d.getPending().map{it.id});assertEquals(listOf("a"),d.getAllIds("m1"))
        d.deleteAllByMerchant("m2");assertNull(d.getById("b"));d.deleteById("a");assertEquals(0,d.countByMerchant("m1"))
    }
    @Test fun sessionLatestLoginAndLastActiveUseDifferentOrdering()=runTest {
        val d=db.sessionDao();assertNull(d.getLatestSession());assertNull(d.observeLatestSession().first())
        d.upsertAtomic(SessionEntity("a","device-a","A",loginDate=1,lastActive=9));d.insertSession(SessionEntity("b","device-b","B",loginDate=2,lastActive=3))
        assertEquals("b",d.getLatestSession()!!.id);assertEquals("a",d.observeLatestSession().first()!!.id)
        d.updateLastActive("b",10);assertEquals(listOf("b","a"),d.getAllSessions().first().map{it.id});assertEquals(10,d.getById("b")!!.lastActive)
        d.deleteSessionById("b");assertNull(d.getById("b"));d.deleteAll();assertTrue(d.getAllSessions().first().isEmpty())
    }
    @Test fun pendingMessagesRetryStateAndCleanupStayMerchantScoped()=runTest {
        val d=db.pendingMessageDao();d.insert(PendingMessageEntity("a","m1","نص","S",createdAt=1));d.insert(PendingMessageEntity("b","m2","آخر","S",createdAt=2))
        d.setFailed("a",true);assertTrue(d.getAll("m1").first().single().isFailed);assertFalse(d.getAll("m2").first().single().isFailed)
        d.setFailed("a",false);assertFalse(d.getAll("m1").first().single().isFailed)
        d.deleteAll("m1");assertTrue(d.getAll("m1").first().isEmpty());assertEquals("b",d.getAll("m2").first().single().tempId)
        d.delete("b");assertTrue(d.getAll("m2").first().isEmpty())
    }
    @Test fun paymentMethodsRoundTripTypesUpdatesAndDeletion()=runTest {
        val d=db.paymentMethodDao();d.insertAll(listOf(PaymentMethodEntity(1,"نقد","CASH"),PaymentMethodEntity(2,"دين","DEBT")))
        assertEquals(2,d.getAllPaymentMethods().first().size);assertEquals("CASH",d.getPaymentMethodById(1)!!.type)
        d.updatePaymentMethod(PaymentMethodEntity(1,"نقد جديد","CASH"));assertEquals("نقد جديد",d.getPaymentMethodById(1)!!.name)
        d.deletePaymentMethod(d.getPaymentMethodById(2)!!);assertNull(d.getPaymentMethodById(2));assertEquals(1,d.getAllOnce().size)
        d.deleteAll();assertTrue(d.getAllOnce().isEmpty())
    }
    @Test fun customerSearchPhoneExclusionAndDeletePreserveVisitor()=runTest {
        val d=db.customerDao();d.insertAll(listOf(CustomerEntity(-1,"زائر"),CustomerEntity(1,"سارة",phone="0501234567"),CustomerEntity(2,"سامر")))
        assertEquals(setOf(1L,2L),d.searchCustomers("سا").first().map{it.id}.toSet())
        assertEquals(1,d.getByPhone("0501234567",2)!!.id);assertNull(d.getByPhone("0501234567",1));assertNull(d.getByPhone("",1))
        assertEquals(0,d.getTransactionCount(1));d.deleteCustomer(d.getCustomerById(2)!!);assertNull(d.getCustomerById(2))
        d.deleteAll();assertEquals(listOf(-1L),d.getAllCustomers().first().map{it.id})
    }
}
