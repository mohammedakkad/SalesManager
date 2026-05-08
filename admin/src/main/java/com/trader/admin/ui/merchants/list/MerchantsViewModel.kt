package com.trader.admin.ui.merchants.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 1. تحسين الـ UI State ليكون شاملاً وواضحاً
data class MerchantsUiState(
    val merchants: List<Merchant> = emptyList(),
    val isLoading: Boolean = false,
    val activeFilter: MerchantFilter = MerchantFilter.ALL,
    val errorMessage: String? = null
)

enum class MerchantFilter { ALL, ACTIVE, EXPIRED, DISABLED, FREE, PREMIUM,LINKED, UNLINKED }

@OptIn(FlowPreview::class)
class MerchantsViewModel(private val repo: MerchantAdminRepository) : ViewModel() {

    private val _filter = MutableStateFlow(MerchantFilter.ALL)
    private val _search = MutableStateFlow("")
    val search = _search.asStateFlow()
    private val _isLoading = MutableStateFlow(true)

    // 2. استخدام التجميع الذكي (Flow Combination) مع تحسين الأداء
    val uiState: StateFlow<MerchantsUiState> = combine(
        repo.getAllMerchants().onEach { _isLoading.value = false },
        _filter,
        _search.debounce(300) // تحسين: لا تبحث إلا بعد توقف المستخدم عن الكتابة بـ 300ms
    ) { list, filter, query ->
        val filteredList = filterMerchants(list, filter, query)
        MerchantsUiState(
            merchants = filteredList,
            activeFilter = filter,
            isLoading = _isLoading.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MerchantsUiState(isLoading = true)
    )

    // 3. فصل منطق الفلترة (SRP) لسهولة الاختبار والقراءة
    private fun filterMerchants(list: List<Merchant>, filter: MerchantFilter, query: String): List<Merchant> {
        return list.asSequence() // استخدام Sequence لأداء أفضل مع القوائم الكبيرة
            .filter { merchant ->
                when (filter) {
                    MerchantFilter.ALL      -> true
                    MerchantFilter.ACTIVE   -> merchant.status == MerchantStatus.ACTIVE
                    MerchantFilter.EXPIRED  -> merchant.status == MerchantStatus.EXPIRED
                    MerchantFilter.DISABLED -> merchant.status == MerchantStatus.DISABLED
                    MerchantFilter.FREE     -> merchant.tier == MerchantTier.FREE
                    MerchantFilter.PREMIUM  -> merchant.tier == MerchantTier.PREMIUM
                    MerchantFilter.LINKED   -> !merchant.deviceId.isNullOrBlank()
                    MerchantFilter.UNLINKED -> merchant.deviceId.isNullOrBlank()
                }
            }
            .filter { merchant ->
                query.isEmpty() ||
                        merchant.name.contains(query, ignoreCase = true) ||
                        merchant.phone.contains(query) ||
                        merchant.id.takeLast(6).contains(query) // البحث في آخر 6 أرقام من الـ ID (مفيد للبائعين الشبح)
            }
            .toList()
    }

    // 4. الدوال المطلوبة لإدارة البيانات
    fun setFilter(filter: MerchantFilter) {
        _filter.value = filter
    }

    fun setSearch(query: String) {
        _search.value = query
    }

    fun toggleMerchantStatus(id: String, currentStatus: MerchantStatus) {
        viewModelScope.launch {
            val newStatus = if (currentStatus == MerchantStatus.ACTIVE)
                MerchantStatus.DISABLED else MerchantStatus.ACTIVE
            repo.setMerchantStatus(id, newStatus)
        }
    }

    // وظيفة فك ارتباط الجهاز (المطلوبة للتعامل مع تغيير الهواتف)
    fun unlinkDevice(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.unlinkDevice(id)
            _isLoading.value = false
        }
    }

    fun setStatus(id: String, status: MerchantStatus) { viewModelScope.launch { repo.setMerchantStatus(id, status) } }

    fun deleteMerchant(id: String) {
        viewModelScope.launch {
            repo.deleteMerchant(id)
        }
    }
}