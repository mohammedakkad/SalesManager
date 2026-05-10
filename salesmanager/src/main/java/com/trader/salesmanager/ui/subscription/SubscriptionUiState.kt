package com.trader.salesmanager.ui.subscription

import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan


data class SubscriptionUiState(
    val plans: List<SubscriptionPlan> = emptyList(),
    val paymentMethods: List<SubscriptionPaymentMethod> = emptyList(),
    val selectedPlan: SubscriptionPlan? = null,
    val selectedPaymentMethod: SubscriptionPaymentMethod? = null,
    val receiptImageBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = selectedPaymentMethod != null && receiptImageBytes != null && !isLoading

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubscriptionUiState) return false
        return plans == other.plans &&
                paymentMethods == other.paymentMethods &&
                selectedPlan == other.selectedPlan &&
                selectedPaymentMethod == other.selectedPaymentMethod &&
                receiptImageBytes.contentEquals(other.receiptImageBytes) &&
                isLoading == other.isLoading &&
                isSuccess == other.isSuccess &&
                error == other.error
    }

    override fun hashCode(): Int {
        var result = plans.hashCode()
        result = 31 * result + paymentMethods.hashCode()
        result = 31 * result + (selectedPlan?.hashCode() ?: 0)
        result = 31 * result + (selectedPaymentMethod?.hashCode() ?: 0)
        result = 31 * result + (receiptImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isSuccess.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}