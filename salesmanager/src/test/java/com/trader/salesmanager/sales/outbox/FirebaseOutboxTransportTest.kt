package com.trader.salesmanager.sales.outbox

import android.app.Application
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.trader.core.data.local.entity.SaleOutboxEntity
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** Executes the production Firebase adapter's transaction callbacks against deterministic SDK fakes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class FirebaseOutboxTransportTest {
    private val fs=mockk<FirebaseFirestore>()
    private val rt=mockk<FirebaseDatabase>()
    private val ref=mockk<DatabaseReference>()
    private val docs=mutableMapOf<String,Map<String,Any?>>()
    private val root=mutableMapOf<String,Any?>()
    private var wrapConflicts=false
    private var unitWrites=0
    private var cashWrites=0
    private val prefix="merchants/fixture/"
    private val payload=SalePayload(changes=listOf(
        RemoteChange("FIRESTORE","products/product/units/unit",mapOf("quantityInStock" to 4.0),mapOf("quantityInStock" to 2.0)),
        RemoteChange("RTDB","cash_boxes/1",mapOf("currentBalance" to 100.0),mapOf("currentBalance" to 150.0))),
        local=SaleLocalState(null,emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))
    private fun event(id:String="event",revision:Long=1)=SaleOutboxEntity(eventId=id,operationId=id,merchantId="fixture",aggregateId=8,
        aggregateRevision=revision,eventType="CREATE",payloadVersion=1,payload=Gson().toJson(payload),createdAt=1,updatedAt=1)
    @Suppress("UNCHECKED_CAST") private fun read(path:String):Any? {
        var value:Any?=root
        for(part in path.split('/').filter { it.isNotBlank() }) value=(value as? Map<String,Any?>)?.get(part)
        return value
    }
    @Suppress("UNCHECKED_CAST") private fun write(path:String,value:Any?) {
        val parts=path.split('/');var parent=root
        for(part in parts.dropLast(1)) {
            val child=(parent[part] as? Map<String,Any?>)?.toMutableMap() ?: mutableMapOf()
            parent[part]=child;parent=child
        }
        if(value==null) parent.remove(parts.last()) else parent[parts.last()]=value
        if(path=="cash_boxes/1" || path.startsWith("cash_boxes/1/")) cashWrites++
    }
    private fun node(path:String=""):MutableData=mockk(relaxed=true) {
        every { child(any()) } answers { node(listOf(path,firstArg<String>()).filter { it.isNotEmpty() }.joinToString("/")) }
        every { value } answers { read(path) }
        every { value=any() } answers { write(path,firstArg()) }
    }
    @Before fun setup() {
        docs[prefix+"products/product/units/unit"]=mapOf("quantityInStock" to 4.0)
        write("cash_boxes/1",mapOf("currentBalance" to 100.0));cashWrites=0
        every { fs.document(any()) } answers { val p=firstArg<String>();mockk { every { path } returns p } }
        every { fs.runTransaction<Boolean>(any<com.google.firebase.firestore.Transaction.Function<Boolean>>()) } answers {
            val fn=firstArg<com.google.firebase.firestore.Transaction.Function<Boolean>>()
            val writes=mutableListOf<Pair<String,Map<String,Any?>?>>()
            val tx=mockk<com.google.firebase.firestore.Transaction>(relaxed=true)
            every { tx.get(any<DocumentReference>()) } answers {
                val data=docs[firstArg<DocumentReference>().path]
                mockk { every { this@mockk.data } returns data;every { getLong("revision") } returns (data?.get("revision") as? Number)?.toLong() }
            }
            every { tx.set(any<DocumentReference>(),any<Any>()) } answers {
                @Suppress("UNCHECKED_CAST") val data=secondArg<Any>() as Map<String,Any?>
                writes+=firstArg<DocumentReference>().path to data;tx
            }
            every { tx.set(any<DocumentReference>(),any<Any>(),any<SetOptions>()) } answers {
                @Suppress("UNCHECKED_CAST") val data=secondArg<Any>() as Map<String,Any?>
                val path=firstArg<DocumentReference>().path
                writes+=path to (docs[path].orEmpty()+data);tx
            }
            every { tx.delete(any()) } answers { writes+=firstArg<DocumentReference>().path to null;tx }
            try {
                val result=fn.apply(tx)
                writes.forEach { (path,data)-> if(data==null) docs.remove(path) else docs[path]=data
                    if(path==prefix+"products/product/units/unit") unitWrites++ }
                Tasks.forResult(result)
            } catch(error:Exception) { Tasks.forException(if(wrapConflicts) FirebaseFirestoreException("wrapped SDK conflict",FirebaseFirestoreException.Code.ABORTED,error) else error) }
        }
        val parent=mockk<DatabaseReference>()
        every { rt.getReference("merchants") } returns parent
        every { parent.child("fixture") } returns ref
        every { ref.get() } returns Tasks.forResult(mockk(relaxed=true))
        every { ref.runTransaction(any<com.google.firebase.database.Transaction.Handler>(),false) } answers {
            val handler=firstArg<com.google.firebase.database.Transaction.Handler>()
            val result=handler.doTransaction(node())
            handler.onComplete(null,result.isSuccess,null)
        }
    }
    @Test fun bothConditionalBackendPhasesCompleteBeforeFullAcknowledgment()=runBlocking {
        assertTrue(FirebaseOutboxTransport(fs,rt).deliver(event()) is RemoteDelivery.Applied)
        assertEquals(2.0,(docs[prefix+"products/product/units/unit"]!!["quantityInStock"] as Number).toDouble(),0.0)
        assertEquals(150.0,(read("cash_boxes/1/currentBalance") as Number).toDouble(),0.0)
        assertNotNull(read("_sale_events/event"));assertNotNull(docs[prefix+"_sale_events/event"])
    }
    @Test fun repeatedDeliveryUsesBothReceiptsWithoutReapplyingFinancialEffects()=runBlocking {
        val remote=FirebaseOutboxTransport(fs,rt)
        assertTrue(remote.deliver(event()) is RemoteDelivery.Applied);assertTrue(remote.deliver(event()) is RemoteDelivery.Applied)
        assertEquals(1,unitWrites);assertEquals(1,cashWrites)
    }
    @Test fun failureBetweenBackendsRetainsCompletedPhaseForIdempotentRetry()=runBlocking {
        val remote=FirebaseOutboxTransport(fs,rt)
        every { ref.get() } returns Tasks.forException(IOException("network interruption"))
        try { remote.deliver(event());fail("Expected interrupted second phase") } catch(expected:IOException) {}
        assertEquals(1,unitWrites);assertEquals(0,cashWrites);assertNotNull(docs[prefix+"_sale_events/event"])
        every { ref.get() } returns Tasks.forResult(mockk(relaxed=true))
        assertTrue(remote.deliver(event()) is RemoteDelivery.Applied)
        assertEquals(1,unitWrites);assertEquals(1,cashWrites)
    }
    @Test fun conflictingFirestoreValueIsNotOverwritten()=runBlocking {
        docs[prefix+"products/product/units/unit"]=mapOf("quantityInStock" to 1.0)
        assertTrue(FirebaseOutboxTransport(fs,rt).deliver(event()) is RemoteDelivery.Conflict)
        assertEquals(0,unitWrites);assertEquals(0,cashWrites)
        verify(exactly=0) { ref.get() }
    }
    @Test fun secondBackendConflictIsExplicitRatherThanClaimedAtomicSuccess()=runBlocking {
        write("cash_boxes/1",mapOf("currentBalance" to 900.0));cashWrites=0
        val result=FirebaseOutboxTransport(fs,rt).deliver(event())
        assertTrue(result is RemoteDelivery.Conflict)
        assertEquals(1,unitWrites);assertEquals(0,cashWrites)
        assertTrue((result as RemoteDelivery.Conflict).reason.endsWith("AFTER_FIRESTORE"))
    }
    @Test fun staleRevisionAndEventIdentityCollisionCannotOverwriteRemoteData()=runBlocking {
        val remote=FirebaseOutboxTransport(fs,rt);remote.deliver(event())
        assertTrue(remote.deliver(event("another",1)) is RemoteDelivery.Conflict)
        assertTrue(remote.deliver(event("event",2)) is RemoteDelivery.Conflict)
        assertEquals(1,unitWrites);assertEquals(1,cashWrites)
    }
    @Test fun sdkWrappedConflictRemainsExplicitAndDoesNotBecomeInfiniteTransientRetry()=runBlocking {
        wrapConflicts=true
        docs[prefix+"products/product/units/unit"]=mapOf("quantityInStock" to 1.0)
        assertTrue(FirebaseOutboxTransport(fs,rt).deliver(event()) is RemoteDelivery.Conflict)
        assertEquals(0,unitWrites);assertEquals(0,cashWrites)
    }

    @Test fun completedEventFromOlderRestoreCannotAcknowledgeNewerRemoteRevision()=runBlocking {
        val remote=FirebaseOutboxTransport(fs,rt);remote.deliver(event())
        docs[prefix+"_sale_heads/8"]=mapOf("revision" to 2L,"eventId" to "newer")
        assertTrue(remote.deliver(event()) is RemoteDelivery.Conflict)
        assertEquals(1,unitWrites);assertEquals(1,cashWrites)
    }

    @Test fun foreignMerchantPayloadIsRejectedBeforeAnySdkWrite()=runBlocking {
        val foreign=payload.copy(changes=payload.changes+RemoteChange("FIRESTORE","invoice_items/foreign",null,mapOf("merchantId" to "other")))
        val result=FirebaseOutboxTransport(fs,rt).deliver(event().copy(payload=Gson().toJson(foreign)))
        assertTrue(result is RemoteDelivery.Conflict)
        verify(exactly=0) { fs.runTransaction<Boolean>(any<com.google.firebase.firestore.Transaction.Function<Boolean>>()) }
        assertEquals(0,unitWrites);assertEquals(0,cashWrites)
    }
    @Test fun numericCompareDoesNotRoundDistinctLongIdentifiersIntoOneValue() {
        assertFalse(ConditionalProtocol.matches(9007199254740992L,9007199254740993L))
        assertTrue(ConditionalProtocol.matches(1L,1.0))
    }

    @Test fun conditionalUpsertPreservesUnrelatedRemoteFields()=runBlocking {
        write("cash_boxes/1",mapOf("currentBalance" to 100.0,"serverNote" to "preserve"));cashWrites=0
        assertTrue(FirebaseOutboxTransport(fs,rt).deliver(event()) is RemoteDelivery.Applied)
        assertEquals("preserve",read("cash_boxes/1/serverNote"))
    }

}
