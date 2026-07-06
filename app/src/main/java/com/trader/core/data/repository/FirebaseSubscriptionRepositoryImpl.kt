package com.trader.core.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.model.SubscriptionRequest
import com.trader.core.domain.model.SubscriptionStatus
import com.trader.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseSubscriptionRepositoryImpl(private val db: FirebaseDatabase) : SubscriptionRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getPendingRequests(): Flow<List<SubscriptionRequest>> = callbackFlow {
        val ref = db.getReference("subscription_requests")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = snapshot.children.flatMap { merchant ->
                    merchant.children.mapNotNull { it.getValue(SubscriptionRequest::class.java) }
                }.filter { it.status == SubscriptionStatus.PENDING.name }

                trySend(requests.sortedByDescending { it.requestedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun approveRequest(request: SubscriptionRequest, planDurationDays: Int) {
        val key = findRequestKey(request) ?: return

        val dayInMs = 24L * 60L * 60L * 1000L
        val expiryMs = System.currentTimeMillis() + (planDurationDays.toLong() * dayInMs)
        val expiryTimestamp = Timestamp(Date(expiryMs))

        db.getReference("subscription_requests")
            .child(request.merchantCode)
            .child(key)
            .child("status")
            .setValue(SubscriptionStatus.APPROVED.name)
            .await()

        val firestoreDocId = resolveFirestoreDocId(request.merchantCode)
        firestore.collection("merchants")
            .document(firestoreDocId)
            .update(
                mapOf(
                    "tier" to MerchantTier.PREMIUM.name,
                    "status" to MerchantStatus.ACTIVE.name,
                    "expiryDate" to expiryTimestamp,
                    "planName" to request.planType,
                    "paymentMethod" to request.paymentMethod,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

        db.reference
            .child("activation_codes")
            .child(request.merchantCode)
            .setValue(mapOf("status" to MerchantStatus.ACTIVE.name))
            .await()
    }

    override suspend fun rejectRequest(request: SubscriptionRequest) {
        val key = findRequestKey(request) ?: return
        db.reference
            .child("subscription_requests")
            .child(request.merchantCode)
            .child(key)
            .child("status")
            .setValue(SubscriptionStatus.REJECTED.name)
            .await()
    }

    private suspend fun resolveFirestoreDocId(merchantCode: String): String {
        val querySnapshot = firestore.collection("merchants")
            .whereEqualTo("activationCode", merchantCode)
            .get()
            .await()

        return querySnapshot.documents.firstOrNull()?.id ?: merchantCode
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
