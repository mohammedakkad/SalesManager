package com.trader.admin.ui.merchants.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MerchantDetailViewModel(
    private val repo: MerchantAdminRepository,
    private val merchantId: String
) : ViewModel() {

    private val _merchant = MutableStateFlow<Merchant?>(null)
    val merchant = _merchant.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch { _merchant.value = repo.getMerchantById(merchantId) }
    }

    fun setStatus(status: MerchantStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.setMerchantStatus(merchantId, status)
            _merchant.value = repo.getMerchantById(merchantId)
            _isLoading.value = false
        }
    }

    fun adjustExpiry(deltaDays: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.adjustExpiry(merchantId, deltaDays)
            _merchant.value = repo.getMerchantById(merchantId)
            _isLoading.value = false
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteMerchant(merchantId)
            onDeleted()
        }
    }
}
