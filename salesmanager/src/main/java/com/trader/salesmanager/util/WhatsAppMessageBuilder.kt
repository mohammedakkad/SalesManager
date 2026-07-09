package com.trader.salesmanager.util

import android.content.Context
import com.trader.core.util.DateUtils.toDateString
import com.trader.salesmanager.R
import java.util.Locale

object WhatsAppMessageBuilder {

    fun build(
        context: Context,
        customerName: String,
        amount: Double,
        dueDate: Long,
        storeName: String
    ): String {
        val formattedAmount = String.format(Locale.US, "%,.2f", amount)
        val formattedDate = dueDate.toDateString()
        val store = storeName.ifBlank { context.getString(R.string.whatsapp_default_store_name) }
        return context.getString(
            R.string.whatsapp_debt_reminder_message,
            customerName,
            formattedAmount,
            formattedDate,
            store
        )
    }

    fun sanitizePhone(phone: String): String = phone.filter { it.isDigit() }

    fun isPhoneLikelyInvalid(phone: String): Boolean {
        val digits = sanitizePhone(phone)
        return digits.isEmpty() || digits.length < 9
    }

    fun buildWaMeUrl(phone: String, message: String): String {
        val digits = sanitizePhone(phone)
        val encoded = java.net.URLEncoder.encode(message, Charsets.UTF_8.name())
        return "https://wa.me/$digits?text=$encoded"
    }
}
