package com.trader.core.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import java.security.MessageDigest
import java.util.UUID

/**
 * DeviceFingerprintProvider — يُنتج بصمة جهاز مستقرة لربط التفعيل بجهاز محدد.
 *
 * الاستراتيجية متعددة الطبقات:
 * 1. Primary fingerprint: مُركَّبة من Build constants (لا تتغير مطلقاً عبر resets)
 * 2. Enriched fingerprint: تُضاف ANDROID_ID + UUID محلي للتمييز بين نسخ المصنع
 * 3. Persisted UUID: يُخزَّن في SharedPreferences كـ fallback عند تغيير ANDROID_ID
 *
 * Single Responsibility: مسؤول فقط عن توليد وحفظ بصمة الجهاز.
 * يُسجَّل كـ single() في Koin — نسخة واحدة طوال عمر التطبيق.
 */
class DeviceFingerprintProvider(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "device_identity_prefs"
        private const val KEY_PERSISTED_UUID = "persisted_device_uuid"
        private const val KEY_CACHED_FINGERPRINT = "cached_fingerprint"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * يُعيد بصمة الجهاز المُركَّبة — مُخبَّأة بعد أول توليد.
     * مستقرة عبر: Clear Data ❌ → نعم لأن SharedPrefs يُفقد، لكن Build constants تبقى
     * مستقرة عبر: Factory Reset → ANDROID_ID يتغير لكن Build constants تبقى (بصمة تتغير — مقصود)
     * مستقرة عبر: تغيير User Profile → نعم، Build constants مستقلة عن المستخدم
     */
    fun getFingerprint(): String {
        val cached = prefs.getString(KEY_CACHED_FINGERPRINT, null)
        if (!cached.isNullOrBlank()) return cached

        val fresh = buildFingerprint()
        prefs.edit { putString(KEY_CACHED_FINGERPRINT, fresh) }
        return fresh
    }

    /**
     * معلومات مقروءة عن الجهاز — تُعرَض في شاشة الجلسات.
     */
    fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        fingerprint = getFingerprint(),
        model = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        brand = Build.BRAND,
        androidVersion = Build.VERSION.RELEASE
    )

    // ── Private ───────────────────────────────────────────────────

    @SuppressLint("HardwareIds")
    private fun buildFingerprint(): String {
        // Layer 1: Build constants — ثابتة مطلقاً، تُميِّز طراز الجهاز
        val buildString = listOf(
            Build.BOARD, Build.BRAND, Build.DEVICE,
            Build.HARDWARE, Build.MANUFACTURER, Build.MODEL,
            Build.PRODUCT, Build.BOOTLOADER
        )
            .filter { it.isNotBlank() && it != Build.UNKNOWN }
            .joinToString("|")

        // Layer 2: ANDROID_ID — يُضيف entropy، يتجاهل القيمة المزيفة الشائعة
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ).takeIf { it?.isNotBlank() == true && it != "9774d56d682e549c" } ?: ""

        // Layer 3: UUID محلي مُستمَر — يُميِّز بين أجهزة متطابقة الطراز
        val uuid = getOrCreatePersistedUuid()

        return sha256("$buildString|$androidId|$uuid").take(32)
    }

    private fun getOrCreatePersistedUuid(): String {
        val existing = prefs.getString(KEY_PERSISTED_UUID, null)
        if (!existing.isNullOrBlank()) return existing
        val newUuid = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_PERSISTED_UUID, newUuid) }
        return newUuid
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

/**
 * معلومات الجهاز المقروءة بشرياً — للعرض في شاشة الجلسات.
 */
data class DeviceInfo(
    val fingerprint: String,
    val model: String,
    val brand: String,
    val androidVersion: String
) {
    val displayName: String get() = "$model (Android $androidVersion)"
}
