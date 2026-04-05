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

    init { viewModelScope.launch { _merchant.value = repo.getMerchantById(merchantId) } }

    fun setStatus(status: MerchantStatus) { viewModelScope.launch { repo.setMerchantStatus(merchantId, status); _merchant.value = repo.getMerchantById(merchantId) } }
    fun delete(onDeleted: () -> Unit) { viewModelScope.launch { repo.deleteMerchant(merchantId); onDeleted() } }
}
