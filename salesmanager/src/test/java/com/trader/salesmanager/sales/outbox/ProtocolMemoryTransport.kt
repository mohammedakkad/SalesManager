package com.trader.salesmanager.sales.outbox

import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A deterministic two-backend server fake using the production conditional decision function. */
internal class ProtocolMemoryTransport(seed:SaleOutboxEntity):OutboxTransport {
    val values=mutableMapOf<Pair<String,String>,Map<String,Any?>>()
    private val receipts=mutableMapOf<Pair<String,String>,Map<String,Any?>>()
    private val heads=mutableMapOf<Pair<String,Long>,Long>()
    private val mutex=Mutex()
    var pauseAfterCommit: (suspend () -> Unit)?=null
    var writes=0
    init { ConditionalProtocol.payload(seed).changes.forEach { c->c.before?.let { values[c.backend to c.path]=it } } }
    fun advanceAggregate(id:Long,revision:Long) { for(backend in listOf("FIRESTORE","RTDB")) heads[backend to id]=revision }
    override suspend fun deliver(event:SaleOutboxEntity):RemoteDelivery {
        val payload=ConditionalProtocol.payload(event)
        val result=mutex.withLock {
            for(backend in listOf("FIRESTORE","RTDB")) {
                val changes=payload.changes.filter { it.backend==backend }
                val decision=ConditionalProtocol.decide(event,receipts[backend to event.eventId],heads[backend to event.aggregateId] ?: 0,
                    changes,changes.associate { it.path to values[backend to it.path] })
                if(decision==ConditionalProtocol.Decision.CONFLICT) return@withLock RemoteDelivery.Conflict("${backend}_PREIMAGE_OR_REVISION")
                if(decision==ConditionalProtocol.Decision.APPLY) {
                    changes.forEach { c->if(c.after==null) values.remove(backend to c.path) else values[backend to c.path]=values[backend to c.path].orEmpty()+c.after;writes++ }
                    heads[backend to event.aggregateId]=event.aggregateRevision
                    receipts[backend to event.eventId]=ConditionalProtocol.receipt(event)
                }
            }
            RemoteDelivery.Applied("${event.eventId}:${event.aggregateRevision}")
        }
        pauseAfterCommit?.invoke()
        return result
    }
}
