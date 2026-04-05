package com.trader.core.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
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
        val listener = merchantsRef.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject<MerchantFirestore>()?.toDomain(doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getMerchantById(id: String): Merchant? {
        val doc = merchantsRef.document(id).get().await()
        return doc.toObject<MerchantFirestore>()?.toDomain(doc.id)
    }

    suspend fun addMerchant(merchant: Merchant): String {
        val data = merchant.toFirestore()
        val ref = merchantsRef.add(data).await()
        return ref.id
    }

    suspend fun updateMerchant(merchant: Merchant) {
        merchantsRef.document(merchant.id).set(merchant.toFirestore()).await()
    }

    suspend fun deleteMerchant(id: String) {
        merchantsRef.document(id).delete().await()
    }

    suspend fun setStatus(id: String, status: MerchantStatus) {
        merchantsRef.document(id).update("status", status.name).await()
    }

    private fun Merchant.toFirestore() = mapOf(
        "name" to name, "phone" to phone, "activationCode" to activationCode,
        "status" to status.name, "isPermanent" to isPermanent,
        "expiryDate" to expiryDate, "createdAt" to (createdAt ?: Timestamp.now()),
        "lastSeen" to lastSeen
    )
}

data class MerchantFirestore(
    val name: String = "", val phone: String = "", val activationCode: String = "",
    val status: String = "ACTIVE", val isPermanent: Boolean = true,
    val expiryDate: Timestamp? = null, val createdAt: Timestamp? = null, val lastSeen: Timestamp? = null
) {
    fun toDomain(id: String) = Merchant(
        id = id, name = name, phone = phone, activationCode = activationCode,
        status = MerchantStatus.valueOf(status), isPermanent = isPermanent,
        expiryDate = expiryDate, createdAt = createdAt, lastSeen = lastSeen
    )
}
