package com.trader.salesmanager.ui.customers.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.domain.model.Customer
import com.trader.salesmanager.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditCustomerUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class AddEditCustomerViewModel(private val repo: CustomerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditCustomerUiState())
    val uiState: StateFlow<AddEditCustomerUiState> = _uiState.asStateFlow()
    private var editingCustomerId: Long? = null

    fun loadCustomer(customerId: Long?) {
        if (customerId == null) return
        editingCustomerId = customerId
        viewModelScope.launch {
            val customer = repo.getCustomerById(customerId) ?: return@launch
            _uiState.update { it.copy(name = customer.name, isEditMode = true) }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name, error = null) }

    fun save() {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) { _uiState.update { it.copy(error = "اسم الزبون مطلوب") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val id = editingCustomerId
            if (id == null) repo.insertCustomer(Customer(name = name))
            else repo.updateCustomer(Customer(id = id, name = name))
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}