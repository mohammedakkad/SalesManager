package com.trader.core.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.model.MerchantTier
import kotlinx.coroutines.tasks.await

/**
 * Initializes Firebase Remote Config and pushes values to FeatureFlags.
 *
 * Call once from SalesManagerApp.onCreate() after Koin starts.
 *
 * Remote Config keys (all Boolean unless noted):
 *   month_report, returns_system, inventory_session, stock_reports,
 *   barcode_scanner, advanced_analytics, report_export, admin_chat,
 *   multi_unit, cost_price_tracking
 *
 * Firebase Console → Remote Config → create params with these keys.
 * Set default value = false for FREE, true for PREMIUM condition.
 */
object RemoteConfigManager {

    private val rc by lazy { FirebaseRemoteConfig.getInstance() }

    // ── defaults baked into the APK — Remote Config overrides these ──
    private val IN_APP_DEFAULTS = mapOf(
        "month_report"        to false,
        "returns_system"      to false,
        "inventory_session"   to false,
        "stock_reports"       to false,
        "barcode_scanner"     to false,
        "advanced_analytics"  to false,
        "report_export"       to false,
        "admin_chat"          to false,
        "multi_unit"          to false,
        "cost_price_tracking" to false
    )

    /**
     * Called from SalesManagerApp.
     * 1. Sets in-app defaults (works offline — instant).
     * 2. Fetches & activates latest config from Firebase.
     * 3. Applies flag overrides to FeatureFlags.
     *
     * @param merchantTier — read from DataStore / Firestore before calling
     */
    suspend fun initialize(merchantTier: MerchantTier) {
        // Step 1: configure + set defaults (sync, never throws)
        rc.setConfigSettingsAsync(remoteConfigSettings {
            // Dev: fetch every 60s. Prod: use default (12h)
            minimumFetchIntervalInSeconds = if (com.trader.core.BuildConfig.DEBUG) 60L else 43200L
        }).await()
        rc.setDefaultsAsync(IN_APP_DEFAULTS.mapValues { it.value }).await()

        // Step 2: apply base tier first (works offline)
        FeatureFlags.applyTier(merchantTier)

        // Step 3: fetch latest overrides (fails silently offline)
        runCatching {
            rc.fetchAndActivate().await()
            val overrides = IN_APP_DEFAULTS.keys.associateWith { key ->
                rc.getBoolean(key)
            }
            FeatureFlags.applyRemoteOverrides(overrides)
        }
        // offline → FeatureFlags keeps tier-based defaults — safe
    }
}
