package com.trader.core.data.migration

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class FirestoreMigrationHelper(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun migrateAll() {
        val merchantIds = listOf(
            "DP0cj8KhDIGlOCbvQab7",
            "tp20feNZ2UV0gwsiIDUu",
            "ionSHvNPlxFKudru5Wkm",
            "VXYAWGAT",
            "WhPSMTrjVafBg28bblMc",
            "EY9PUU37"
        )
        merchantIds.forEach {
            id ->
            migrateUnitsToSubcollection(id)
        }
    }

    private suspend fun migrateUnitsToSubcollection(merchantId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("Migration", "بدأ ترحيل: $merchantId")

                val oldUnits = db.collection("merchants")
                .document(merchantId)
                .collection("product_units")
                .get().await()

                if (oldUnits.isEmpty) {
                    Log.d("Migration", "[$merchantId] لا توجد وحدات — تم التخطي")
                    return@withContext
                }

                Log.d("Migration", "[$merchantId] عدد الوحدات: ${oldUnits.size()}")

                oldUnits.documents.chunked(400).forEach {
                    chunk ->
                    val batch = db.batch()
                    chunk.forEach {
                        doc ->
                        val productId = doc.getString("productId")
                        if (productId.isNullOrEmpty()) return@forEach
                        val newRef = db.collection("merchants")
                        .document(merchantId)
                        .collection("products")
                        .document(productId)
                        .collection("units")
                        .document(doc.id)
                        batch.set(newRef, doc.data!!)
                    }
                    batch.commit().await()
                }

                Log.d("Migration", "[$merchantId] ✅ اكتمل")

            } catch (e: Exception) {
                Log.e("Migration", "[$merchantId] ❌ فشل: ${e.message}")
            }
        }
    }
}