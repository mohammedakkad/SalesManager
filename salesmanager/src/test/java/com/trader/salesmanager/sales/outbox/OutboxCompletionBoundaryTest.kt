package com.trader.salesmanager.sales.outbox

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.ReturnInvoiceEntity
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real SQLite receipts/leases; transport and session races are controlled through public seams. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class OutboxCompletionBoundaryTest {
    private lateinit var db: AppDatabase
    private val session = MutableStateFlow("tenant")
    private var now = 100L
    @Before fun open() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun close() { db.close() }
    private fun event(retries: Int=0, returns: List<ReturnInvoiceEntity> = emptyList()) = SaleOutboxEntity(
        eventId="event", operationId="operation", merchantId="tenant", aggregateId=8,
        aggregateRevision=1, eventType="RETURN", payloadVersion=1,
        payload=Gson().toJson(SalePayload(changes=emptyList(),local=SaleLocalState(null,emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),returns))),
        retryCount=retries,createdAt=1,updatedAt=1)
    private fun dispatcher(current: suspend () -> String = { session.value }, send: OutboxTransport) =
        OutboxDispatcher(db,send,session,current,{now},leaseMillis=1000)
    private fun returned(status: String="PENDING") = ReturnInvoiceEntity("return",8,"tenant","PARTIAL",12.0,"refund",1,status)

    @Test fun blankMerchantDoesNotClaimOrTransmit() = runBlocking {
        db.saleOutboxDao().insert(event()); var calls=0
        assertEquals(DispatchResult.PAUSED,dispatcher { calls++; RemoteDelivery.Applied("receipt") }.once(" "))
        assertEquals(0,calls); assertEquals("PENDING",db.saleOutboxDao().get("event")!!.state)
    }
    @Test fun sessionChangedAfterClaimReleasesOwnershipWithoutSending() = runBlocking {
        db.saleOutboxDao().insert(event()); var reads=0; var sends=0
        val d=dispatcher(current={if(++reads==1) "tenant" else "other"}) { sends++; RemoteDelivery.Applied("receipt") }
        assertEquals(DispatchResult.PAUSED,d.once("tenant"))
        val row=db.saleOutboxDao().get("event")!!
        assertEquals(0,sends); assertEquals("FAILED",row.state); assertNull(row.leaseOwner); assertNull(row.acknowledgedAt)
    }
    @Test fun sessionChangedWhileEmptyClaimWasReadPausesQueueRecovery() = runBlocking {
        var reads=0
        val d=dispatcher(current={if(++reads==1) "tenant" else "other"}) { error("No send expected") }
        assertEquals(DispatchResult.PAUSED,d.once("tenant")); assertTrue(db.saleOutboxDao().all().isEmpty())
    }
    @Test fun sessionChangedAfterRemoteResultCannotCommitAcknowledgment() = runBlocking {
        db.saleOutboxDao().insert(event()); var reads=0
        val d=dispatcher(current={if(++reads<=2) "tenant" else "other"}) { RemoteDelivery.Applied("uncertain") }
        assertEquals(DispatchResult.PAUSED,d.once("tenant"))
        val row=db.saleOutboxDao().get("event")!!
        assertEquals("FAILED",row.state); assertNull(row.acknowledgedAt); assertNull(row.remoteReceipt)
    }
    @Test fun oldConflictCannotReplaceAReclaimedLease() = runBlocking {
        db.saleOutboxDao().insert(event())
        val d=dispatcher {
            now+=1000
            assertNotNull(db.saleOutboxDao().claim("tenant","new-owner",now,1000))
            RemoteDelivery.Conflict("old conflict")
        }
        assertEquals(DispatchResult.STALE,d.once("tenant"))
        val row=db.saleOutboxDao().get("event")!!
        assertEquals("PROCESSING",row.state); assertEquals("new-owner",row.leaseOwner); assertNull(row.lastError)
    }
    @Test fun oldNetworkFailureCannotReleaseAnotherWorkersLease() = runBlocking {
        db.saleOutboxDao().insert(event())
        val d=dispatcher {
            now+=1000; assertNotNull(db.saleOutboxDao().claim("tenant","new-owner",now,1000))
            throw java.io.IOException("old upload failed")
        }
        assertEquals(DispatchResult.RETRY,d.once("tenant"))
        val row=db.saleOutboxDao().get("event")!!
        assertEquals("PROCESSING",row.state); assertEquals("new-owner",row.leaseOwner); assertEquals(0L,row.nextAttemptAt)
    }
    @Test fun durableReceiptIsBoundedWithoutChangingRemoteCompletion() = runBlocking {
        db.saleOutboxDao().insert(event()); val receipt="r".repeat(201)
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied(receipt) }.once("tenant"))
        assertEquals(receipt.take(200),db.saleOutboxDao().get("event")!!.remoteReceipt)
    }
    @Test fun durableConflictReasonIsBoundedAndNeverAcknowledged() = runBlocking {
        db.saleOutboxDao().insert(event()); val reason="c".repeat(161)
        assertEquals(DispatchResult.CONFLICT,dispatcher { RemoteDelivery.Conflict(reason) }.once("tenant"))
        val row=db.saleOutboxDao().get("event")!!
        assertEquals(reason.take(160),row.lastError); assertNull(row.acknowledgedAt)
    }
    @Test fun repeatedFailureCapsBackoffAtSixHoursAndRetainsEventIdentity() = runBlocking {
        db.saleOutboxDao().insert(event(retries=16)); var calls=0
        val d=dispatcher { calls++; throw java.io.IOException("offline") }
        assertEquals(DispatchResult.RETRY,d.once("tenant"))
        val first=db.saleOutboxDao().get("event")!!
        assertEquals(17,first.retryCount); assertEquals(now+21600000L,first.nextAttemptAt)
        now=first.nextAttemptAt-1; assertEquals(DispatchResult.IDLE,d.once("tenant")); assertEquals(1,calls)
        now++; assertEquals(DispatchResult.RETRY,d.once("tenant"))
        val second=db.saleOutboxDao().get("event")!!
        assertEquals(18,second.retryCount); assertEquals(now+21600000L,second.nextAttemptAt); assertEquals(first.payload,second.payload)
    }
    @Test fun matchingReturnIsAcknowledgedWithoutChangingRefund() = runBlocking {
        val original=returned(); db.returnDao().insertReturnInvoice(original); db.saleOutboxDao().insert(event(returns=listOf(original)))
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("receipt") }.once("tenant"))
        assertEquals(original.copy(syncStatus="SYNCED"),db.returnDao().getById(original.id))
    }
    @Test fun changedReturnRemainsPendingDespiteOlderReceipt() = runBlocking {
        val original=returned(); val changed=original.copy(totalRefund=18.0)
        db.returnDao().insertReturnInvoice(changed); db.saleOutboxDao().insert(event(returns=listOf(original)))
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("receipt") }.once("tenant"))
        assertEquals(changed,db.returnDao().getById(original.id))
    }
    @Test fun deletedReturnIsNotResurrectedByQueueDrain() = runBlocking {
        val original=returned(); db.saleOutboxDao().insert(event(returns=listOf(original)))
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("receipt") }.once("tenant"))
        assertNull(db.returnDao().getById(original.id))
    }
    @Test fun alreadySyncedReturnDoesNotReceiveARedundantWrite() = runBlocking {
        val original=returned("SYNCED"); db.returnDao().insertReturnInvoice(original)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER no_redundant_return_update BEFORE UPDATE ON return_invoices BEGIN SELECT RAISE(ABORT,'redundant acknowledgment'); END")
        db.saleOutboxDao().insert(event(returns=listOf(original)))
        assertEquals(DispatchResult.ACKNOWLEDGED,dispatcher { RemoteDelivery.Applied("receipt") }.once("tenant"))
        assertEquals(original,db.returnDao().getById(original.id))
    }
    @Test fun returnStatusFailureRollsBackReceiptAndAllowsIdempotentRetry() = runBlocking {
        val original=returned(); db.returnDao().insertReturnInvoice(original); db.saleOutboxDao().insert(event(returns=listOf(original)))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_return_update BEFORE UPDATE ON return_invoices BEGIN SELECT RAISE(ABORT,'injected return failure'); END")
        val d=dispatcher { RemoteDelivery.Applied("receipt") }
        assertEquals(DispatchResult.RETRY,d.once("tenant"))
        val failed=db.saleOutboxDao().get("event")!!
        assertEquals("FAILED",failed.state); assertNull(failed.acknowledgedAt); assertEquals(original,db.returnDao().getById(original.id))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_return_update"); now=failed.nextAttemptAt
        assertEquals(DispatchResult.ACKNOWLEDGED,d.once("tenant")); assertEquals("SYNCED",db.returnDao().getById(original.id)!!.syncStatus)
    }
}
