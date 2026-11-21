package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.SyncStatus

/**
 * ⚠️ لا نستخدم ForeignKey حقيقي على payment_methods:
 * PaymentMethodDao يعتمد OnConflictStrategy.REPLACE (حذف + إدراج داخلياً)،
 * وأي CASCADE سيمسح الصندوق عند كل مزامنة لطرق الدفع.
 * الربط يتم عبر unique index + منطق التتالي في CashBoxRepositoryImpl.
 */
@Entity(
    tableName = "cash_boxes",
    indices = [Index(value = ["paymentMethodId"], unique = true)]
)
data class CashBoxEntity(
    @PrimaryKey val id: String,
    val paymentMethodId: Long,
    val paymentMethodName: String,
    val currentBalance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val initialBalanceSetAt: Long? = null,
    val merchantId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING.name
) {
    fun toDomain() = CashBox(
        id = id,
        paymentMethodId = paymentMethodId,
        paymentMethodName = paymentMethodName,
        currentBalance = currentBalance,
        initialBalance = initialBalance,
        initialBalanceSetAt = initialBalanceSetAt,
        merchantId = merchantId,
        updatedAt = updatedAt,
        syncStatus = runCatching {
            SyncStatus.valueOf(syncStatus)
        }.getOrDefault(SyncStatus.PENDING)
    )

    companion object {
        fun fromDomain(box: CashBox) = CashBoxEntity(
            id = box.id,
            paymentMethodId = box.paymentMethodId,
            paymentMethodName = box.paymentMethodName,
            currentBalance = box.currentBalance,
            initialBalance = box.initialBalance,
            initialBalanceSetAt = box.initialBalanceSetAt,
            merchantId = box.merchantId,
            updatedAt = box.updatedAt,
            syncStatus = box.syncStatus.name
        )
    }
}
