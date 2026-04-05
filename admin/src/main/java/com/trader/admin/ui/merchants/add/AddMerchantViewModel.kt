package com.trader.admin.ui.merchants.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

data class AddMerchantUiState(
    val name: String = "", val phone: String = "",
    val isPermanent: Boolean = true, val durationDays: String = "30",
    val isLoading: Boolean = false, val isSaved: Boolean = false, val error: String? = null
)

class AddMerchantViewModel(private val repo: MerchantAdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddMerchantUiState())
    val state: StateFlow<AddMerchantUiState> = _state.asStateFlow()

    fun updateName(v: String)      = _state.update { it.copy(name = v, error = null) }
    fun updatePhone(v: String)     = _state.update { it.copy(phone = v, error = null) }
    fun updatePermanent(v: Boolean)= _state.update { it.copy(isPermanent = v) }
    fun updateDuration(v: String)  = _state.update { it.copy(durationDays = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "اسم البائع مطلوب") }; return }
        if (s.phone.isBlank()) { _state.update { it.copy(error = "رقم الهاتف مطلوب") }; return }
        val code = generateCode()
        val expiry = if (!s.isPermanent) {
            val days = s.durationDays.toIntOrNull() ?: 30
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
            Timestamp(cal.time)
        } else null
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repo.addMerchant(Merchant(
                name = s.name.trim(), phone = s.phone.trim(), activationCode = code,
                status = MerchantStatus.ACTIVE, isPermanent = s.isPermanent,
                expiryDate = expiry, createdAt = Timestamp.now()
            ))
            _state.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    private fun generateCode() = (100000..999999).random().toString()
}
