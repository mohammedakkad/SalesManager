package com.trader.salesmanager.ui.transactions.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.inventory.invoice.InvoiceLineItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class AddEditTransactionUiState(
    val customers: List<Customer> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedCustomer: Customer? = WALK_IN_CUSTOMER,
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
    private val customerRepo: CustomerRepository,
    private val stockRepo: StockRepository,
    private val invoiceRepo: InvoiceItemRepository,
    private val merchantId: String
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
                    )
                }
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
            val customer = if (t.customerId == WALK_IN_CUSTOMER.id) WALK_IN_CUSTOMER
            else customerRepo.getCustomerById(t.customerId)
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
            )
        }
    }

    fun applyInvoiceLinesFromJson(json: String, total: Double) {
        try {
            val arr = org.json.JSONArray(json)
            _uiState.update {
                it.copy(
                    hasItems = arr.length() > 0,
                    amount = if (arr.length() > 0) String.format("%.2f", total) else it.amount
                )
            }
        } catch (_: Exception) {}
    }

    fun selectCustomer(c: Customer) = _uiState.update {
        it.copy(selectedCustomer = c, error = null)
    }
    fun updateAmount(a: String) = _uiState.update {
        it.copy(amount = a.toLatinDigits(), error = null)
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
        val customer = state.selectedCustomer ?: WALK_IN_CUSTOMER
        val amount = state.amount.toLatinDigits().toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _uiState.update {
                it.copy(error = "أدخل مبلغ صحيح")
            }
            return
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

            if (editingId == null) {
                // ────── حالة الإضافة ──────────────────────────────────
                val savedId = transactionRepo.insertTransaction(transaction)

                if (state.pendingLines.isNotEmpty()) {
                    saveItemsAndDeductStock(savedId, state.pendingLines)
                }

                _uiState.update {
                    it.copy(isLoading = false, isSaved = true, savedTransactionId = savedId)
                }

            } else {
                // ────── حالة التعديل ──────────────────────────────────
                transactionRepo.updateTransaction(transaction)
                val txId = editingId!!

                if (state.pendingLines.isNotEmpty()) {
                    // 1. جلب الأصناف القديمة قبل الحذف
                    val oldItems = invoiceRepo.getItemsForTransactionOnce(txId)

                    // 2. إرجاع المخزون للأصناف القديمة
                    oldItems.forEach {
                        old ->
                        stockRepo.returnStock(
                            productId = old.productId,
                            unitId = old.unitId,
                            quantity = old.quantity,
                            transactionId = txId,
                            productName = old.productName,
                            unitLabel = old.unitLabel
                        )
                    }

                    // 3. حذف الأصناف القديمة وحفظ الجديدة مع خصم المخزون
                    invoiceRepo.deleteItemsForTransaction(txId)
                    saveItemsAndDeductStock(txId, state.pendingLines)
                }
                // إذا لم يغيّر المستخدم الأصناف (pendingLines فارغ) → لا تعديل على المخزون

                _uiState.update {
                    it.copy(isLoading = false, isSaved = true, savedTransactionId = txId)
                }
            }
        }
    }

    // ── مساعد مشترك: حفظ الأصناف + خصم المخزون + مزامنة ──────────
    private suspend fun saveItemsAndDeductStock(transactionId: Long, lines: List<InvoiceLineItem>) {
        val items = lines.map {
            line ->
            InvoiceItem(
                id = UUID.randomUUID().toString(),
                transactionId = transactionId,
                productId = line.product.product.id,
                productName = line.product.product.name,
                unitId = line.selectedUnit.id,
                unitLabel = line.selectedUnit.unitLabel,
                quantity = line.quantity,
                pricePerUnit = line.effectivePrice,
                totalPrice = line.totalPrice,
                merchantId = merchantId
            )
        }
        invoiceRepo.saveItems(items)

        lines.forEach {
            line ->
            stockRepo.deductStock(
                productId = line.product.product.id,
                unitId = line.selectedUnit.id,
                quantity = line.quantity,
                transactionId = transactionId,
                productName = line.product.product.name,
                unitLabel = line.selectedUnit.unitLabel
            )
        }

        // مزامنة الكميات مع Firebase
        stockRepo.syncPendingMovements()
    }
}

/** تحويل الأرقام العربية/الفارسية إلى أرقام لاتينية */
private fun String.toLatinDigits(): String = this
.replace('٠','0').replace('١','1').replace('٢','2')
.replace('٣','3').replace('٤','4').replace('٥','5')
.replace('٦','6').replace('٧','7').replace('٨','8')
.replace('٩','9').replace('۰','0').replace('۱','1')
.replace('۲','2').replace('۳','3').replace('۴','4')
.replace('۵','5').replace('۶','6').replace('۷','7')
.replace('۸','8').replace('۹','9')