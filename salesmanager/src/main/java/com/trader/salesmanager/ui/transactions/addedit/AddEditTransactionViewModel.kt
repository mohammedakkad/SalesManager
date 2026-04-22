package com.trader.salesmanager.ui.transactions.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.inventory.invoice.InvoiceLineItem
import com.trader.salesmanager.ui.inventory.invoice.SaleWeightUnit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
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
    val hasItems: Boolean = false,
    val userEditedLines: Boolean = false

)

class AddEditTransactionViewModel(
    private val transactionRepo: TransactionRepository,
    private val customerRepo: CustomerRepository,
    private val productRepo: ProductRepository,
    private val stockRepo: StockRepository,
    private val invoiceRepo: InvoiceItemRepository,
    private val merchantId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private var editingId: Long? = null
    private var isSavingInProgress = false

    init {
        viewModelScope.launch {
            customerRepo.getAllCustomers().collect {
                customers ->
                val withGuest = listOf(WALK_IN_CUSTOMER) +
                customers.filter {
                    it.id != WALK_IN_CUSTOMER.id
                }
                _uiState.update {
                    it.copy(customers = withGuest)
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

            // ✅ جلب الأصناف مرة واحدة فقط — بدون Flow مستمر
            val items = invoiceRepo.getItemsForTransactionOnce(transactionId)
            val lines = items.mapNotNull {
                item ->
                val productWithUnits = productRepo.getProductById(item.productId)
                ?: return@mapNotNull null
                val unit = productWithUnits.units.firstOrNull {
                    it.id == item.unitId
                }
                ?: return@mapNotNull null
                InvoiceLineItem(
                    product = productWithUnits,
                    selectedUnit = unit,
                    displayQty = item.quantity,
                    displayWeightUnit = SaleWeightUnit.KG,
                    customPrice = if (item.pricePerUnit != unit.price) item.pricePerUnit else null
                )
            }
            if (lines.isNotEmpty()) {
                val total = lines.sumOf {
                    it.totalPrice
                }
                _uiState.update {
                    it.copy(
                        pendingLines = lines,
                        hasItems = true,
                        // ✅ حدّث المبلغ ليشمل كل الأصناف
                        amount = String.format(Locale.US, "%.2f", total)
                    )
                }
            }
        }
    }


    // ── مسار 1: الأصناف تأتي مباشرة من InvoiceItemsScreen (Lambda) ──
    fun applyInvoiceLines(lines: List<InvoiceLineItem>, total: Double) {
        _uiState.update {
            it.copy(
                pendingLines = lines,
                hasItems = lines.isNotEmpty(),
                userEditedLines = true,
                amount = if (lines.isNotEmpty())
                    String.format(Locale.US, "%.2f", total) else it.amount
            )
        }
    }

    // ── مسار 2: الأصناف تأتي كـ JSON من SavedStateHandle ─────────────
    //
    // ✅ الإصلاح: يقرأ "displayQty" (وليس "quantity") لأن serializeLines
    //    في AppNavigation يحفظ الحقل باسم "displayQty".
    //    كان يقرأ "quantity" → JSONException: No value for quantity.
    //
    // التسلسل الصحيح:
    //   serializeLines يكتب:  "displayQty", "displayWeightUnit", "price"
    //   applyInvoiceLinesFromJson يقرأ: "displayQty", "displayWeightUnit", "price"
    //   quantity (بالكيلو) = displayQty × weightUnit.toKg  (محسوبة تلقائياً)
    fun applyInvoiceLinesFromJson(json: String, total: Double) {
        viewModelScope.launch {
            try {
                val arr = org.json.JSONArray(json)
                if (arr.length() == 0) return@launch

                val rebuilt = mutableListOf<InvoiceLineItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val productId = obj.getString("productId")
                    val unitId = obj.getString("unitId")
                    val price = obj.getDouble("price")
                    val displayQty = obj.getDouble("displayQty")
                    val weightUnit = runCatching {
                        SaleWeightUnit.valueOf(obj.optString("displayWeightUnit", "KG"))
                    }.getOrDefault(SaleWeightUnit.KG)
                    val productWithUnits = productRepo.getProductById(productId) ?: continue
                    val unit = productWithUnits.units.firstOrNull {
                        it.id == unitId
                    } ?: continue
                    rebuilt += InvoiceLineItem(
                        product = productWithUnits,
                        selectedUnit = unit,
                        displayQty = displayQty,
                        displayWeightUnit = weightUnit,
                        customPrice = if (price != unit.price) price else null
                    )
                }

                // ✅ المبلغ يحسب من كل الأصناف — القديمة والجديدة معاً
                val newTotal = rebuilt.sumOf {
                    it.totalPrice
                }
                _uiState.update {
                    it.copy(
                        pendingLines = rebuilt,
                        hasItems = rebuilt.isNotEmpty(),
                        userEditedLines = true,
                        amount = String.format(Locale.US, "%.2f", newTotal)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "فشل استعادة البيانات: ${e.message}")
                }
            }
        }
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
        if (isSavingInProgress || _uiState.value.isLoading) return
        val state = _uiState.value
        val customer = state.selectedCustomer ?: WALK_IN_CUSTOMER

        // ✅ المبلغ يُحسب من الأصناف إذا كانت موجودة — لا من حقل الإدخال
        val amount = if (state.pendingLines.isNotEmpty()) {
            state.pendingLines.sumOf {
                it.totalPrice
            }
        } else {
            state.amount.toLatinDigits().toDoubleOrNull()
        }

        if (amount == null || amount <= 0) {
            _uiState.update {
                it.copy(error = "أدخل مبلغ صحيح")
            }
            return
        }

        viewModelScope.launch {
            try {
                isSavingInProgress = true
                _uiState.update {
                    it.copy(isLoading = true, error = null)
                }

                val transaction = Transaction(
                    id = editingId ?: 0,
                    customerId = customer.id,
                    amount = amount, // ✅ المبلغ الصحيح دائماً
                    isPaid = state.isPaid,
                    paymentType = state.paymentType,
                    paymentMethodId = state.selectedPaymentMethod?.id,
                    note = state.note,
                    paidAt = if (state.isPaid) System.currentTimeMillis() else null,
                    hasItems = state.pendingLines.isNotEmpty()
                )

                if (editingId == null) {
                    val savedId = transactionRepo.insertTransaction(transaction)
                    if (state.pendingLines.isNotEmpty()) {
                        saveItemsAndDeductStock(savedId, state.pendingLines)
                    }
                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedTransactionId = savedId)
                    }
                } else {
                    val txId = editingId!!
                    transactionRepo.updateTransaction(transaction)

                    if (state.userEditedLines) {
                        // ✅ فقط إذا عدّل المستخدم الأصناف فعلاً
                        val oldItems = invoiceRepo.getItemsForTransactionOnce(txId)
                        oldItems.forEach {
                            old ->
                            stockRepo.returnStock(
                                productId = old.productId, unitId = old.unitId,
                                quantity = old.quantity, transactionId = txId,
                                productName = old.productName, unitLabel = old.unitLabel
                            )
                        }
                        invoiceRepo.deleteItemsForTransaction(txId)
                        saveItemsAndDeductStock(txId, state.pendingLines)
                    }

                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedTransactionId = txId)
                    }
                }
            } catch (e: Exception) {
                isSavingInProgress = false
                _uiState.update {
                    it.copy(isLoading = false, error = "حدث خطأ: ${e.message}")
                }
            }
        }
    }

    fun serializePendingLines(): String? {
        val lines = _uiState.value.pendingLines
        if (lines.isEmpty()) return null
        val arr = org.json.JSONArray()
        lines.forEach {
            line ->
            arr.put(org.json.JSONObject().apply {
                put("productId", line.product.product.id)
                put("unitId", line.selectedUnit.id)
                put("displayQty", line.displayQty)
                put("displayWeightUnit", line.displayWeightUnit.name)
                put("price", line.effectivePrice)
            })
        }
        return arr.toString()
    }

    // ── حفظ الأصناف + خصم المخزون + مزامنة ──────────────────────
    private suspend fun saveItemsAndDeductStock(
        transactionId: Long,
        lines: List<InvoiceLineItem>
    ) {
        val items = lines.map {
            line ->
            // نحفظ الوحدة التجارية في unitLabel للعرض في الفاتورة
            val unitLabelDisplay = if (
                line.selectedUnit.unitType == UnitType.WEIGHT &&
                line.displayWeightUnit != SaleWeightUnit.KG
            ) "${line.selectedUnit.unitLabel} (${line.displayWeightUnit.labelAr})"
            else line.selectedUnit.unitLabel

            InvoiceItem(
                id = UUID.randomUUID().toString(),
                transactionId = transactionId,
                productId = line.product.product.id,
                productName = line.product.product.name,
                unitId = line.selectedUnit.id,
                unitLabel = unitLabelDisplay,
                quantity = line.quantity, // ✅ بالكيلو دائماً
                pricePerUnit = line.effectivePrice,
                totalPrice = line.totalPrice,
                merchantId = merchantId
            )
        }

        // local + remote
        invoiceRepo.saveItems(items)

        // خصم المخزون بالكيلو
        lines.forEach {
            line ->
            stockRepo.deductStock(
                productId = line.product.product.id,
                unitId = line.selectedUnit.id,
                quantity = line.quantity, // ✅ displayQty × toKg
                transactionId = transactionId,
                productName = line.product.product.name,
                unitLabel = line.selectedUnit.unitLabel
            )
        }

        // مزامنة مع Firebase
        // stockRepo.syncPendingMovements()
    }
}

private fun String.toLatinDigits(): String = this
.replace('٠', '0').replace('١', '1').replace('٢', '2')
.replace('٣', '3').replace('٤', '4').replace('٥', '5')
.replace('٦', '6').replace('٧', '7').replace('٨', '8')
.replace('٩', '9').replace('۰', '0').replace('۱', '1')
.replace('۲', '2').replace('۳', '3').replace('۴', '4')
.replace('۵', '5').replace('۶', '6').replace('۷', '7')
.replace('۸', '8').replace('۹', '9')