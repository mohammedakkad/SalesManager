package com.trader.core.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import com.trader.core.domain.model.SubscriptionRequest
import com.trader.core.domain.model.SubscriptionState
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_prefs")

class SubscriptionManager(
    private val context: Context,
    private val firebaseSyncService: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) {

    private val KEY_EXPIRY = longPreferencesKey("sub_expiry")
    private val KEY_GRACE_END = longPreferencesKey("sub_grace_end")
    private val KEY_SERVER_OFFSET = longPreferencesKey("sub_server_offset")
    private val PLANS_JSON_KEY = stringPreferencesKey("plans_json")
    private val METHODS_JSON_KEY = stringPreferencesKey("methods_json")

    private val DEFAULT_PLANS = listOf(
        SubscriptionPlan("monthly", "الباقة الشهرية", "20₪", "شهرياً"),
        SubscriptionPlan("yearly", "الباقة السنوية", "150₪", "سنوياً", "وفّر 30%", true)
    )

    private val DEFAULT_METHODS = listOf(
        SubscriptionPaymentMethod("palpay", "Pal Pay", "0599708629"),
        SubscriptionPaymentMethod("bank", "بنك فلسطين", "0599708629")
    )

    val configFlow: Flow<Pair<List<SubscriptionPlan>, List<SubscriptionPaymentMethod>>> =
        context.subscriptionDataStore.data.map { prefs ->
            val plansJson = prefs[PLANS_JSON_KEY]
            val methodsJson = prefs[METHODS_JSON_KEY]
            val plans = if (plansJson.isNullOrEmpty()) DEFAULT_PLANS else parsePlans(plansJson)
            val methods =
                if (methodsJson.isNullOrEmpty()) DEFAULT_METHODS else parseMethods(methodsJson)
            Pair(plans, methods)
        }

    suspend fun syncAdminConfig() {
        runCatching {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("admin_settings/subscription")
                .get()
                .await()

            val plans = mutableListOf<SubscriptionPlan>()
            snapshot.child("plans").children.forEach {
                plans.add(
                    SubscriptionPlan(
                        id = it.child("id").getValue(String::class.java) ?: "",
                        arabicLabel = it.child("arabicLabel").getValue(String::class.java) ?: "",
                        priceLabel = it.child("priceLabel").getValue(String::class.java) ?: "",
                        periodLabel = it.child("periodLabel").getValue(String::class.java) ?: "",
                        savingBadge = it.child("savingBadge").getValue(String::class.java),
                        isRecommended = it.child("isRecommended")
                            .getValue(Boolean::class.java) == true
                    )
                )
            }

            val methods = mutableListOf<SubscriptionPaymentMethod>()
            snapshot.child("paymentMethods").children.forEach {
                methods.add(
                    SubscriptionPaymentMethod(
                        id = it.child("id").getValue(String::class.java) ?: "",
                        name = it.child("name").getValue(String::class.java) ?: "",
                        accountNumber = it.child("accountNumber").getValue(String::class.java) ?: ""
                    )
                )
            }

            if (plans.isNotEmpty() || methods.isNotEmpty()) {
                context.subscriptionDataStore.edit { prefs ->
                    if (plans.isNotEmpty()) prefs[PLANS_JSON_KEY] = serializePlans(plans)
                    if (methods.isNotEmpty()) prefs[METHODS_JSON_KEY] = serializeMethods(methods)
                }
            }
        }
    }

    suspend fun syncSubscriptionFromRemote(merchantCode: String) {
        runCatching {
            val snapshot = firebaseSyncService.fetchMerchantActivationData(merchantCode)
            if (!snapshot.exists()) return

            val tierRaw = snapshot.child("tier").getValue(String::class.java)
            val tier = runCatching { MerchantTier.valueOf(tierRaw ?: "") }.getOrDefault(MerchantTier.FREE)
            val expiry = snapshot.child("subscriptionExpiry").getValue(Long::class.java) ?: 0L

            activationRepo.saveMerchantTier(tier)
            updateLocalExpiryInfo(expiry)
        }
    }
    private suspend fun updateLocalExpiryInfo(expiry: Long) {
        context.subscriptionDataStore.edit { prefs ->
            prefs[KEY_EXPIRY] = expiry
            prefs[KEY_GRACE_END] = expiry + GRACE_PERIOD_MS
        }
    }

    suspend fun clearPendingState() {
        if (activationRepo.getMerchantTier() == MerchantTier.PENDING) {
            activationRepo.saveMerchantTier(MerchantTier.FREE)
        }
    }

    private fun serializePlans(plans: List<SubscriptionPlan>): String {
        val arr = JSONArray()
        plans.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("arabicLabel", it.arabicLabel)
                put("priceLabel", it.priceLabel)
                put("periodLabel", it.periodLabel)
                put("savingBadge", it.savingBadge)
                put("isRecommended", it.isRecommended)
            })
        }
        return arr.toString()
    }

    private fun parsePlans(json: String): List<SubscriptionPlan> = runCatching {
        val list = mutableListOf<SubscriptionPlan>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SubscriptionPlan(
                    obj.getString("id"), obj.getString("arabicLabel"),
                    obj.getString("priceLabel"), obj.getString("periodLabel"),
                    obj.optionalString("savingBadge"), obj.getBoolean("isRecommended")
                )
            )
        }
        list
    }.getOrDefault(DEFAULT_PLANS)

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    private fun serializeMethods(methods: List<SubscriptionPaymentMethod>): String {
        val arr = JSONArray()
        methods.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("accountNumber", it.accountNumber)
            })
        }
        return arr.toString()
    }

    private fun parseMethods(json: String): List<SubscriptionPaymentMethod> = runCatching {
        val list = mutableListOf<SubscriptionPaymentMethod>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SubscriptionPaymentMethod(
                    obj.getString("id"), obj.getString("name"), obj.getString("accountNumber")
                )
            )
        }
        list
    }.getOrDefault(DEFAULT_METHODS)

    val subscriptionState: Flow<SubscriptionState> = combine(
        context.subscriptionDataStore.data,
        activationRepo.observeMerchantTier()
    ) { prefs, tier ->
        val expiry = prefs[KEY_EXPIRY] ?: 0L
        val graceEnd = prefs[KEY_GRACE_END] ?: 0L
        val offset = prefs[KEY_SERVER_OFFSET] ?: 0L
        val now = System.currentTimeMillis() + offset

        val isExpired = tier == MerchantTier.PREMIUM && expiry > 0L && now > expiry
        val isInGrace = isExpired && graceEnd > 0L && now <= graceEnd

        SubscriptionState(tier, expiry, isExpired, isInGrace, graceEnd)
    }

    suspend fun markPending() {
        activationRepo.saveMerchantTier(MerchantTier.PENDING)
    }

    suspend fun pushSubscriptionRequest(merchantCode: String, request: SubscriptionRequest) {
        val db = FirebaseDatabase.getInstance()
        val key = db.getReference("subscription_requests").child(merchantCode).push().key ?: return
        val payload = request.toFirebaseMap().toMutableMap()
        payload["requestedAt"] = ServerValue.TIMESTAMP
        db.getReference("subscription_requests").child(merchantCode).child(key).setValue(payload)
            .await()
    }

    suspend fun syncServerTimeOffset() {
        runCatching {
            val offset =
                FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").get().await()
                    .getValue(Long::class.java) ?: 0L
            context.subscriptionDataStore.edit { it[KEY_SERVER_OFFSET] = offset }
        }
    }

    companion object {
        private const val GRACE_PERIOD_MS = 7L * 24L * 60L * 60L * 1_000L
    }
}
