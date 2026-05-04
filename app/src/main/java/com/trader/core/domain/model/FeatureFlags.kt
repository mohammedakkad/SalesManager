package com.trader.core.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central feature flag store.
 *
 * Values come from RemoteConfigManager (Firebase Remote Config).
 * Fallback defaults = most restrictive (FREE) — never crash offline.
 *
 * Usage in any Composable:
 *   val flags by FeatureFlags.flow.collectAsState()
 *   if (flags.returnsSystem) { ... }
 */
object FeatureFlags {

    data class FlagSet(
        val tier: MerchantTier = MerchantTier.FREE,

        // ── Limits ──────────────────────────────────────────────
        val maxProducts:       Int = 20,
        val maxPaymentMethods: Int = 3,

        // ── Free features (always true) ──────────────────────────
        val transactions: Boolean = true,
        val customers:    Boolean = true,
        val debtTracking: Boolean = true,
        val whatsappInvoice: Boolean = true,
        val basicReports: Boolean = true,
        val darkMode: Boolean = true,

        // ── Period control ───────────────────────────────────────
        val monthReport: Boolean = false,   // FREE: today+week only

        // ── Premium features ─────────────────────────────────────
        val costPriceTracking: Boolean = false,
        val returnsSystem:     Boolean = false,
        val inventorySession:  Boolean = false,
        val stockReports:      Boolean = false,
        val barcodeScanner:    Boolean = false,
        val advancedAnalytics: Boolean = false,
        val reportExport:      Boolean = false,
        val adminChat:         Boolean = false,
        val multiUnit:         Boolean = false,
        val weightConversion:  Boolean = false
    ) {
        val isPremium: Boolean get() = tier == MerchantTier.PREMIUM
    }

    // ── FREE defaults ───────────────────────────────────────────
    private val FREE_FLAGS = FlagSet(tier = MerchantTier.FREE)

    // ── PREMIUM defaults (Remote Config may override these) ──────
    private val PREMIUM_FLAGS = FlagSet(
        tier                = MerchantTier.PREMIUM,
        maxProducts         = Int.MAX_VALUE,
        maxPaymentMethods   = Int.MAX_VALUE,
        monthReport         = true,
        costPriceTracking   = true,
        returnsSystem       = true,
        inventorySession    = true,
        stockReports        = true,
        barcodeScanner      = true,
        advancedAnalytics   = true,
        reportExport        = true,
        adminChat           = true,
        multiUnit           = true,
        weightConversion    = true
    )

    private val _flow = MutableStateFlow(FREE_FLAGS)
    val flow: StateFlow<FlagSet> = _flow.asStateFlow()

    /** Current snapshot — use `flow.collectAsState()` in Composables */
    val current: FlagSet get() = _flow.value

    /** Called by RemoteConfigManager after fetching config */
    fun applyTier(tier: MerchantTier) {
        _flow.value = if (tier == MerchantTier.PREMIUM) PREMIUM_FLAGS else FREE_FLAGS
    }

    /** Override individual flags from Remote Config values */
    fun applyRemoteOverrides(overrides: Map<String, Boolean>) {
        val base = _flow.value
        _flow.value = base.copy(
            monthReport       = overrides["month_report"]        ?: base.monthReport,
            returnsSystem     = overrides["returns_system"]      ?: base.returnsSystem,
            inventorySession  = overrides["inventory_session"]   ?: base.inventorySession,
            stockReports      = overrides["stock_reports"]       ?: base.stockReports,
            barcodeScanner    = overrides["barcode_scanner"]     ?: base.barcodeScanner,
            advancedAnalytics = overrides["advanced_analytics"]  ?: base.advancedAnalytics,
            reportExport      = overrides["report_export"]       ?: base.reportExport,
            adminChat         = overrides["admin_chat"]          ?: base.adminChat,
            multiUnit         = overrides["multi_unit"]          ?: base.multiUnit,
            costPriceTracking = overrides["cost_price_tracking"] ?: base.costPriceTracking
        )
    }
}
