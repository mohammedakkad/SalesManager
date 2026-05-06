package com.trader.core.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore // ✅ إضافة Firestore
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
        val expiryMs = System.currentTimeMillis() + (planDurationDays * 24 * 60 * 60 * 1000L)

        // 1. تحديث حالة الطلب في RTDB
        db.reference.child("subscription_requests")
            .child(request.merchantCode)
            .child(key)
            .child("status")
            .setValue(SubscriptionStatus.APPROVED.name).await()

        // 2. تحديث ملف التاجر في Firestore
        val expiryTimestamp = com.google.firebase.Timestamp(java.util.Date(expiryMs))
        FirebaseFirestore.getInstance().collection("merchants").document(request.merchantCode)
            .update(
                mapOf(
                    "tier" to MerchantTier.PREMIUM.name,
                    "expiryDate" to expiryTimestamp,
                    "planName" to request.planType,
                    "paymentMethod" to request.paymentMethod
                )
            ).await()
    }

    override suspend fun rejectRequest(request: SubscriptionRequest) {
        val key = findRequestKey(request) ?: return
        db.reference.child("subscription_requests")
            .child(request.merchantCode)
            .child(key)
            .child("status")
            .setValue(SubscriptionStatus.REJECTED.name).await()
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
