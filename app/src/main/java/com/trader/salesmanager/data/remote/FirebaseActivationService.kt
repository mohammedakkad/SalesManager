package com.trader.salesmanager.data.remote

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseActivationService {
    private val db = FirebaseDatabase.getInstance()

    suspend fun validateCode(code: String): Boolean {
        return try {
            val snapshot = db.reference.child("activation_codes").child(code).get().await()
            snapshot.exists() && snapshot.getValue(Boolean::class.java) == true
        } catch (e: Exception) {
            false
        }
    }
}