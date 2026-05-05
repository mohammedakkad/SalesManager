package com.trader.core.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.model.SubscriptionRequest
import com.trader.core.domain.model.SubscriptionState
import com.trader.core.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_prefs")

class SubscriptionManager(private val context: Context) {

    private val KEY_TIER        = stringPreferencesKey("sub_tier")
    private val KEY_EXPIRY      = longPreferencesKey("sub_expiry")
    private val KEY_GRACE_END   = longPreferencesKey("sub_grace_end")
    private val KEY_SERVER_OFFSET = longPreferencesKey("sub_server_offset")

    val subscriptionState: Flow<SubscriptionState> =
        context.subscriptionDataStore.data.map { prefs ->
            val tierName = prefs[KEY_TIER] ?: MerchantTier.FREE.name
            val tier     = runCatching { MerchantTier.valueOf(tierName) }
                .getOrDefault(MerchantTier.FREE)
            val expiry   = prefs[KEY_EXPIRY]    ?: 0L
            val graceEnd = prefs[KEY_GRACE_END] ?: 0L
            val offset   = prefs[KEY_SERVER_OFFSET] ?: 0L
            val now      = System.currentTimeMillis() + offset

            val isExpired  = tier == MerchantTier.PREMIUM && expiry > 0L && now > expiry
            val isInGrace  = isExpired && graceEnd > 0L && now <= graceEnd

            SubscriptionState(
                tier              = tier,
                expiryDate        = expiry,
                isExpired         = isExpired,
                isInGracePeriod   = isInGrace,
                gracePeriodEndsAt = graceEnd
            )
        }

    suspend fun markPending() {
        context.subscriptionDataStore.edit { prefs ->
            prefs[KEY_TIER] = MerchantTier.PENDING.name
        }
    }

    suspend fun approve(expiryDateMs: Long) {
        context.subscriptionDataStore.edit { prefs ->
            prefs[KEY_TIER]      = MerchantTier.PREMIUM.name
            prefs[KEY_EXPIRY]    = expiryDateMs
            prefs[KEY_GRACE_END] = expiryDateMs + GRACE_PERIOD_MS
        }
    }

    suspend fun reject() {
        context.subscriptionDataStore.edit { prefs ->
            prefs[KEY_TIER]      = MerchantTier.FREE.name
            prefs[KEY_EXPIRY]    = 0L
            prefs[KEY_GRACE_END] = 0L
        }
    }

    suspend fun restoreFromFirebase(tier: MerchantTier, expiryDateMs: Long) {
        context.subscriptionDataStore.edit { prefs ->
            prefs[KEY_TIER]   = tier.name
            prefs[KEY_EXPIRY] = expiryDateMs
            prefs[KEY_GRACE_END] = if (tier == MerchantTier.PREMIUM && expiryDateMs > 0L) {
                expiryDateMs + GRACE_PERIOD_MS
            } else {
                0L
            }
        }
    }

    suspend fun syncServerTimeOffset() {
        runCatching {
            val ref = FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset")
            val snapshot = ref.get().await()
            val offset = snapshot.getValue(Long::class.java) ?: 0L
            context.subscriptionDataStore.edit { prefs ->
                prefs[KEY_SERVER_OFFSET] = offset
            }
        }
    }

    suspend fun pushSubscriptionRequest(
        merchantCode: String,
        request: SubscriptionRequest
    ) {
        val db  = FirebaseDatabase.getInstance()
        val key = db.getReference("subscription_requests")
            .child(merchantCode)
            .push()
            .key ?: throw IllegalStateException("Firebase push key was null")

        val payload = request.toFirebaseMap().toMutableMap()
        payload["requestedAt"] = ServerValue.TIMESTAMP

        db.getReference("subscription_requests")
            .child(merchantCode)
            .child(key)
            .setValue(payload)
            .await()
    }

    suspend fun fetchPendingRequestFromFirebase(merchantCode: String): Boolean {
        val snapshot = FirebaseDatabase.getInstance()
            .getReference("subscription_requests")
            .child(merchantCode)
            .orderByChild("status")
            .equalTo(SubscriptionStatus.PENDING.name)
            .get()
            .await()
        return snapshot.exists()
    }

    suspend fun fetchAndRestoreTierFromFirebase(merchantCode: String) {
        val snapshot = FirebaseDatabase.getInstance()
            .getReference("merchants")
            .child(merchantCode)
            .get()
            .await()

        val tierRaw  = snapshot.child("tier").getValue(String::class.java)
        val expiry   = snapshot.child("subscriptionExpiry").getValue(Long::class.java) ?: 0L
        val tier     = runCatching { MerchantTier.valueOf(tierRaw ?: "") }
            .getOrDefault(MerchantTier.FREE)

        val hasPending = fetchPendingRequestFromFirebase(merchantCode)

        if (hasPending) {
            markPending()
        } else {
            restoreFromFirebase(tier, expiry)
        }
    }

    companion object {
        private const val GRACE_PERIOD_MS = 7L * 24L * 60L * 60L * 1_000L
    }
}
