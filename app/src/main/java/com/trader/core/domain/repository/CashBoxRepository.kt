package com.trader.core.domain.repository

import com.trader.core.domain.model.CashBox
import kotlinx.coroutines.flow.Flow

interface CashBoxRepository {
    fun getAllBoxes(): Flow<List<CashBox>>

    suspend fun getBoxByPaymentMethod(paymentMethodId: Long): CashBox?

    /** يُحدَّد مرة واحدة فقط لكل صندوق — يصبح الرصيد الابتدائي والحالي معاً */
    suspend fun setInitialBalance(boxId: String, amount: Double)

    /**
     * يعكس أثر عملية على أرصدة الصناديق:
     * إضافة/تعديل/حذف عملية — يُلغى الأثر القديم ثم يُطبَّق الجديد.
     */
    suspend fun applyTransactionEffect(
        oldPaymentMethodId: Long?, oldAmount: Double, oldWasPaid: Boolean,
        newPaymentMethodId: Long?, newAmount: Double, newWasPaid: Boolean
    )

    /** إعادة رفع أي صندوق ما زال PENDING */
    suspend fun syncPendingBoxes()
}
