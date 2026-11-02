package com.trader.salesmanager.sales.outbox

import com.google.gson.Gson
import com.trader.core.data.local.entity.SaleOutboxEntity
import java.security.MessageDigest

/** Evaluated inside EACH backend's server-retried transaction. No blind monetary/quantity overwrite. */
object ConditionalProtocol {
    enum class Decision { APPLY, DUPLICATE, CONFLICT }
    fun digest(event:SaleOutboxEntity):String=MessageDigest.getInstance("SHA-256").digest(event.payload.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    fun receipt(event:SaleOutboxEntity)=mapOf("eventId" to event.eventId,"revision" to event.aggregateRevision,"digest" to digest(event))
    fun decide(event:SaleOutboxEntity,receipt:Map<String,Any?>?,revision:Long,changes:List<RemoteChange>,current:Map<String,Map<String,Any?>?>):Decision {
        if(receipt!=null) return if(matches(receipt,receipt(event))) Decision.DUPLICATE else Decision.CONFLICT
        if(revision!=event.aggregateRevision-1) return Decision.CONFLICT
        if(changes.any { !matches(current[it.path],it.before) }) return Decision.CONFLICT
        return Decision.APPLY
    }
    fun matches(actual:Any?,expected:Any?):Boolean = when {
        expected==null -> actual==null
        expected is Number && actual is Number -> runCatching { java.math.BigDecimal(actual.toString()).compareTo(java.math.BigDecimal(expected.toString()))==0 }.getOrDefault(false)
        expected is Map<*,*> && actual is Map<*,*> -> expected.all { (key,value)-> matches(actual[key],value) }
        else -> actual==expected
    }
    fun payload(event:SaleOutboxEntity):SalePayload {
        require(event.payloadVersion==1 && event.merchantId.matches(Regex("[A-Za-z0-9_-]+")) && event.aggregateId>0 && event.aggregateRevision>0)
        val payload=com.google.gson.GsonBuilder().setObjectToNumberStrategy(com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE).create().fromJson(event.payload,SalePayload::class.java)
        require(payload.version==1 && payload.changes.size<=400 && event.payload.toByteArray().size<=524288)
        payload.changes.forEach { change ->
            val parts=change.path.split('/')
            require(parts.all { it.matches(Regex("[A-Za-z0-9_-]+")) })
            require(when(change.backend) {
                "RTDB" -> parts.size==2 && parts[0] in setOf("transactions","invoice_items","cash_boxes","cash_box_movements","return_invoices")
                "FIRESTORE" -> (parts.size==2 && parts[0] in setOf("invoice_items","stock_movements")) || (parts.size==4 && parts[0]=="products" && parts[2]=="units")
                else -> false
            })
        }
        require(payload.changes.map { it.backend to it.path }.distinct().size==payload.changes.size)
        return payload
    }
}
