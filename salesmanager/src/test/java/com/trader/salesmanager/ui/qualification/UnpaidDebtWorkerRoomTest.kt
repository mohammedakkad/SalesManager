package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.salesmanager.worker.UnpaidDebtWorker
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class UnpaidDebtWorkerRoomTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    private val manager get()=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        // Only select the worker's database instance. All queries and rows use real SQLite.
        mockkObject(AppDatabase.Companion);every { AppDatabase.build(any()) } returns db
        db.customerDao().upsertCustomer(CustomerEntity(id=1,name="FIRST",createdAt=0))
        db.customerDao().upsertCustomer(CustomerEntity(id=2,name="SECOND",createdAt=0))
        manager.cancelAll()
    }
    @After fun cleanup(){unmockkObject(AppDatabase.Companion);db.close();manager.cancelAll()}
    private fun worker()=TestListenableWorkerBuilder<UnpaidDebtWorker>(context).build()
    private fun transaction(id:Long,customer:Long=1,amount:Double=12.0,paid:Boolean=false,ageHours:Long=9)=
        TransactionEntity(id=id,customerId=customer,amount=amount,isPaid=paid,date=System.currentTimeMillis()-TimeUnit.HOURS.toMillis(ageHours))

    @Test fun paidAndRecentTransactionsDoNotProduceDebtNotifications()=runBlocking {
        db.transactionDao().insertTransaction(transaction(1,paid=true))
        db.transactionDao().insertTransaction(transaction(2,ageHours=7))
        assertEquals(Result.success(),worker().doWork())
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
        assertFalse(db.transactionDao().getTransactionById(2)!!.isPaid)
    }
    @Test fun overdueInvoicesAreGroupedByCustomerWithCorrectTotalAndNavigation()=runBlocking {
        db.transactionDao().insertTransaction(transaction(1,amount=12.0))
        db.transactionDao().insertTransaction(transaction(2,amount=18.0))
        db.transactionDao().insertTransaction(transaction(3,customer=2,amount=9.0))
        assertEquals(Result.success(),worker().doWork())
        val rows=shadowOf(manager).allNotifications
        assertEquals(2,rows.size)
        assertNotNull(manager.getNotificationChannel("debt_reminder_channel"))
        val first=rows.single { it.extras.getString("android.title")!!.contains("FIRST") }
        assertTrue(first.extras.getString("android.text")!!.contains("30"))
        assertTrue(first.extras.getString("android.text")!!.contains("2"))
        val intent=shadowOf(first.contentIntent).savedIntent
        assertEquals("customer_details",intent.getStringExtra("navigate_to"))
        assertEquals(1L,intent.getLongExtra("customer_id",-1))
        val second=rows.single { it.extras.getString("android.title")!!.contains("SECOND") }
        assertTrue(second.extras.getString("android.text")!!.contains("9"))
        assertTrue(second.extras.getString("android.text")!!.contains("فاتورة"))
        assertEquals(listOf(12.0,18.0,9.0),(1L..3L).map { db.transactionDao().getTransactionById(it)!!.amount })
        assertTrue((1L..3L).none { db.transactionDao().getTransactionById(it)!!.isPaid })
    }
    @Test fun repeatExecutionUpdatesExistingNotificationsWithoutChangingDebt()=runBlocking {
        db.transactionDao().insertTransaction(transaction(1))
        assertEquals(Result.success(),worker().doWork())
        assertEquals(Result.success(),worker().doWork())
        assertEquals(1,shadowOf(manager).allNotifications.size)
        assertEquals(12.0,db.transactionDao().getTransactionById(1)!!.amount,0.0)
        assertFalse(db.transactionDao().getTransactionById(1)!!.isPaid)
    }
}
