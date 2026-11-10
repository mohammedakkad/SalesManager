package com.trader.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class OutboxRecoveryQueriesTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private val name="outbox-recovery-query.db"
    private lateinit var db:AppDatabase
    private fun open(){db=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().build()}
    @Before fun setup(){context.deleteDatabase(name);open()}
    @After fun cleanup(){db.close();context.deleteDatabase(name)}
    private fun event(id:String,aggregate:Long=1,revision:Long=1,merchant:String="m",state:String="PENDING")=
        SaleOutboxEntity(eventId=id,operationId="operation-$id",merchantId=merchant,aggregateId=aggregate,aggregateRevision=revision,
            eventType="DELETE",payloadVersion=1,payload="{\"snapshot\":\"$id\"}",state=state,createdAt=1,updatedAt=1)
    @Test fun latestReceiptPagesExcludeSupersededRevisionsAndOtherMerchants()=runBlocking{
        val dao=db.saleOutboxDao()
        dao.insert(event("old",revision=1,state="ACKNOWLEDGED"));dao.insert(event("new",revision=2,state="ACKNOWLEDGED"))
        dao.insert(event("second",aggregate=2,state="ACKNOWLEDGED"));dao.insert(event("foreign",merchant="other",state="ACKNOWLEDGED"))
        dao.insert(event("blocked-old",aggregate=3,state="ACKNOWLEDGED"));dao.insert(event("blocked-new",aggregate=3,revision=2))
        val first=dao.latestAcknowledgedPage("m",0,1).single();assertEquals("new",first.eventId)
        val next=dao.latestAcknowledgedPage("m",first.sequence,1).single();assertEquals("second",next.eventId)
        assertTrue(dao.latestAcknowledgedPage("m",next.sequence,1).isEmpty())
        assertEquals(2L,dao.revision("m",1));assertEquals(0L,dao.revision("absent",1))
    }
    @Test fun fullyPopulatedFailureAndAcknowledgmentMetadataSurviveRestart()=runBlocking{
        val dao=db.saleOutboxDao();dao.insert(event("one"));dao.claim("m","worker",10,100)
        assertEquals(1,dao.finishAttempt("one",1,"worker","FAILED",50,11,"retryable-unavailable"))
        db.close();open();val failed=db.saleOutboxDao().get("one")!!
        assertEquals("FAILED",failed.state);assertEquals(1,failed.retryCount);assertEquals(50L,failed.nextAttemptAt)
        assertNull(failed.leaseOwner);assertNull(failed.leaseExpiresAt);assertEquals("retryable-unavailable",failed.lastError)
        assertNull(db.saleOutboxDao().claim("m","second",49,100))
        db.saleOutboxDao().claim("m","second",50,100)
        assertEquals(1,db.saleOutboxDao().acknowledge("one",1,"second",51,"immutable-receipt"))
        db.close();open();val done=db.saleOutboxDao().get("one")!!
        assertEquals("ACKNOWLEDGED",done.state);assertEquals("immutable-receipt",done.remoteReceipt)
        assertEquals(51L,done.acknowledgedAt);assertEquals(2,done.retryCount);assertNull(done.lastError);assertNull(done.leaseOwner)
        assertEquals("{\"snapshot\":\"one\"}",done.payload)
    }
    @Test fun observersExposeCommittedQueueAndDeletionWithoutLosingTheDurableTombstone()=runBlocking{
        val dao=db.saleOutboxDao();assertEquals(0,dao.observeOutstanding().first());assertEquals(0L,dao.observeLatestSequence().first())
        dao.insert(event("one"));dao.insert(event("two",aggregate=2,state="ACKNOWLEDGED"))
        assertEquals(listOf("one"),dao.observeUnacknowledged().first().map{it.eventId});assertEquals(1,dao.observeOutstanding().first())
        assertEquals(dao.latestSequence(),dao.observeLatestSequence().first());assertTrue(dao.hasHistoryFor("m"));assertTrue(dao.manages(1))
        assertFalse(dao.hasOutstandingFor("other"));assertTrue(dao.hasOutstanding())
        db.close();open();assertEquals("DELETE",db.saleOutboxDao().get("one")!!.eventType)
        db.saleOutboxDao().deleteAll();assertEquals(0,db.saleOutboxDao().observeOutstanding().first());assertFalse(db.saleOutboxDao().hasHistoryFor("m"))
    }
    @Test fun malformedProcessingLeaseCannotBeReclaimedWithoutAnExplicitRecoveryPolicy()=runBlocking{
        val dao=db.saleOutboxDao();dao.insert(event("one",state="PROCESSING").copy(leaseOwner="old",leaseExpiresAt=null))
        assertNull(dao.claim("m","new",100,100));assertEquals("old",dao.get("one")!!.leaseOwner)
        assertEquals(0,dao.acknowledge("one",1,"old",100,"receipt"));assertNull(dao.get("one")!!.remoteReceipt)
    }
    @Test fun claimAtExpiryBoundaryRejectsTheOldAcknowledgmentAndIncrementsRetryExactlyOnce()=runBlocking{
        val dao=db.saleOutboxDao();dao.insert(event("one"));dao.claim("m","old",10,100)
        assertEquals(0,dao.acknowledge("one",1,"old",110,"late"))
        val claimed=dao.claim("m","new",110,100)!!;assertEquals(2,claimed.retryCount)
        assertEquals(0,dao.finishAttempt("one",1,"old","FAILED",999,111,"stale-error"))
        assertEquals("new",dao.get("one")!!.leaseOwner);assertNull(dao.get("one")!!.lastError)
    }
    @Test fun rollbackOfClaimPreservesOriginalOwnerAndAllowsRecoveryAfterRestart()=runBlocking{
        val dao=db.saleOutboxDao();dao.insert(event("one"))
        try{db.withTransaction{assertNotNull(dao.claim("m","rolled-back",10,100));error("injected-after-claim")}}catch(expected:IllegalStateException){}
        db.close();open();val pending=db.saleOutboxDao().get("one")!!
        assertEquals("PENDING",pending.state);assertEquals(0,pending.retryCount);assertNull(pending.leaseOwner)
        assertEquals("committed",db.saleOutboxDao().claim("m","committed",11,100)!!.leaseOwner)
    }
}
