package com.trader.salesmanager.ui.transactions.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.inventory.invoice.InvoiceLineItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditTransactionUiState(
    val customers: List<Customer> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedCustomer: Customer? = null,
    val amount: String = "",
    val isPaid: Boolean = true,
    val paymentType: PaymentType = PaymentType.DEBT,
    val selectedPaymentMethod: PaymentMethod? = null,
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val savedTransactionId: Long? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val pendingLines: List<InvoiceLineItem> = emptyList(),
    val hasItems: Boolean = false
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
            customerRepo.getAllCustomers().collect {
                customers ->
                _uiState.update {
                    it.copy(customers = customers)
                }
            }
        }
    }

    fun loadPaymentMethods(repo: PaymentMethodRepository) {
        viewModelScope.launch {
            repo.getAllPaymentMethods().collect {
                methods ->
                _uiState.update {
                    it.copy(
                        paymentMethods = methods,
                        selectedPaymentMethod = it.selectedPaymentMethod ?: methods.firstOrNull()
                    )}
            }
        }
    }

    fun preselect(customerId: Long?) {
        if (customerId == null) return
        viewModelScope.launch {
            val customer = customerRepo.getCustomerById(customerId)
            _uiState.update {
                it.copy(selectedCustomer = customer)
            }
        }
    }

    fun loadTransaction(transactionId: Long?) {
        if (transactionId == null) return
        editingId = transactionId
        viewModelScope.launch {
            val t = transactionRepo.getTransactionById(transactionId) ?: return@launch
            val customer = customerRepo.getCustomerById(t.customerId)
            _uiState.update {
                state ->
                state.copy(
                    selectedCustomer = customer,
                    amount = t.amount.toString(),
                    isPaid = t.isPaid,
                    paymentType = t.paymentType,
                    note = t.note,
                    hasItems = t.hasItems,
                    isEditMode = true
                )
            }
        }
    }

    fun applyInvoiceLines(lines: List<InvoiceLineItem>, total: Double) {
        _uiState.update {
            it.copy(
                pendingLines = lines,
                hasItems = lines.isNotEmpty(),
                amount = if (lines.isNotEmpty()) String.format("%.2f", total) else it.amount
            )}
    }

    fun applyInvoiceLinesFromJson(json: String, total: Double) {
        try {
            val arr = org.json.JSONArray(json)
            _uiState.update {
                it.copy(
                    hasItems = arr.length() > 0,
                    amount = if (arr.length() > 0) String.format("%.2f", total) else it.amount
                )}
        } catch (_: Exception) {}
    }

    fun selectCustomer(c: Customer) = _uiState.update {
        it.copy(selectedCustomer = c, error = null)
    }
    fun updateAmount(a: String) = _uiState.update {
        it.copy(amount = a, error = null)
    }
    fun updateIsPaid(v: Boolean) = _uiState.update {
        it.copy(isPaid = v)
    }
    fun updatePaymentType(t: PaymentType) = _uiState.update {
        it.copy(paymentType = t)
    }
    fun selectPaymentMethod(m: PaymentMethod) = _uiState.update {
        it.copy(selectedPaymentMethod = m)
    }
    fun updateNote(n: String) = _uiState.update {
        it.copy(note = n)
    }

    fun save() {
        val state = _uiState.value
        val customer = state.selectedCustomer
        val amount = state.amount.toDoubleOrNull()
        if (customer == null) {
            _uiState.update {
                it.copy(error = "اختر الزبون")
            }; return
        }
        if (amount == null || amount <= 0) {
            _uiState.update {
                it.copy(error = "أدخل مبلغ صحيح")
            }; return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val transaction = Transaction(
                id = editingId ?: 0,
                customerId = customer.id,
                amount = amount,
                isPaid = state.isPaid,
                paymentType = state.paymentType,
                paymentMethodId = state.selectedPaymentMethod?.id,
                note = state.note,
                paidAt = if (state.isPaid) System.currentTimeMillis() else null,
                hasItems = state.hasItems
            )
            val savedId = if (editingId == null) transactionRepo.insertTransaction(transaction)
            else {
                transactionRepo.updateTransaction(transaction); editingId!!
            }
            _uiState.update {
                it.copy(isLoading = false, isSaved = true, savedTransactionId = savedId)
            }
        }
    }
}