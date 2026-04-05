package com.trader.admin.ui.merchants.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MerchantsUiState(
    val merchants: List<Merchant> = emptyList(),
    val filter: MerchantStatus? = null,
    val search: String = ""
)

class MerchantsViewModel(private val repo: MerchantAdminRepository) : ViewModel() {
    private val _filter = MutableStateFlow<MerchantStatus?>(null)
    private val _search = MutableStateFlow("")

    val uiState: StateFlow<MerchantsUiState> = combine(repo.getAllMerchants(), _filter, _search) { list, filter, search ->
        val filtered = list
            .filter { filter == null || it.status == filter }
            .filter { search.isEmpty() || it.name.contains(search, ignoreCase = true) || it.phone.contains(search) }
        MerchantsUiState(merchants = filtered, filter = filter, search = search)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MerchantsUiState())

    fun setFilter(f: MerchantStatus?) { _filter.value = f }
    fun setSearch(s: String) { _search.value = s }
    fun setStatus(id: String, status: MerchantStatus) { viewModelScope.launch { repo.setMerchantStatus(id, status) } }
    fun delete(id: String) { viewModelScope.launch { repo.deleteMerchant(id) } }
}
