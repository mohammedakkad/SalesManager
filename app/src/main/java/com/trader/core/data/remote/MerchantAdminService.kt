package com.trader.core.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MerchantAdminService {
    private val db = FirebaseFirestore.getInstance()
    private val merchantsRef = db.collection("merchants")

    fun getAllMerchants(): Flow<List<Merchant>> = callbackFlow {
        val listener = merchantsRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Merchant(
                        id          = doc.id,
                        name        = data["name"] as? String ?: "",
                        phone       = data["phone"] as? String ?: "",
                        activationCode = data["activationCode"] as? String ?: "",
                        status      = MerchantStatus.valueOf(data["status"] as? String ?: "ACTIVE"),
                        isPermanent = data["isPermanent"] as? Boolean ?: true,
                        expiryDate  = doc.getTimestamp("expiryDate"),
                        createdAt   = doc.getTimestamp("createdAt"),
                        lastSeen    = doc.getTimestamp("lastSeen")
                    )
                } catch (e: Exception) { null }
            }
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getMerchantById(id: String): Merchant? {
        val doc = merchantsRef.document(id).get().await()
        val data = doc.data ?: return null
        return try {
            Merchant(
                id          = doc.id,
                name        = data["name"] as? String ?: "",
                phone       = data["phone"] as? String ?: "",
                activationCode = data["activationCode"] as? String ?: "",
                status      = MerchantStatus.valueOf(data["status"] as? String ?: "ACTIVE"),
                isPermanent = data["isPermanent"] as? Boolean ?: true,
                expiryDate  = doc.getTimestamp("expiryDate"),
                createdAt   = doc.getTimestamp("createdAt"),
                lastSeen    = doc.getTimestamp("lastSeen")
            )
        } catch (e: Exception) { null }
    }

    suspend fun addMerchant(merchant: Merchant): String {
        val ref = merchantsRef.add(merchant.toMap()).await()
        return ref.id
    }

    suspend fun updateMerchant(merchant: Merchant) {
        merchantsRef.document(merchant.id).set(merchant.toMap()).await()
    }

    suspend fun deleteMerchant(id: String) {
        merchantsRef.document(id).delete().await()
    }

    suspend fun setStatus(id: String, status: MerchantStatus) {
        merchantsRef.document(id).update("status", status.name).await()
    }

    private fun Merchant.toMap() = mapOf(
        "name"           to name,
        "phone"          to phone,
        "activationCode" to activationCode,
        "status"         to status.name,
        "isPermanent"    to isPermanent,
        "expiryDate"     to expiryDate,
        "createdAt"      to (createdAt ?: Timestamp.now()),
        "lastSeen"       to lastSeen
    )
}
