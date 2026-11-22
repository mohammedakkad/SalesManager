package com.trader.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class SaleOutboxStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name="outbox-storage.db"
    private lateinit var db: AppDatabase
    private fun open() { db=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().build() }
    @Before fun setup() { context.deleteDatabase(name);open() }
    @After fun close() { db.close();context.deleteDatabase(name) }
    private fun event(id:String="one",merchant:String="fixture",revision:Long=1)=SaleOutboxEntity(eventId=id,operationId=id,
        merchantId=merchant,aggregateId=8,aggregateRevision=revision,eventType="CREATE",payloadVersion=1,payload="{}",createdAt=1,updatedAt=1)
    @Test fun eventSurvivesDatabaseRestart()=runBlocking {
        db.saleOutboxDao().insert(event());db.close();open()
        assertEquals("one",db.saleOutboxDao().all().single().eventId)
    }
    @Test fun transactionFailureRollsBackEvent()=runBlocking {
        try { db.withTransaction { db.saleOutboxDao().insert(event());error("injected") } } catch(expected:IllegalStateException) {}
        assertTrue(db.saleOutboxDao().all().isEmpty())
    }
    @Test fun duplicateIdentityAndRevisionAreRejected()=runBlocking {
        db.saleOutboxDao().insert(event())
        for(e in listOf(event(),event("two"))) {
            try { db.saleOutboxDao().insert(e);fail("Duplicate event accepted") }
            catch(expected:android.database.sqlite.SQLiteConstraintException) {}
        }
        assertEquals(1,db.saleOutboxDao().all().size)
    }
    @Test fun merchantsAreClaimedSeparately()=runBlocking {
        db.saleOutboxDao().insert(event());db.saleOutboxDao().insert(event("other","other"))
        assertEquals("other",db.saleOutboxDao().claim("other","owner",10,100)!!.merchantId)
        assertEquals("fixture",db.saleOutboxDao().claim("fixture","owner2",10,100)!!.merchantId)
    }
    @Test fun concurrentClaimsHaveOneOwnerAndExpiredOwnerCannotAcknowledge()=runBlocking {
        db.saleOutboxDao().insert(event())
        val claims=(1..2).map { n->async(Dispatchers.Default) { db.saleOutboxDao().claim("fixture","owner$n",10,100) } }.awaitAll().filterNotNull()
        assertEquals(1,claims.size)
        assertNull(db.saleOutboxDao().claim("fixture","later",50,100))
        val reclaimed=db.saleOutboxDao().claim("fixture","new-owner",111,100)!!
        assertEquals(0,db.saleOutboxDao().acknowledge("one",1,claims.single().leaseOwner!!,112,"stale"))
        assertEquals(0,db.saleOutboxDao().acknowledge("one",2,reclaimed.leaseOwner!!,112,"wrong-revision"))
        assertEquals(1,db.saleOutboxDao().acknowledge("one",1,reclaimed.leaseOwner!!,112,"receipt"))
    }
}
