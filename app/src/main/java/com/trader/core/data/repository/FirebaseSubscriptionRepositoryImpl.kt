package com.trader.core.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSubscriptionRepositoryImpl(private val db: FirebaseDatabase) : SubscriptionRepository {

    override fun getPendingRequests(): Flow<List<SubscriptionRequest>> = callbackFlow {
        val ref = db.getReference("subscription_requests")
        val listener = ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val requests = snapshot.children.flatMap { merchant ->
                    merchant.children.mapNotNull { it.getValue(SubscriptionRequest::class.java) }
                }.filter { it.status == SubscriptionStatus.PENDING.name }
                trySend(requests.sortedByDescending { it.requestedAt })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun approveRequest(request: SubscriptionRequest, planDurationDays: Int) {
        val key = findRequestKey(request) ?: return
        val expiry = System.currentTimeMillis() + (planDurationDays * 24 * 60 * 60 * 1000L)

        val updates = mapOf(
            "subscription_requests/${request.merchantCode}/$key/status" to SubscriptionStatus.APPROVED.name,
            "merchants/${request.merchantCode}/tier" to MerchantTier.PREMIUM.name,
            "merchants/${request.merchantCode}/subscriptionExpiry" to expiry
        )
        db.reference.updateChildren(updates).await()
    }

    override suspend fun rejectRequest(request: SubscriptionRequest) {
        val key = findRequestKey(request) ?: return
        val updates = mapOf(
            "subscription_requests/${request.merchantCode}/$key/status" to SubscriptionStatus.REJECTED.name,
            "merchants/${request.merchantCode}/tier" to MerchantTier.FREE.name
        )
        db.reference.updateChildren(updates).await()
    }

    private suspend fun findRequestKey(request: SubscriptionRequest): String? {
        val snapshot = db.getReference("subscription_requests/${request.merchantCode}")
            .orderByChild("requestedAt")
            .equalTo(request.requestedAt.toDouble())
            .get()
            .await()
        return snapshot.children.firstOrNull()?.key
    }
}