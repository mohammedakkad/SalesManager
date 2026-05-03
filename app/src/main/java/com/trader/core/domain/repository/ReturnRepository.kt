package com.trader.core.domain.repository

import com.trader.core.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ReturnRepository {

    /**
     * الدالة الرئيسية — تعالج كل خطوات الإرجاع في transaction ذرية:
     * 1. التحقق من الكميات
     * 2. حفظ فاتورة الإرجاع
     * 3. إعادة المخزون
     * 4. تعديل مبلغ العملية الأصلية
     * 5. تحديث returnStatus في العملية
     *
     * @throws IllegalArgumentException إذا تجاوزت الكمية المسموح بها
     */
    suspend fun processReturn(
        returnInvoice: ReturnInvoice,
        items: List<ReturnItem>
    ): ReturnInvoice

    /** فواتير الإرجاع المرتبطة بعملية معينة */
    fun getReturnsByTransaction(transactionId: Long): Flow<List<ReturnInvoice>>

    /** أصناف فاتورة إرجاع معينة */
    suspend fun getReturnItems(returnInvoiceId: String): List<ReturnItem>

    /** كل فواتير الإرجاع للتاجر */
    fun getAllReturns(): Flow<List<ReturnInvoice>>

    /**
     * ملخص الإرجاع لعملية واحدة — للعرض في شاشة التفاصيل.
     * يحسب: returnStatus + totalRefunded + returnedByUnit
     */
    suspend fun getReturnSummary(transactionId: Long, invoiceItems: List<InvoiceItem>): ReturnSummary

    /** هل الإرجاع الجزئي مفعّل لهذا التاجر؟ */
    suspend fun isPartialReturnEnabled(): Boolean
}