package com.trader.salesmanager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

object InvoiceSharer {

    /**
     * ينشئ نص فاتورة منسق ويفتح واتساب مباشرة.
     * إذا لم يكن واتساب مثبتاً يفتح أي تطبيق مشاركة.
     */
    fun shareInvoice(
        context: Context,
        transaction: Transaction,
        items: List<InvoiceItem>,
        merchantName: String = "المتجر"
    ) {
        val text = buildInvoiceText(transaction, items, merchantName)
        openWhatsApp(context, text)
    }

    fun buildInvoiceText(
        transaction: Transaction,
        items: List<InvoiceItem>,
        merchantName: String
    ): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("ar"))
            .format(Date(transaction.date))

        return buildString {
            appendLine("🧾 *فاتورة من $merchantName*")
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

                    appendLine(
                        "• ${item.productName} (${item.unitLabel}) × $qtyStr" +
                        " = ₪${String.format("%.2f", item.totalPrice)}"
                    )
                }
                appendLine("━━━━━━━━━━━━━━━━━━━━")
            }

            appendLine("💰 *الإجمالي:* ₪${String.format("%.2f", transaction.amount)}")

            val paymentStr = when (transaction.paymentType) {
                PaymentType.CASH   -> "✅ كاش"
                PaymentType.DEBT   -> "📋 دين"
                PaymentType.BANK   -> "🏦 بنك"
                PaymentType.WALLET -> "💳 محفظة"
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
        // محاولة فتح واتساب مباشرة
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // إذا واتساب غير مثبت — نستخدم أي تطبيق
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
