package com.trader.core.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class MerchantAdminService {
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance().reference
    private val merchantsRef = db.collection("merchants")

    fun getAllMerchants(): Flow<List<Merchant>> = callbackFlow {
        val listener = merchantsRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Merchant(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        phone = data["phone"] as? String ?: "",
                        activationCode = data["activationCode"] as? String ?: "",
                        status = MerchantStatus.valueOf(data["status"] as? String ?: "ACTIVE"),
                        isPermanent = data["isPermanent"] as? Boolean != false,
                        expiryDate = safeTimestamp(doc, "expiryDate"),
                        createdAt = safeTimestamp(doc, "createdAt"),
                        lastSeen = safeTimestamp(doc, "lastSeen"),
                        tier = runCatching {
                            MerchantTier.valueOf(data["tier"] as? String ?: "FREE")
                        }.getOrDefault(MerchantTier.FREE),
                        deviceId = data["deviceId"] as? String,
                        isSelfRegistered = data["isSelfRegistered"] as? Boolean == true,
                        planName = data["planName"] as? String,
                        paymentMethod = data["paymentMethod"] as? String
                    )
                } catch (e: Exception) {
                    null
                }
            }
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun unlinkDevice(id: String) {
        // Setting deviceId to null allows the merchant to bind to a new device on next login
        merchantsRef.document(id).update("deviceId", null).await()
    }

    suspend fun getMerchantById(id: String): Merchant? {
        val doc = merchantsRef.document(id).get().await()
        val data = doc.data ?: return null
        return try {
            Merchant(
                id = doc.id,
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                activationCode = data["activationCode"] as? String ?: "",
                status = MerchantStatus.valueOf(data["status"] as? String ?: "ACTIVE"),
                isPermanent = data["isPermanent"] as? Boolean ?: true,
                expiryDate = safeTimestamp(doc, "expiryDate"),
                createdAt = safeTimestamp(doc, "createdAt"),
                lastSeen = safeTimestamp(doc, "lastSeen"),
                tier = runCatching {
                    MerchantTier.valueOf(data["tier"] as? String ?: "FREE")
                }.getOrDefault(MerchantTier.FREE),
                isSelfRegistered = data["isSelfRegistered"] as? Boolean ?: false,
                planName = data["planName"] as? String,
                paymentMethod = data["paymentMethod"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMerchant(merchant: Merchant): String {
        val ref = merchantsRef.add(merchant.toMap()).await()
        return ref.id
    }

    suspend fun updateMerchant(merchant: Merchant) {
        merchantsRef.document(merchant.id).set(merchant.toMap()).await()
    }

    suspend fun deleteMerchant(id: String) {
        val code = getMerchantById(id)?.activationCode
        merchantsRef.document(id).delete().await()
        if (!code.isNullOrEmpty()) {
            rtdb.child("activation_codes").child(code).removeValue().await()
        }
    }

    suspend fun setStatus(id: String, status: MerchantStatus) {
        val code = getMerchantById(id)?.activationCode ?: ""
        merchantsRef.document(id).update("status", status.name).await()
        if (code.isNotEmpty()) {
            val rtdbStatus = when (status) {
                MerchantStatus.ACTIVE -> mapOf("status" to "ACTIVE")
                MerchantStatus.DISABLED -> mapOf("status" to "DISABLED")
                MerchantStatus.EXPIRED -> mapOf("status" to "EXPIRED")
            }
            rtdb.child("activation_codes").child(code).setValue(rtdbStatus).await()
        }
    }

    suspend fun adjustExpiry(id: String, deltaDays: Int) {
        val merchant = getMerchantById(id) ?: return
        if (merchant.isPermanent) return

        val baseDate = merchant.expiryDate?.toDate() ?: java.util.Date()
        val cal = Calendar.getInstance().apply {
            time = baseDate
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        val newExpiry = Timestamp(cal.time)
        val isNowActive = cal.timeInMillis > System.currentTimeMillis()
        val newStatus = if (isNowActive) MerchantStatus.ACTIVE else MerchantStatus.EXPIRED

        merchantsRef.document(id).update(
            mapOf("expiryDate" to newExpiry, "status" to newStatus.name)
        ).await()

        if (merchant.activationCode.isNotEmpty()) {
            rtdb.child("activation_codes").child(merchant.activationCode)
                .setValue(mapOf("status" to newStatus.name)).await()
        }
    }

    suspend fun setSubscriptionType(
        id: String,
        isPermanent: Boolean,
        expiryDate: Timestamp?,
        planName: String?
    ) {
        val updates = mutableMapOf<String, Any?>("isPermanent" to isPermanent)
        if (planName != null) updates["planName"] = planName
        if (isPermanent) {
            updates["expiryDate"] = null
            updates["status"] = MerchantStatus.ACTIVE.name
        } else if (expiryDate != null) {
            updates["expiryDate"] = expiryDate
            val isActive = expiryDate.toDate().time > System.currentTimeMillis()
            updates["status"] =
                if (isActive) MerchantStatus.ACTIVE.name else MerchantStatus.EXPIRED.name
        }
        merchantsRef.document(id).update(updates).await()
    }

    private fun safeTimestamp(doc: DocumentSnapshot, field: String): Timestamp? =
        try {
            doc.getTimestamp(field) ?: when (val raw = doc.get(field)) {
                is Long -> Timestamp(java.util.Date(raw))
                is Double -> Timestamp(java.util.Date(raw.toLong()))
                else -> null
            }
        } catch (e: Exception) {
            null
        }

    private fun Merchant.toMap() = mapOf(
        "name" to name,
        "phone" to phone,
        "activationCode" to activationCode,
        "status" to status.name,
        "isPermanent" to isPermanent,
        "expiryDate" to expiryDate,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "lastSeen" to lastSeen,
        "tier" to tier.name,
        "isSelfRegistered" to isSelfRegistered,
        "planName" to planName,
        "paymentMethod" to paymentMethod
    )
}
