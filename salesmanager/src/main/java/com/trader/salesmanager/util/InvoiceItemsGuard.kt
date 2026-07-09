package com.trader.salesmanager.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.Transaction

class InvoiceItemsMismatchException(
    transactionId: Long,
    hasItems: Boolean,
    amount: Double,
    originalAmount: Double,
    itemsTotal: Double,
    itemCount: Int
) : Exception(
    "invoice_items_mismatch txId=$transactionId hasItems=$hasItems amount=$amount " +
        "originalAmount=$originalAmount itemsTotal=$itemsTotal count=$itemCount"
)

object InvoiceItemsGuard {

    private const val AMOUNT_TOLERANCE = 0.01

    fun filter(items: List<InvoiceItem>, transaction: Transaction?): List<InvoiceItem> {
        if (transaction == null || items.isEmpty()) return items

        val itemsTotal = items.sumOf { it.totalPrice }
        val orphanWithoutFlag = !transaction.hasItems
        val exceedsCurrentAmount = itemsTotal > transaction.amount + AMOUNT_TOLERANCE
        val exceedsOriginalAmount = itemsTotal > transaction.originalAmount + AMOUNT_TOLERANCE

        if (!orphanWithoutFlag && !(exceedsCurrentAmount && exceedsOriginalAmount)) {
            return items
        }

        FirebaseCrashlytics.getInstance().log(
            "invoice_items_mismatch txId=${transaction.id} hasItems=${transaction.hasItems} " +
                "amount=${transaction.amount} originalAmount=${transaction.originalAmount} " +
                "itemsTotal=$itemsTotal count=${items.size}"
        )
        FirebaseCrashlytics.getInstance().recordException(
            InvoiceItemsMismatchException(
                transactionId = transaction.id,
                hasItems = transaction.hasItems,
                amount = transaction.amount,
                originalAmount = transaction.originalAmount,
                itemsTotal = itemsTotal,
                itemCount = items.size
            )
        )
        return emptyList()
    }
}
