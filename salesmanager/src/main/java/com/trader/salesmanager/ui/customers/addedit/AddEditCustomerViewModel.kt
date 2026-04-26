package com.trader.salesmanager.ui.customers.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

data class AddEditCustomerUiState(
    val name: String = "",
    val phone: String = "",
    val phoneConflict: String? = null,
    val phoneChecking: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
) {
    // ✅ الاسم: 2 حرف على الأقل، الهاتف: فارغ أو 10 أرقام بالضبط
    val nameError: String? get() = when {
        name.isEmpty() -> null // لا نُظهر خطأ قبل أي كتابة
        name.trim().length < 2 -> "الاسم يجب أن يكون حرفين على الأقل"
        else -> null
    }
    val phoneError: String? get() = when {
        phoneConflict != null -> phoneConflict
        phone.isEmpty() -> null
        phone.length != 10 -> "رقم الهاتف يجب أن يكون 10 أرقام"
        !phone.all {
            it.isDigit()
        } -> "رقم الهاتف يجب أن يحتوي أرقام فقط"
        else -> null
    }
    val canSave get() = name.trim().length >= 2
    && phoneConflict == null
    && !phoneChecking
    && (phone.isEmpty() || phone.length == 10)
}

class AddEditCustomerViewModel(private val repo: CustomerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditCustomerUiState())
    val uiState: StateFlow<AddEditCustomerUiState> = _uiState.asStateFlow()
    private var editingId: Long? = null
    private var phoneCheckJob: Job? = null

    fun loadCustomer(id: Long?) {
        if (id == null) return
        editingId = id
        viewModelScope.launch {
            val c = repo.getCustomerById(id) ?: return@launch
            _uiState.update {
                it.copy(name = c.name, phone = c.phone, isEditMode = true)
            }
        }
    }

    fun updateName(v: String) = _uiState.update {
        it.copy(name = v, error = null)
    }

    fun updatePhone(v: String) {
        // ✅ فلترة: أرقام فقط، بحد أقصى 10
        val digits = v.filter {
            it.isDigit()
        }.take(10)
        _uiState.update {
            it.copy(phone = digits, phoneConflict = null)
        }
        // تحقق من التعارض فقط إذا اكتمل الرقم
        if (digits.length == 10) checkPhone(digits)
    }

    private fun checkPhone(phone: String) {
        phoneCheckJob?.cancel()
        if (phone.isBlank()) {
            _uiState.update {
                it.copy(phoneConflict = null, phoneChecking = false)
            }
            return
        }
        phoneCheckJob = viewModelScope.launch {
            _uiState.update {
                it.copy(phoneChecking = true)
            }
            delay(350)
            val conflictName = repo.getPhoneConflict(phone, editingId ?: -999L)
            _uiState.update {
                it.copy(
                    phoneChecking = false,
                    phoneConflict = conflictName?.let {
                        n -> "مستخدم للعميل: $n"
                    }
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.length < 2) {
            _uiState.update {
                it.copy(error = "الاسم يجب أن يكون حرفين على الأقل")
            }
            return
        }
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val phone = state.phone.trim()
            if (phone.isNotBlank()) {
                val conflict = repo.getPhoneConflict(phone, editingId ?: -999L)
                if (conflict != null) {
                    _uiState.update {
                        it.copy(isLoading = false, phoneConflict = "مستخدم للعميل: $conflict")
                    }
                    return@launch
                }
            }
            val id = editingId
            if (id == null) repo.insertCustomer(Customer(name = name, phone = phone))
            else repo.updateCustomer(Customer(id = id, name = name, phone = phone))
            _uiState.update {
                it.copy(isLoading = false, isSaved = true)
            }
        }
    }
}