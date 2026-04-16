package com.trader.salesmanager.util

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

object InvoiceSharer {

    /**
     * يقرأ اسم المحل من DataStore ثم يشارك الفاتورة.
     * يُستدعى من Composable لذا لا نستخدم runBlocking هنا —
     * بل نمرر storeName كمعامل من الـ UI.
     */
    fun shareInvoice(
        context: Context,
        transaction: Transaction,
        items: List<InvoiceItem>,
        storeName: String   // ← يُمرَّر من الـ UI عبر collectAsState
    ) {
        val text = buildInvoiceText(transaction, items, storeName)
        openWhatsApp(context, text)
    }

    fun buildInvoiceText(
        transaction: Transaction,
        items: List<InvoiceItem>,
        storeName: String
    ): String {
        val displayName = storeName.ifBlank { "المتجر" }
        val dateStr = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("ar"))
            .format(Date(transaction.date))

        return buildString {
            appendLine("🧾 *فاتورة من $displayName*")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📅 *التاريخ:* $dateStr")
            appendLine("👤 *الزبون:* ${transaction.customerName}")
            appendLine()

            if (items.isNotEmpty()) {
                appendLine("*الأصناف:*")
                items.forEach { item ->
                    val qtyStr = if (item.quantity == item.quantity.toLong().toDouble())
                        item.quantity.toLong().toString()
                    else String.format("%.3f", item.quantity).trimEnd('0').trimEnd('.')
                    appendLine("• ${item.productName} (${item.unitLabel}) × $qtyStr = ₪${String.format("%.2f", item.totalPrice)}")
                }
                appendLine("━━━━━━━━━━━━━━━━━━━━")
            }

            appendLine("💰 *الإجمالي:* ₪${String.format("%.2f", transaction.amount)}")
            val paymentStr = when (transaction.paymentType) {
                PaymentType.CASH   -> "✅ كاش"
                PaymentType.DEBT   -> "📋 دين"
                PaymentType.BANK   -> "🏦 بنك"
                PaymentType.WALLET -> "💳 محفظة"
                PaymentType.OTHER  -> "💰 أخرى"
            }
            appendLine("💳 *طريقة الدفع:* $paymentStr")
            if (transaction.isPaid) appendLine("✅ *الحالة:* مدفوع")
            else appendLine("⏳ *الحالة:* غير مدفوع")
            if (transaction.note.isNotEmpty()) {
                appendLine()
                appendLine("📝 *ملاحظة:* ${transaction.note}")
            }
            appendLine()
            appendLine("_شكراً لتعاملك معنا_ 🙏")
        }
    }

    private fun openWhatsApp(context: Context, text: String) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent.createChooser(fallbackIntent, "مشاركة الفاتورة")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
        }
    }
}
