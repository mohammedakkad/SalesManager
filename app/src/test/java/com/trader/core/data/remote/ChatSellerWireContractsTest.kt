package com.trader.core.data.remote

import android.app.Application
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.trader.core.domain.model.ChatMessage
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** Seller wire operations only; deterministic SDK tasks, no server or Admin send operation. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ChatSellerWireContractsTest {
    private val db=mockk<FirebaseFirestore>()
    private val chats=mockk<CollectionReference>()
    private val merchant=mockk<DocumentReference>()
    private val messages=mockk<CollectionReference>()
    private val query=mockk<Query>()
    private val refs=mutableMapOf<String,DocumentReference>()
    private val sets=mutableMapOf<String,Map<*,*>>()
    private val updates=mutableMapOf<String,Any>()
    private val batches=mutableListOf<WriteBatch>()
    private var listener:EventListener<QuerySnapshot>?=null
    private var removals=0
    private var failCommitAt=0
    private fun ref(id:String)=refs.getOrPut(id){mockk<DocumentReference>().also{node->
        every{node.id} returns id
        every{node.set(any<Any>())} answers{sets[id]=firstArg<Map<*,*>>();Tasks.forResult(null)}
        every{node.update(any<Map<String,Any>>())} answers{updates[id]=firstArg<Map<*,*>>();Tasks.forResult(null)}
        every{node.update(any<String>(),any(),*anyVararg())} answers{updates[id]=args.toList();Tasks.forResult(null)}
    }}
    @Before fun setup(){
        mockkStatic(FirebaseFirestore::class);every{FirebaseFirestore.getInstance()} returns db
        every{db.collection("chat")} returns chats;every{chats.document("seller-fixture")} returns merchant
        every{merchant.collection("messages")} returns messages;every{messages.document(any())} answers{ref(firstArg())}
        every{messages.orderBy("timestamp",Query.Direction.ASCENDING)} returns query
        every{messages.whereEqualTo("isRead",false)} returns query
        every{query.addSnapshotListener(any<EventListener<QuerySnapshot>>())} answers{
            listener=firstArg();mockk<ListenerRegistration>{every{remove()} answers{removals++;Unit}}
        }
        every{db.batch()} answers{
            val number=batches.size+1
            mockk<WriteBatch>().also{batch->batches+=batch
                every{batch.update(any<DocumentReference>(),any<Map<String,Any>>())} returns batch
                every{batch.commit()} answers{if(failCommitAt==number)Tasks.forException(IOException("fixture write failure")) else Tasks.forResult(null)}
            }
        }
    }
    @After fun cleanup(){unmockkStatic(FirebaseFirestore::class)}
    @Test fun sendingUsesStableMessageIdentityAndClearsRemoteReadAndEditFlags()=runTest{
        ChatService().sendMessage("seller-fixture",ChatMessage("stable-message","مرحبا","seller-fixture","بائع"))
        val row=sets["stable-message"]!!;assertEquals("مرحبا",row["text"]);assertEquals("seller-fixture",row["senderId"])
        assertEquals(false,row["isRead"]);assertNull(row["readAt"]);assertNull(row["editedAt"]);assertNull(row["deletedAt"])
        assertTrue(row["timestamp"] is Timestamp);verify(exactly=0){db.collection("notifications")}
    }
    @Test fun rejectedSendPropagatesTheSdkFailure()=runTest{
        val failure=IOException("fixture denied");val node=ref("one");every{node.set(any<Any>())} returns Tasks.forException(failure)
        try{ChatService().sendMessage("seller-fixture",ChatMessage(id="one",senderId="seller-fixture"));fail("write acknowledged")}catch(actual:IOException){assertSame(failure,actual)}
    }
    @Test fun markingReadWritesAnActualTimestampWithoutReplacingMessageContent()=runTest{
        ChatService().markAsRead("seller-fixture","one")
        val row=updates["one"] as Map<*,*>;assertEquals(setOf("isRead","readAt"),row.keys)
        assertEquals(true,row["isRead"]);assertTrue((row["readAt"] as Long)>0);assertTrue(sets.isEmpty())
    }
    @Test fun invalidReadIdentitiesAndEmptyBulkReadNeverWrite()=runTest{
        val service=ChatService();service.markAsRead("","one");service.markAsRead("seller-fixture","")
        service.markAllAsRead("seller-fixture",emptyList());service.markAllAsRead("",listOf("one"))
        assertTrue(updates.isEmpty());assertTrue(batches.isEmpty());verify(exactly=0){db.collection(any())}
    }
    @Test fun bulkReadSplitsAtFourHundredAndWaitsForEveryBatch()=runTest{
        ChatService().markAllAsRead("seller-fixture",(1..801).map{"m$it"})
        assertEquals(3,batches.size)
        for((index,count) in listOf(400,400,1).withIndex()){
            verify(exactly=count){batches[index].update(any<DocumentReference>(),any<Map<String,Any>>())}
            verify(exactly=1){batches[index].commit()}
        }
    }
    @Test fun partialBulkReadFailureStopsBeforeStartingLaterBatches()=runTest{
        failCommitAt=2
        try{ChatService().markAllAsRead("seller-fixture",(1..801).map{"m$it"});fail("partial write reported success")}catch(expected:IOException){}
        assertEquals(2,batches.size);verify(exactly=1){batches[0].commit()};verify(exactly=1){batches[1].commit()}
        // The first batch may have committed. This test does not assert distributed rollback or exactly-once delivery.
    }
    @Test fun editingWritesOnlyTextAndEditTimestamp()=runTest{
        ChatService().editMessage("seller-fixture","one","تصحيح")
        val row=updates["one"] as Map<*,*>;assertEquals(setOf("text","editedAt"),row.keys);assertEquals("تصحيح",row["text"]);assertTrue(row["editedAt"] is Timestamp)
    }
    @Test fun deletionPreservesTheDocumentUsingATimestampTombstone()=runTest{
        ChatService().deleteMessage("seller-fixture","one")
        val args=updates["one"] as List<*>;assertEquals("deletedAt",args[0]);assertTrue(args[1] is Timestamp);assertTrue(sets.isEmpty())
    }
    @Test fun blankMerchantCannotSendReadEditDeleteOrSubscribe()=runTest{
        val service=ChatService()
        try{service.sendMessage("",ChatMessage());fail("anonymous send")}catch(expected:IllegalArgumentException){}
        try{service.getMessages("");fail("anonymous subscription")}catch(expected:IllegalArgumentException){}
        service.editMessage("","one","text");service.deleteMessage("","one")
        assertEquals(0,service.getUnreadCount("","seller").first());verify(exactly=0){db.collection(any())}
    }
    @Test fun incomingSnapshotRetainsReadEditAndDeletionMetadataThenUnregisters()=runTest{
        val stamp=Timestamp(123,0);val doc=mockk<DocumentSnapshot>()
        every{doc.id} returns "one";every{doc.getString(any())} answers{mapOf("text" to "رسالة","senderId" to "support","senderName" to "الدعم")[firstArg<String>()]}
        every{doc.getTimestamp(any())} returns stamp;every{doc.getBoolean("isRead")} returns true;every{doc.getLong("readAt")} returns 321L
        val snapshot=mockk<QuerySnapshot>();every{snapshot.documents} returns listOf(doc)
        val result=async{ChatService().getMessages("seller-fixture").first()};runCurrent();listener!!.onEvent(snapshot,null);runCurrent()
        assertEquals(ChatMessage("one","رسالة","support","الدعم",stamp,true,321L,stamp,stamp),result.await().single());assertEquals(1,removals)
    }
    @Test fun cancellingAnActiveMessagesSubscriptionAlwaysUnregisters()=runTest{
        val job=ChatService().getMessages("seller-fixture").launchIn(backgroundScope);runCurrent();assertEquals(0,removals)
        job.cancelAndJoin();assertEquals(1,removals)
    }
    @Test fun unreadSnapshotExcludesTheSellersOwnMessages()=runTest{
        fun doc(sender:String)=mockk<DocumentSnapshot>{every{getString("senderId")} returns sender}
        val incoming=doc("support");val own=doc("seller-fixture");val snapshot=mockk<QuerySnapshot>();every{snapshot.documents} returns listOf(incoming,own)
        val result=async{ChatService().getUnreadCount("seller-fixture","seller-fixture").first()};runCurrent();listener!!.onEvent(snapshot,null);runCurrent()
        assertEquals(1,result.await());assertEquals(1,removals)
    }
}
