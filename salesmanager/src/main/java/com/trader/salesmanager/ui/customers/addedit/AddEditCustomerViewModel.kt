package com.trader.salesmanager.ui.customers.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditCustomerUiState(
    val name: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class AddEditCustomerViewModel(private val repo: CustomerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditCustomerUiState())
    val uiState: StateFlow<AddEditCustomerUiState> = _uiState.asStateFlow()
    private var editingId: Long? = null

    fun loadCustomer(id: Long?) {
        if (id == null) return
        editingId = id
        viewModelScope.launch {
            val c = repo.getCustomerById(id) ?: return@launch
            _uiState.update { it.copy(name = c.name, phone = c.phone, isEditMode = true) }
        }
    }

    fun updateName(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun updatePhone(v: String) = _uiState.update { it.copy(phone = v) }

    fun save() {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) { _uiState.update { it.copy(error = "اسم الزبون مطلوب") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val phone = _uiState.value.phone.trim()
            val id = editingId
            if (id == null) repo.insertCustomer(Customer(name = name, phone = phone))
            else            repo.updateCustomer(Customer(id = id, name = name, phone = phone))
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
