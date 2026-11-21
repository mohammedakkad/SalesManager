package com.trader.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.*

data class DashboardStats(
    val total: Int = 0, val active: Int = 0,
    val expired: Int = 0, val disabled: Int = 0
)

class DashboardViewModel(private val repo: MerchantAdminRepository) : ViewModel() {
    val stats: StateFlow<DashboardStats> = repo.getAllMerchants()
        .map { list ->
            DashboardStats(
                total    = list.size,
                active   = list.count { it.status == MerchantStatus.ACTIVE },
                expired  = list.count { it.status == MerchantStatus.EXPIRED },
                disabled = list.count { it.status == MerchantStatus.DISABLED }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}
