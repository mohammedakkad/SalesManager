package com.trader.salesmanager.sales.outbox

import com.google.gson.Gson
import com.trader.core.data.local.entity.SaleOutboxEntity
import org.junit.Test
import org.junit.Assert.*

/** Exercises the public immutable-event validation/receipt contract, without any Firebase connection. */
class ConditionalProtocolBoundaryTest {
    private val local = SaleLocalState(null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    private fun change(backend: String = "RTDB", path: String = "transactions/1",
        before: Map<String, Any?>? = null, after: Map<String, Any?>? = mapOf("amount" to 12L)) = RemoteChange(backend,path,before,after)
    private fun event(changes: List<RemoteChange> = listOf(change())) = SaleOutboxEntity(
        eventId="event",operationId="operation",merchantId="tenant",aggregateId=1,aggregateRevision=1,
        eventType="CREATE",payloadVersion=1,payload=Gson().toJson(SalePayload(changes=changes,local=local)),createdAt=1,updatedAt=1)
    private fun rejected(e: SaleOutboxEntity) {
        try { ConditionalProtocol.payload(e); fail("Unsafe event accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun validTombstoneRetainsBeforeImageAndNullAfterImage() {
        val e=event(listOf(change(before=mapOf("amount" to 12L),after=null)))
        val decoded=ConditionalProtocol.payload(e).changes.single()
        assertNull(decoded.after); assertEquals(12L,decoded.before!!["amount"])
    }
    @Test fun payloadVersionCannotBeUpgradedWithoutProtocolSupport() { rejected(event().copy(payloadVersion=2)) }
    @Test fun eventIdentityCannotEscapeReceiptNamespace() { rejected(event().copy(eventId="event/other")) }
    @Test fun merchantIdentityCannotEscapeTenantNamespace() { rejected(event().copy(merchantId="tenant/other")) }
    @Test fun zeroAggregateCannotTargetAnUnallocatedSale() { rejected(event().copy(aggregateId=0)) }
    @Test fun revisionZeroCannotMasqueradeAsFirstMutation() { rejected(event().copy(aggregateRevision=0)) }
    @Test fun foreignBeforeImageIsRejectedEvenForDeletion() {
        rejected(event(listOf(change(before=mapOf("merchantId" to "other"),after=null))))
    }
    @Test fun foreignAfterImageCannotCreateAnotherTenantProjection() {
        rejected(event(listOf(change(after=mapOf("merchantId" to "other")))))
    }
    @Test fun repeatedTargetCannotApplyAmbiguousEffects() { rejected(event(listOf(change(),change(after=mapOf("amount" to 24L))))) }
    @Test fun sameLegacyPathAcrossIndependentBackendsRemainsValid() {
        val e=event(listOf(change(path="invoice_items/1"),change("FIRESTORE","invoice_items/1")))
        assertEquals(2,ConditionalProtocol.payload(e).changes.size)
    }
    @Test fun unsupportedBackendCannotFallThroughToSuccessfulDelivery() { rejected(event(listOf(change("UNKNOWN")))) }
    @Test fun rtdbCannotWriteProtocolReceiptsAsBusinessData() { rejected(event(listOf(change(path="_sale_events/forged")))) }
    @Test fun firestoreCannotWriteAnEntireProductInsteadOfAUnit() { rejected(event(listOf(change("FIRESTORE","products/1")))) }
    @Test fun unitPathMustUseTheUnitsCollection() { rejected(event(listOf(change("FIRESTORE","products/1/other/2")))) }
    @Test fun trailingSeparatorCannotCreateAnEmptyRemoteKey() { rejected(event(listOf(change(path="transactions/1/")))) }
    @Test fun firestoreLimitAccepts450ButRejects451Effects() {
        val changes=(1..450).map { change("FIRESTORE","invoice_items/$it") }
        assertEquals(450,ConditionalProtocol.payload(event(changes)).changes.size)
        rejected(event(changes+change("FIRESTORE","invoice_items/451")))
    }
    @Test fun totalEffectLimitIncludesRealtimeEffects() {
        val changes=(1..2000).map { change(path="transactions/$it") }
        assertEquals(2000,ConditionalProtocol.payload(event(changes)).changes.size)
        rejected(event(changes+change(path="transactions/2001")))
    }
    @Test fun receiptRequiresSameDigestAndCurrentHead() {
        val e=event(); val receipt=ConditionalProtocol.receipt(e)
        assertEquals(ConditionalProtocol.Decision.DUPLICATE,ConditionalProtocol.decide(e,receipt,1,emptyList(),emptyMap()))
        assertEquals(ConditionalProtocol.Decision.CONFLICT,ConditionalProtocol.decide(e,receipt,2,emptyList(),emptyMap()))
        val edited=event(listOf(change(after=mapOf("amount" to 24L))))
        assertEquals(ConditionalProtocol.Decision.CONFLICT,ConditionalProtocol.decide(edited,receipt,1,emptyList(),emptyMap()))
    }
    @Test fun numericBeforeImagePreservesLargeIntegerPrecisionAndAllowsSdkNumberKinds() {
        val e=event();val c=change(before=mapOf("amount" to 9007199254740993L))
        assertEquals(ConditionalProtocol.Decision.CONFLICT,ConditionalProtocol.decide(e,null,0,listOf(c),mapOf(c.path to mapOf("amount" to 9007199254740992L))))
        val compatible=change(before=mapOf("amount" to 12L))
        assertEquals(ConditionalProtocol.Decision.APPLY,ConditionalProtocol.decide(e,null,0,listOf(compatible),mapOf(c.path to mapOf("amount" to 12.0,"unrelated" to true))))
    }
    @Test fun nonfiniteRemoteBeforeImageCannotMatchAFiniteExpectedBalance() {
        assertFalse(ConditionalProtocol.matches(Double.NaN,12L))
        assertFalse(ConditionalProtocol.matches(Double.POSITIVE_INFINITY,12L))
        assertFalse(ConditionalProtocol.matches("12",12L))
    }
}
