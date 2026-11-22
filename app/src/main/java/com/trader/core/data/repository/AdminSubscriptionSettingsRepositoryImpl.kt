package com.trader.core.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import com.trader.core.domain.repository.AdminSubscriptionSettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminSubscriptionSettingsRepositoryImpl(firebaseDatabase: FirebaseDatabase) :
    AdminSubscriptionSettingsRepository {

    private val configRef = firebaseDatabase
        .getReference("admin_settings/subscription")

    override fun getSubscriptionConfig(): Flow<Pair<List<SubscriptionPlan>, List<SubscriptionPaymentMethod>>> =
        callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val plans = snapshot.child("plans").children.mapNotNull { child ->
                        runCatching {
                            SubscriptionPlan(
                                id = child.child("id").getValue(String::class.java)
                                    ?: return@mapNotNull null,
                                arabicLabel = child.child("arabicLabel")
                                    .getValue(String::class.java) ?: "",
                                priceLabel = child.child("priceLabel").getValue(String::class.java)
                                    ?: "",
                                periodLabel = child.child("periodLabel")
                                    .getValue(String::class.java) ?: "",
                                savingBadge = child.child("savingBadge")
                                    .getValue(String::class.java),
                                isRecommended = child.child("isRecommended")
                                    .getValue(Boolean::class.java) == true
                            )
                        }.getOrNull()
                    }

                    val methods = snapshot.child("paymentMethods").children.mapNotNull { child ->
                        runCatching {
                            SubscriptionPaymentMethod(
                                id = child.child("id").getValue(String::class.java)
                                    ?: return@mapNotNull null,
                                name = child.child("name").getValue(String::class.java) ?: "",
                                accountNumber = child.child("accountNumber")
                                    .getValue(String::class.java) ?: ""
                            )
                        }.getOrNull()
                    }

                    trySend(Pair(plans, methods))
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            configRef.addValueEventListener(listener)
            awaitClose { configRef.removeEventListener(listener) }
        }

    override suspend fun savePlans(plans: List<SubscriptionPlan>) {
        val payload = plans.associate { plan ->
            plan.id to buildMap<String, Any> {
                put("id", plan.id)
                put("arabicLabel", plan.arabicLabel)
                put("priceLabel", plan.priceLabel)
                put("periodLabel", plan.periodLabel)
                put("isRecommended", plan.isRecommended)
                plan.savingBadge?.let { put("savingBadge", it) }
            }
        }
        configRef.child("plans").setValue(payload).await()
    }

    override suspend fun savePaymentMethods(methods: List<SubscriptionPaymentMethod>) {
        val payload = methods.associate { method ->
            method.id to mapOf(
                "id" to method.id,
                "name" to method.name,
                "accountNumber" to method.accountNumber
            )
        }
        configRef.child("paymentMethods").setValue(payload).await()
    }
}