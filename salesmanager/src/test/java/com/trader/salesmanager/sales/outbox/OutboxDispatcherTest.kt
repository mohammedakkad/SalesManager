package com.trader.salesmanager.sales.outbox

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class OutboxDispatcherTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private val name="outbox-dispatcher.db"
    private lateinit var db:AppDatabase
    private val session=MutableStateFlow("fixture")
    private var now=100L
    private fun open() { db=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().build() }
    @Before fun setup() { context.deleteDatabase(name);open() }
    @After fun close() { db.close();context.deleteDatabase(name) }
    private fun event(id:String="event-one",revision:Long=1,merchant:String="fixture")=SaleOutboxEntity(eventId=id,operationId=id,merchantId=merchant,
        aggregateId=8,aggregateRevision=revision,eventType="DELETE",payloadVersion=1,
        payload=Gson().toJson(SalePayload(changes=emptyList(),local=SaleLocalState(null,emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))),createdAt=1,updatedAt=1)
    private fun dispatcher(timeout:Long=1000,transport:OutboxTransport)=OutboxDispatcher(db,transport,session,{session.value},{now},1000,timeout)
    @Test fun remoteFailureRetainsIdentityAndRetriesAfterBackoff()=runBlocking {
        db.saleOutboxDao().insert(event());var calls=0
        val d=dispatcher { if(calls++==0) error("offline") else RemoteDelivery.Applied("receipt") }
        assertEquals(DispatchResult.RETRY,d.once("fixture"))
        val failed=db.saleOutboxDao().all().single();assertEquals("FAILED",failed.state);assertEquals(1,failed.retryCount)
        assertEquals(DispatchResult.IDLE,d.once("fixture"))
        now=failed.nextAttemptAt
        assertEquals(DispatchResult.ACKNOWLEDGED,d.once("fixture"));assertEquals("event-one",db.saleOutboxDao().all().single().eventId)
    }
    @Test fun timeoutAndRestartKeepDeletionIntent()=runBlocking {
        db.saleOutboxDao().insert(event())
        assertEquals(DispatchResult.RETRY,dispatcher(30,OutboxTransport { awaitCancellation() }).once("fixture"))
        val failed=db.saleOutboxDao().all().single();assertEquals("TIMEOUT_UNCERTAIN",failed.lastError)
        db.close();open();now=failed.nextAttemptAt
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("receipt") }.once("fixture"))
    }
    @Test fun cancellationReleasesClaimWithoutAcknowledging()=runBlocking {
        db.saleOutboxDao().insert(event());val entered=CompletableDeferred<Unit>()
        val job=launch(Dispatchers.Default) { dispatcher { entered.complete(Unit);awaitCancellation() }.once("fixture") }
        withTimeout(10000) { entered.await() };job.cancelAndJoin()
        val row=db.saleOutboxDao().all().single();assertEquals("FAILED",row.state);assertNull(row.leaseOwner);assertNull(row.acknowledgedAt)
    }
    @Test fun logoutDuringDispatchCancelsOldMerchantAndRetainsIntent()=runBlocking {
        db.saleOutboxDao().insert(event());val entered=CompletableDeferred<Unit>()
        val job=async(Dispatchers.Default) { dispatcher { entered.complete(Unit);awaitCancellation() }.once("fixture") }
        withTimeout(10000) { entered.await() };session.value=""
        assertEquals(DispatchResult.PAUSED,withTimeout(10000) { job.await() })
        assertEquals("FAILED",db.saleOutboxDao().all().single().state)
    }
    @Test fun merchantSwitchNeverClaimsAnotherTenant()=runBlocking {
        db.saleOutboxDao().insert(event());db.saleOutboxDao().insert(event("other",1,"other"))
        val delivered=mutableListOf<String>();val d=dispatcher { delivered+=it.merchantId;RemoteDelivery.Applied("receipt") }
        session.value="other"
        assertEquals(DispatchResult.PAUSED,d.once("fixture"));assertEquals(DispatchResult.ACKNOWLEDGED,d.once("other"))
        assertEquals(listOf("other"),delivered);assertEquals("PENDING",db.saleOutboxDao().get("event-one")!!.state)
    }
    @Test fun concurrentDispatchersDoNotClaimOneEventTwice()=runBlocking {
        db.saleOutboxDao().insert(event());val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();var calls=0
        val remote=OutboxTransport { calls++;entered.complete(Unit);release.await();RemoteDelivery.Applied("receipt") }
        val first=async(Dispatchers.Default) { dispatcher(transport=remote).once("fixture") }
        withTimeout(10000) { entered.await() }
        assertEquals(DispatchResult.IDLE,dispatcher(transport=remote).once("fixture"));release.complete(Unit)
        assertEquals(DispatchResult.ACKNOWLEDGED,first.await());assertEquals(1,calls)
    }
    @Test fun expiredOwnershipRejectsOldNetworkCompletion()=runBlocking {
        db.saleOutboxDao().insert(event());val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val first=async(Dispatchers.Default) { dispatcher { entered.complete(Unit);release.await();RemoteDelivery.Applied("old") }.once("fixture") }
        withTimeout(10000) { entered.await() };now+=1001
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("new") }.once("fixture"))
        release.complete(Unit);assertEquals(DispatchResult.STALE,first.await())
        assertEquals("new",db.saleOutboxDao().all().single().remoteReceipt)
    }
    @Test fun newEditDuringFlightDoesNotLoseNewRevisionOrHoldDatabaseLock()=runBlocking {
        db.saleOutboxDao().insert(event());val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val first=async(Dispatchers.Default) { dispatcher { entered.complete(Unit);release.await();RemoteDelivery.Applied("one") }.once("fixture") }
        withTimeout(10000) { entered.await() }
        withTimeout(10000) { withContext(Dispatchers.IO) { db.saleOutboxDao().insert(event("event-two",2)) } }
        release.complete(Unit);assertEquals(DispatchResult.ACKNOWLEDGED,first.await())
        assertEquals("PENDING",db.saleOutboxDao().get("event-two")!!.state)
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("two") }.once("fixture"))
    }
    @Test fun conflictIsRetainedAndBlocksLaterFinancialEvents()=runBlocking {
        db.saleOutboxDao().insert(event());db.saleOutboxDao().insert(event("event-two",2))
        val d=dispatcher { RemoteDelivery.Conflict("PREIMAGE_MISMATCH") }
        assertEquals(DispatchResult.CONFLICT,d.once("fixture"));assertEquals(DispatchResult.IDLE,d.once("fixture"))
        assertEquals("CONFLICTING",db.saleOutboxDao().get("event-one")!!.state);assertEquals("PENDING",db.saleOutboxDao().get("event-two")!!.state)
    }
    @Test fun failedLocalAcknowledgmentKeepsUncertainRemoteCompletionRetryable()=runBlocking {
        db.saleOutboxDao().insert(event());val effects=mutableSetOf<String>()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER injected BEFORE UPDATE OF state ON sale_outbox WHEN NEW.state='ACKNOWLEDGED' BEGIN SELECT RAISE(ABORT,'ack failure'); END")
        val d=dispatcher { effects+=it.eventId;RemoteDelivery.Applied("receipt") }
        assertEquals(DispatchResult.RETRY,d.once("fixture"));val failed=db.saleOutboxDao().all().single()
        assertEquals("FAILED",failed.state);assertNull(failed.acknowledgedAt)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER injected");now=failed.nextAttemptAt
        assertEquals(DispatchResult.ACKNOWLEDGED,d.once("fixture"));assertEquals(1,effects.size)
    }
}
