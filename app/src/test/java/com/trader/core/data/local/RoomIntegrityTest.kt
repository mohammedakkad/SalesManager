package com.trader.core.data.local
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RoomIntegrityTest {
    private lateinit var db:AppDatabase
    @Before fun open() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() { db.close() }
    @Test fun customerUpsertPreservesSalesAndDebt() = runTest {
        val customer=CustomerEntity(id=1,name="fixture",phone="",createdAt=0)
        db.customerDao().upsertCustomer(customer)
        val id=db.transactionDao().insertTransaction(TransactionEntity(customerId=1,amount=75.5,isPaid=false,date=100))
        db.customerDao().upsertCustomer(customer.copy(name="updated"))
        assertEquals(75.5,db.transactionDao().getTransactionById(id)!!.amount,0.0)
        assertEquals(75.5,db.transactionDao().getUnpaidAmountByCustomer(1),0.0)
        assertEquals("updated",db.customerDao().getCustomerById(1)!!.name)
    }
    @Test fun foreignKeyRejectsOrphanSale() = runTest {
        try {
            db.transactionDao().insertTransaction(TransactionEntity(customerId=999,amount=10.0,isPaid=false))
            fail("Orphan accepted")
        } catch(expected:android.database.sqlite.SQLiteConstraintException) { }
        assertTrue(db.transactionDao().getAllOnce().isEmpty())
    }
}
