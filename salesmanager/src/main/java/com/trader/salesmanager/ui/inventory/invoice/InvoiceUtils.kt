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
    .replace(',', '.')    // ✅ الفاصلة العربية → نقطة عشرية

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
