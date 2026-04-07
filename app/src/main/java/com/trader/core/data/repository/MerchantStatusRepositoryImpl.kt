package com.trader.core.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantStatusRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

class MerchantStatusRepositoryImpl : MerchantStatusRepository {
    private val db = FirebaseDatabase.getInstance()

    override fun observeMerchantStatus(code: String): Flow<MerchantStatus?> = callbackFlow {
        val ref = db.reference.child("activation_codes").child(code)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null) // deleted
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java)
                val active = snapshot.getValue(Boolean::class.java)
                when {
                    status != null -> trySend(
                        try { MerchantStatus.valueOf(status) } catch (e: Exception) { MerchantStatus.ACTIVE }
                    )
                    active == false -> trySend(MerchantStatus.DISABLED)
                    else -> trySend(MerchantStatus.ACTIVE)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeExpiryDays(code: String): Flow<Long?> = callbackFlow {
        val ref = db.reference.child("activation_codes").child(code)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isPermanent = snapshot.child("isPermanent").getValue(Boolean::class.java) ?: true
                if (isPermanent) { trySend(null); return }
                val expiryMs = snapshot.child("expiryDate").getValue(Long::class.java)
                if (expiryMs == null) { trySend(null); return }
                val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryMs - System.currentTimeMillis())
                trySend(daysLeft)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}