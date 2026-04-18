package com.trader.salesmanager.ui.inventory.invoice

import java.util.Locale

// ══════════════════════════════════════════════════════════════════
// أرقام — جميع التحويلات تمر هنا حتى لا يُفسد Locale الجهاز الأرقام
// ══════════════════════════════════════════════════════════════════

/** تحويل الأرقام العربية/الفارسية إلى لاتينية + استبدال الفاصلة بنقطة */
fun String.toLatinDigits(): String = this
.replace('٠', '0').replace('١', '1').replace('٢', '2')
.replace('٣', '3').replace('٤', '4').replace('٥', '5')
.replace('٦', '6').replace('٧', '7').replace('٨', '8')
.replace('٩', '9').replace('۰', '0').replace('۱', '1')
.replace('۲', '2').replace('۳', '3').replace('۴', '4')
.replace('۵', '5').replace('۶', '6').replace('۷', '7')
.replace('۸', '8').replace('۹', '9')
.replace(',', '.') // ✅ الفاصلة العربية → نقطة عشرية

/**
 * تحويل String → Double بأمان مع دعم الأرقام العربية والفاصلة العربية.
 * يجب استخدام هذه الدالة بدلاً من toDoubleOrNull() في كل مكان.
 */
fun String.toSafeDouble(): Double? = this.toLatinDigits().toDoubleOrNull()

/**
 * تنسيق Double → String بـ Locale.US دائماً (أرقام لاتينية، نقطة عشرية).
 * يجب استخدام هذه الدالة بدلاً من String.format("%.2f", ...) في كل مكان.
 */
fun Double.formatAmount(decimals: Int = 2): String =
String.format(Locale.US, "%.${decimals}f", this)

/** نفس formatAmount لكن مع إزالة الأصفار الزائدة: 1.00 → "1"، 1.50 → "1.5" */
fun Double.formatQty(): String {
    val s = String.format(Locale.US, "%.3f", this).trimEnd('0').trimEnd('.')
    return if (s.isEmpty()) "0" else s
}

// ══════════════════════════════════════════════════════════════════
// تحويل وحدات الوزن — البائع يُدخل بالوحدة المريحة، النظام يخزن بالكيلو
// ══════════════════════════════════════════════════════════════════

/**
 * وحدات الوزن التجارية المدعومة.
 * [labelAr]   = التسمية المعروضة للبائع
 * [toKg]      = معامل التحويل إلى كيلو  (1 وحدة = X كيلو)
 * [fromKg]    = معامل التحويل من كيلو   (1 كيلو = X وحدة)
 */
enum class SaleWeightUnit(
    val labelAr: String,
    val toKg: Double
) {
    KG ("كيلو", 1.0),
    GRAM ("غرام", 0.001), // 1 غرام   = 0.001 كيلو
    OZ ("أوقية", 0.250), // 1 أوقية  = 250 غرام  (العرفية المحلية)
    POUND ("رطل", 4.0); // 1 رطل    = 453.6 غرام

    val fromKg: Double get() = 1.0 / toKg

    companion object {
        /** الوحدة الافتراضية — نفس وحدة التخزين في المخزن */
        val DEFAULT = KG
    }
}

/**
 * يحوّل الكمية من وحدة البيع إلى الكيلو (وحدة التخزين في المخزن).
 *
 * مثال: displayQty=2, unit=POUND  →  2 × 0.4 = 0.9072 كيلو
 */
fun toKgQuantity(displayQty: Double, unit: SaleWeightUnit): Double =
displayQty * unit.toKg

/**
 * يحوّل الكمية من الكيلو إلى وحدة العرض.
 *
 * مثال: kgQty=0.9072, unit=POUND  →  0.9072 / 0.4 = 2 رطل
 */
fun fromKgQuantity(kgQty: Double, unit: SaleWeightUnit): Double =
kgQty / unit.toKg