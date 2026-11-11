package com.trader.salesmanager.sales.outbox

import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Two independently atomic conditional phases. A receipt in ONE backend is not full delivery. */
class FirebaseOutboxTransport(
    private val firestore:FirebaseFirestore=FirebaseFirestore.getInstance(),
    private val realtime:FirebaseDatabase=FirebaseDatabase.getInstance()
):OutboxTransport {
    override suspend fun deliver(event:SaleOutboxEntity):RemoteDelivery {
        val payload=try { ConditionalProtocol.payload(event) } catch(invalid:Exception) {
            return RemoteDelivery.Conflict("INVALID_OR_UNSUPPORTED_PAYLOAD")
        }
        try {
            firestorePhase(event,payload.changes.filter { it.backend=="FIRESTORE" })
        } catch(error:Exception) {
            val conflict=conflict(error) ?: throw error
            return RemoteDelivery.Conflict("FIRESTORE_${conflict.code}")
        }
        try {
            realtimePhase(event,payload.changes.filter { it.backend=="RTDB" })
        } catch(error:Exception) {
            val conflict=conflict(error) ?: throw error
            return RemoteDelivery.Conflict("RTDB_${conflict.code}_AFTER_FIRESTORE")
        }
        return RemoteDelivery.Applied("${event.eventId}:${event.aggregateRevision}:${ConditionalProtocol.digest(event)}")
    }
    private suspend fun firestorePhase(event:SaleOutboxEntity,changes:List<RemoteChange>) {
        val prefix="merchants/${event.merchantId}"
        val receipt=firestore.document("$prefix/_sale_events/${event.eventId}")
        val head=firestore.document("$prefix/_sale_heads/${event.aggregateId}")
        val refs=changes.associate { it.path to firestore.document("$prefix/${it.path}") }
        firestore.runTransaction { tx ->
            val existing=tx.get(receipt).data
            val revision=tx.get(head).getLong("revision") ?: 0
            if(existing!=null) {
                if(ConditionalProtocol.decide(event,existing,revision,emptyList(),emptyMap())!=ConditionalProtocol.Decision.DUPLICATE)
                    throw ProtocolConflict("EVENT_COLLISION_OR_REMOTE_AHEAD")
                return@runTransaction true
            }
            val current=refs.mapValues { (_,ref)->tx.get(ref).data }
            when(ConditionalProtocol.decide(event,null,revision,changes,current)) {
                ConditionalProtocol.Decision.CONFLICT -> throw ProtocolConflict("REVISION_OR_PREIMAGE_MISMATCH")
                ConditionalProtocol.Decision.APPLY -> {
                    changes.forEach { change ->
                        val after=change.after
                        if(after==null) tx.delete(refs.getValue(change.path))
                        else tx.set(refs.getValue(change.path),after,SetOptions.merge())
                    }
                    tx.set(head,mapOf("revision" to event.aggregateRevision,"eventId" to event.eventId))
                    tx.set(receipt,ConditionalProtocol.receipt(event))
                }
                ConditionalProtocol.Decision.DUPLICATE -> Unit
            }
            true
        }.await()
    }
    private suspend fun realtimePhase(event:SaleOutboxEntity,changes:List<RemoteChange>) {
        val ref=realtime.getReference("merchants").child(event.merchantId)
        // Populate the SDK cache before its first transaction callback; a cache miss must not be mistaken for a server conflict.
        ref.get().await()
        suspendCancellableCoroutine<Unit> { continuation ->
            ref.runTransaction(object:Transaction.Handler {
                override fun doTransaction(data:MutableData):Transaction.Result {
                    @Suppress("UNCHECKED_CAST")
                    val receipt=data.child("_sale_events/${event.eventId}").value as? Map<String,Any?>
                    val revision=(data.child("_sale_heads/${event.aggregateId}/revision").value as? Number)?.toLong() ?: 0
                    val current=changes.associate { change ->
                        @Suppress("UNCHECKED_CAST")
                        val value=data.child(change.path).value as? Map<String,Any?>
                        change.path to value
                    }
                    val result=ConditionalProtocol.decide(event,receipt,revision,changes,current)
                    if(result==ConditionalProtocol.Decision.CONFLICT) return Transaction.abort()
                    if(result==ConditionalProtocol.Decision.APPLY) {
                        changes.forEach { change ->
                            val after=change.after
                            if(after==null) data.child(change.path).value=null
                            else after.forEach { (key,value) -> data.child("${change.path}/$key").value=value }
                        }
                        data.child("_sale_heads/${event.aggregateId}").value=mapOf("revision" to event.aggregateRevision,"eventId" to event.eventId)
                        data.child("_sale_events/${event.eventId}").value=ConditionalProtocol.receipt(event)
                    }
                    return Transaction.success(data)
                }
                override fun onComplete(error:DatabaseError?,committed:Boolean,snapshot:DataSnapshot?) {
                    if(!continuation.isActive) return
                    when {
                        error!=null -> continuation.resumeWithException(error.toException())
                        !committed -> continuation.resumeWithException(ProtocolConflict("REVISION_OR_PREIMAGE_MISMATCH"))
                        else -> continuation.resume(Unit)
                    }
                }
            },false)
        }
    }
    private fun conflict(error:Throwable):ProtocolConflict? = generateSequence(error) { it.cause }.take(16).filterIsInstance<ProtocolConflict>().firstOrNull()
    private class ProtocolConflict(val code:String):RuntimeException(code)
}
