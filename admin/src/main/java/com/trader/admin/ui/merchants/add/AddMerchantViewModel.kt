package com.trader.admin.ui.merchants.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class AddMerchantUiState(
    val name: String = "",
    val phone: String = "",
    val isPermanent: Boolean = true,
    val durationDays: String = "30",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val generatedCode: String? = null
)

class AddMerchantViewModel(private val repo: MerchantAdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddMerchantUiState())
    val state: StateFlow<AddMerchantUiState> = _state.asStateFlow()

    private val rtdb = FirebaseDatabase.getInstance().reference

    fun updateName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun updatePhone(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun updatePermanent(v: Boolean) = _state.update { it.copy(isPermanent = v) }
    fun updateDuration(v: String) = _state.update { it.copy(durationDays = v) }

    fun save() {
        val s = _state.value

        // التحقق من المدخلات
        if (s.name.trim().length < 2) {
            _state.update { it.copy(error = "اسم البائع يجب أن يكون حرفين على الأقل") }
            return
        }
        if (s.phone.trim().length != 10) {
            _state.update { it.copy(error = "رقم الهاتف يجب أن يكون 10 أرقام") }
            return
        }
        if (!s.isPermanent) {
            val days = s.durationDays.toIntOrNull()
            if (days == null || days < 1) {
                _state.update { it.copy(error = "أدخل عدد أيام صحيح") }
                return
            }
        }

        val code = generateCode()
        val expiry = if (!s.isPermanent) {
            val days = s.durationDays.toIntOrNull() ?: 30
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
            Timestamp(cal.time)
        } else null

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. أضف البائع في Firestore
                repo.addMerchant(
                    Merchant(
                        name = s.name.trim(),
                        phone = s.phone.trim(),
                        activationCode = code,
                        status = MerchantStatus.ACTIVE,
                        isPermanent = s.isPermanent,
                        expiryDate = expiry,
                        createdAt = Timestamp.now()
                    )
                )

                // 2. أضف رمز التفعيل في Realtime Database
                // هذا ما يتحقق منه تطبيق البائع
                rtdb.child("activation_codes").child(code).setValue(true).await()

                _state.update { it.copy(isLoading = false, isSaved = true, generatedCode = code) }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "فشل الحفظ: ${e.message}") }
            }
        }
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}