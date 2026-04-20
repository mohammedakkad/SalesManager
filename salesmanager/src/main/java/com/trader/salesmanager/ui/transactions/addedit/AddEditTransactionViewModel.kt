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
    val hasItems: Boolean = false
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

            // ✅ جلب الأصناف الموجودة وتحويلها لـ InvoiceLineItem
            val existingLines = if (t.hasItems) {
                invoiceRepo.getItemsForTransactionOnce(transactionId).mapNotNull {
                    item ->
                    val productWithUnits = productRepo.getProductById(item.productId) ?: return@mapNotNull null
                    val unit = productWithUnits.units.firstOrNull {
                        it.id == item.unitId
                    } ?: return@mapNotNull null
                    InvoiceLineItem(
                        product = productWithUnits,
                        selectedUnit = unit,
                        displayQty = item.quantity,
                        displayWeightUnit = SaleWeightUnit.KG,
                        customPrice = if (item.pricePerUnit != unit.price) item.pricePerUnit else null
                    )
                }
            } else emptyList()

            _uiState.update {
                state ->
                state.copy(
                    selectedCustomer = customer,
                    amount = t.amount.toString(),
                    isPaid = t.isPaid,
                    paymentType = t.paymentType,
                    note = t.note,
                    hasItems = t.hasItems,
                    pendingLines = existingLines, // ✅ أضف
                    isEditMode = true
                )
            }
        }
    }

    // ── مسار 1: الأصناف تأتي مباشرة من InvoiceItemsScreen (Lambda) ──
    fun applyInvoiceLines(lines: List<InvoiceLineItem>, total: Double) {
        _uiState.update {
            it.copy(
                pendingLines = lines,
                hasItems = lines.isNotEmpty(),
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

                    // ✅ قراءة displayQty (وليس quantity)
                    val displayQty = obj.getDouble("displayQty")

                    // استعادة الوحدة التجارية — افتراضي KG إذا غابت
                    val weightUnit = runCatching {
                        SaleWeightUnit.valueOf(obj.optString("displayWeightUnit", "KG"))
                    }.getOrDefault(SaleWeightUnit.KG)

                    // جلب المنتج من DB المحلية
                    val productWithUnits = productRepo.getProductById(productId) ?: continue
                    val unit = productWithUnits.units.firstOrNull {
                        it.id == unitId
                    } ?: continue

                    rebuilt += InvoiceLineItem(
                        product = productWithUnits,
                        selectedUnit = unit,
                        displayQty = displayQty, // ✅ الكمية بوحدة البائع
                        displayWeightUnit = weightUnit, // ✅ وحدة البائع
                        customPrice = if (price != unit.price) price else null
                        // quantity (بالكيلو) = displayQty × weightUnit.toKg — محسوبة تلقائياً
                    )
                }

                _uiState.update {
                    it.copy(
                        pendingLines = rebuilt,
                        hasItems = rebuilt.isNotEmpty(),
                        amount = if (rebuilt.isNotEmpty())
                            String.format(Locale.US, "%.2f", total) else it.amount
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
        // 1. منع النقر المزدوج (برمجياً ومن خلال حالة الـ Loading)
        if (isSavingInProgress || _uiState.value.isLoading) return

        val state = _uiState.value
        val customer = state.selectedCustomer ?: WALK_IN_CUSTOMER
        val amount = state.amount.toLatinDigits().toDoubleOrNull()

        // التحقق من صحة البيانات
        if (amount == null || amount <= 0) {
            _uiState.update {
                it.copy(error = "أدخل مبلغ صحيح")
            }
            return
        }

        viewModelScope.launch {
            try {
                // 2. إغلاق البوابة فوراً
                isSavingInProgress = true
                _uiState.update {
                    it.copy(isLoading = true, error = null)
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
                    // ──── حالة الإضافة الجديدة ────────────────────────────────
                    val savedId = transactionRepo.insertTransaction(transaction)
                    if (state.pendingLines.isNotEmpty()) {
                        saveItemsAndDeductStock(savedId, state.pendingLines)
                    }
                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedTransactionId = savedId)
                    }
                } else {
                    // ──── حالة التعديل (هذا الجزء الذي كان ينقصنا) ──────────────
                    transactionRepo.updateTransaction(transaction)
                    val txId = editingId!!

                    if (state.pendingLines.isNotEmpty()) {
                        // 1. إرجاع المخزون للأصناف القديمة قبل حذفها
                        val oldItems = invoiceRepo.getItemsForTransactionOnce(txId)
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
                        // 2. حذف الأصناف القديمة وحفظ الجديدة وخصم مخزنها
                        invoiceRepo.deleteItemsForTransaction(txId)
                        saveItemsAndDeductStock(txId, state.pendingLines)
                    }

                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedTransactionId = txId)
                    }
                }
            } catch (e: Exception) {
                // 3. في حال الفشل، نفتح البوابة مرة أخرى للمحاولة
                isSavingInProgress = false
                _uiState.update {
                    it.copy(isLoading = false, error = "حدث خطأ: ${e.message}")
                }
            }
        }
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