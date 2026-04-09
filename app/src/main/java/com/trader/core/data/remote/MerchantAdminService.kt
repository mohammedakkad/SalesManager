package com.trader.core.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class MerchantAdminService {
    private val db = FirebaseFirestore.getInstance()
    private val realTimeDatabase = FirebaseDatabase.getInstance().reference
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
                        expiryDate = doc.getTimestamp("expiryDate"),
                        createdAt = doc.getTimestamp("createdAt"),
                        lastSeen = doc.getTimestamp("lastSeen")
                    )
                } catch (_: Exception) {
                    null
                }
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
                id = doc.id,
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                activationCode = data["activationCode"] as? String ?: "",
                status = MerchantStatus.valueOf(data["status"] as? String ?: "ACTIVE"),
                isPermanent = data["isPermanent"] as? Boolean != false,
                expiryDate = doc.getTimestamp("expiryDate"),
                createdAt = doc.getTimestamp("createdAt"),
                lastSeen = doc.getTimestamp("lastSeen")
            )
        } catch (_: Exception) {
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
        // Get code first to clean up Real Time Database too
        val code = getMerchantById(id)?.activationCode
        merchantsRef.document(id).delete().await()
        if (!code.isNullOrEmpty()) {
            realTimeDatabase.child("activation_codes").child(code).removeValue().await()
        }
    }

    /**
     * FIX: Updates BOTH Firestore (status field) AND Realtime Database
     * (activation_codes/{code}) so that the merchant app validation stays in sync.
     * Previously only Firestore was updated → merchant could still re-activate
     * with a disabled code because Real Time Database still showed "ACTIVE".
     */
    suspend fun setStatus(id: String, status: MerchantStatus) {
        val code = getMerchantById(id)?.activationCode ?: ""

        // 1. Update Firestore
        merchantsRef.document(id).update("status", status.name).await()

        // 2. Sync Realtime Database so merchant app picks up the change
        if (code.isNotEmpty()) {
            val realTimeDatabaseStatus = when (status) {
                MerchantStatus.ACTIVE -> mapOf("status" to "ACTIVE")
                MerchantStatus.DISABLED -> mapOf("status" to "DISABLED")
                MerchantStatus.EXPIRED -> mapOf("status" to "EXPIRED")
            }
            realTimeDatabase.child("activation_codes").child(code).setValue(realTimeDatabaseStatus).await()
        }
    }

    /**
     * Adjusts expiry date by [deltaDays] days (positive = extend, negative = reduce).
     * Also updates status to ACTIVE if it was EXPIRED and days > 0.
     */
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

        // Update Firestore
        merchantsRef.document(id).update(
            mapOf(
                "expiryDate" to newExpiry,
                "status" to newStatus.name
            )
        ).await()

        // Sync Real Time Database
        if (merchant.activationCode.isNotEmpty()) {
            realTimeDatabase.child("activation_codes").child(merchant.activationCode)
                .setValue(mapOf("status" to newStatus.name)).await()
        }
    }

    private fun Merchant.toMap() = mapOf(
        "name" to name,
        "phone" to phone,
        "activationCode" to activationCode,
        "status" to status.name,
        "isPermanent" to isPermanent,
        "expiryDate" to expiryDate,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "lastSeen" to lastSeen
    )
}
