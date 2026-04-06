package com.trader.salesmanager.ui.transactions.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditTransactionUiState(
    val customers: List<Customer> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedCustomer: Customer? = null,
    val amount: String = "",
    val isPaid: Boolean = true,
    val selectedPaymentMethod: PaymentMethod? = null,
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class AddEditTransactionViewModel(
    private val transactionRepo: TransactionRepository,
    private val customerRepo: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private var editingId: Long? = null

    init {
        viewModelScope.launch {
            customerRepo.getAllCustomers().collect { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    fun loadPaymentMethods(repo: PaymentMethodRepository) {
        viewModelScope.launch {
            repo.getAllPaymentMethods().collect { methods ->
                _uiState.update { it.copy(paymentMethods = methods, selectedPaymentMethod = it.selectedPaymentMethod ?: methods.firstOrNull()) }
            }
        }
    }

    fun preselect(customerId: Long?) {
        if (customerId == null) return
        viewModelScope.launch {
            val customer = customerRepo.getCustomerById(customerId)
            _uiState.update { it.copy(selectedCustomer = customer) }
        }
    }

    fun loadTransaction(transactionId: Long?) {
        if (transactionId == null) return
        editingId = transactionId
        viewModelScope.launch {
            val t = transactionRepo.getTransactionById(transactionId) ?: return@launch
            val customer = customerRepo.getCustomerById(t.customerId)
            _uiState.update { state ->
                state.copy(
                    selectedCustomer = customer,
                    amount = t.amount.toString(),
                    isPaid = t.isPaid,
                    note = t.note,
                    isEditMode = true
                )
            }
        }
    }

    fun selectCustomer(c: Customer) = _uiState.update { it.copy(selectedCustomer = c, error = null) }
    fun updateAmount(a: String) = _uiState.update { it.copy(amount = a, error = null) }
    fun updateIsPaid(v: Boolean) = _uiState.update { it.copy(isPaid = v) }
    fun selectPaymentMethod(m: PaymentMethod) = _uiState.update { it.copy(selectedPaymentMethod = m) }
    fun updateNote(n: String) = _uiState.update { it.copy(note = n) }

    fun save() {
        val state = _uiState.value
        val customer = state.selectedCustomer
        val amount   = state.amount.toDoubleOrNull()

        if (customer == null) { _uiState.update { it.copy(error = "اختر الزبون") }; return }
        if (amount == null || amount <= 0) { _uiState.update { it.copy(error = "أدخل مبلغ صحيح") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val transaction = Transaction(
                id = editingId ?: 0,
                customerId = customer.id,
                amount = amount,
                isPaid = state.isPaid,
                paymentMethodId = state.selectedPaymentMethod?.id,
                note = state.note,
                paidAt = if (state.isPaid) System.currentTimeMillis() else null
            )
            if (editingId == null) transactionRepo.insertTransaction(transaction)
            else transactionRepo.updateTransaction(transaction)
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}