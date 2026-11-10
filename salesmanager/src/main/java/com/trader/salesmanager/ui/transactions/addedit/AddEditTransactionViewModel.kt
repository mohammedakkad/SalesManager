package com.trader.salesmanager.ui.transactions.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.model.UnitType
import com.trader.core.domain.model.WALK_IN_CUSTOMER
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.salesmanager.ui.inventory.invoice.InvoiceLineItem
import com.trader.salesmanager.ui.inventory.invoice.SaleWeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val userEditedLines: Boolean = false,
    val baseAmount: Double = 0.0,
    // ✅ مشكلة 3: نحفظ ID طريقة الدفع هنا حتى يتم ربطها بعد تحميل القائمة
    val pendingPaymentMethodId: Long? = null,
    val isRestoringLines: Boolean = false
)

class AddEditTransactionViewModel(
    private val transactionRepo: TransactionRepository,
    private val customerRepo: CustomerRepository,
    private val productRepo: ProductRepository,
    private val stockRepo: StockRepository,
    private val invoiceRepo: InvoiceItemRepository,
    private val activationRepo: com.trader.core.domain.repository.ActivationRepository,
    private val saleCommands: com.trader.salesmanager.sales.SaleCommands,
    private val savedState: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private var commandMerchant: String? = savedState.get<String>("saleCommandMerchant")
    private val operationId: String = savedState.get<String>("saleOperationId") ?: UUID.randomUUID().toString().also { savedState["saleOperationId"] = it }
    private val commandDate: Long = savedState.get<Long>("saleCommandDate") ?: System.currentTimeMillis().also { savedState["saleCommandDate"] = it }
    private var loadedSale: Transaction? = null
    private var loadedItems: List<InvoiceItem> = emptyList()
    private var editingId: Long? = null
    private var isSavingInProgress = false
    private var invoiceDraftRevision = 0L
    private var invoiceDraftJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            customerRepo.getAllCustomers().collect { customers ->
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
            repo.getAllPaymentMethods().collect { methods ->
                _uiState.update { state ->
                    // الأولوية 1: الـ ID القادم من العملية المحملة
                    // الأولوية 2: الطريقة المختارة حالياً
                    // الأولوية 3: أول عنصر في القائمة
                    val resolved = methods.find { it.id == state.pendingPaymentMethodId }
                        ?: methods.find { it.id == state.selectedPaymentMethod?.id }
                        ?: methods.firstOrNull()

                    state.copy(
                        paymentMethods = methods,
                        selectedPaymentMethod = resolved,
                        pendingPaymentMethodId = if (resolved?.id == state.pendingPaymentMethodId) null else state.pendingPaymentMethodId
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
            if (loadedSale?.id != t.id) loadedSale = t
            val customer = if (t.customerId == WALK_IN_CUSTOMER.id) WALK_IN_CUSTOMER
            else customerRepo.getCustomerById(t.customerId)

            _uiState.update { state ->
                val matchingMethod = state.paymentMethods.find { it.id == t.paymentMethodId }

                state.copy(
                    selectedCustomer = customer,
                    amount = if (state.userEditedLines) state.amount else t.amount.toString(),
                    isPaid = t.isPaid,
                    paymentType = t.paymentType,
                    note = t.note,
                    hasItems = if (state.userEditedLines) state.hasItems else t.hasItems,
                    isEditMode = true,
                    // ✅ مشكلة 3: نحفظ ID طريقة الدفع لتُطبَّق عند/بعد تحميل القائمة
                    pendingPaymentMethodId = if (matchingMethod == null) t.paymentMethodId else null,
                    selectedPaymentMethod = matchingMethod ?: state.selectedPaymentMethod

                )
            }

            val items = invoiceRepo.getItemsForTransactionOnce(transactionId)
            loadedItems = items
            val lines = items.mapNotNull { item ->
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

            // ✅ الإصلاح: baseAmount = t.amount - مجموع الأصناف
            // يعمل في كل الحالات:
            //   - عملية بدون أصناف:    t.amount=10, itemsTotal=0  → baseAmount=10
            //   - تعديل أول (أضيف 15):  t.amount=25, itemsTotal=15 → baseAmount=10
            //   - تعديل ثاني (أضيف 30): t.amount=25, itemsTotal=15 → baseAmount=10
            // لا يعتمد على hasItems أبداً
            val itemsTotal = lines.sumOf {
                it.totalPrice
            }
            val baseAmount = (t.originalAmount - itemsTotal).coerceAtLeast(0.0)

            _uiState.update { state ->
                if (state.userEditedLines) return@update state
                state.copy(
                    pendingLines = lines,
                    hasItems = lines.isNotEmpty(),
                    amount = String.format(
                        Locale.US, "%.2f",
                        if (lines.isNotEmpty()) itemsTotal + baseAmount else t.originalAmount
                    ),
                    baseAmount = baseAmount
                )
            }

            // إذا المستخدم لم يعدّل بعد، نُحدِّث baseAmount فقط حتى تستخدمه applyInvoiceLinesFromJson
            if (_uiState.value.userEditedLines) {
                _uiState.update { state ->
                    state.copy(
                        baseAmount = baseAmount,
                        amount = String.format(Locale.US, "%.2f",
                            state.pendingLines.sumOf { it.totalPrice } + baseAmount)
                    )
                }
            }
        }
    }

    fun applyInvoiceLinesFromJson(json: String) {
        val revision = ++invoiceDraftRevision
        invoiceDraftJob?.cancel()
        _uiState.update { it.copy(isRestoringLines = true) }
        invoiceDraftJob = viewModelScope.launch {
            try {
                val arr = org.json.JSONArray(json)
                // The item editor cannot confirm an empty selection. Preserve the current
                // invoice when a missing/obsolete navigation draft cannot be restored.
                require(arr.length() > 0) { "Empty invoice draft" }
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
                    val product = productRepo.getProductById(productId) ?: error("Draft product is unavailable")
                    val unit = product.units.firstOrNull { it.id == unitId } ?: error("Draft unit is unavailable")
                    val line = InvoiceLineItem(
                        product = product,
                        selectedUnit = unit,
                        displayQty = displayQty,
                        displayWeightUnit = weightUnit,
                        customPrice = if (price != unit.price) price else null
                    )
                    require(price.isFinite() && price >= 0 && displayQty.isFinite() && displayQty > 0 &&
                        line.quantity.isFinite() && line.quantity > 0 && line.totalPrice.isFinite())
                    rebuilt += line
                }
                if (revision != invoiceDraftRevision) return@launch
                _uiState.update { state ->
                    if (revision != invoiceDraftRevision) return@update state
                    val total = rebuilt.sumOf { it.totalPrice } + state.baseAmount
                    require(total.isFinite())
                    state.copy(
                        pendingLines = rebuilt,
                        hasItems = rebuilt.isNotEmpty(),
                        userEditedLines = true,
                        amount = String.format(Locale.US, "%.2f", total),
                        error = null
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (revision == invoiceDraftRevision) {
                    _uiState.update { it.copy(error = "تعذر استعادة أصناف الفاتورة، أعد اختيار الأصناف") }
                }
            } finally {
                if (revision == invoiceDraftRevision) _uiState.update { it.copy(isRestoringLines = false) }
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
        if (isSavingInProgress || _uiState.value.isLoading || _uiState.value.isSaved || _uiState.value.isRestoringLines) return
        val state = _uiState.value
        val customer = state.selectedCustomer ?: WALK_IN_CUSTOMER

        val amount = if (state.pendingLines.isNotEmpty()) {
            state.pendingLines.sumOf {
                it.totalPrice
            } + state.baseAmount
        } else {
            state.amount.toLatinDigits().toDoubleOrNull()
        }

        if (amount == null || amount <= 0) {
            _uiState.update {
                it.copy(error = "أدخل مبلغ صحيح")
            }
            return
        }

        isSavingInProgress = true
        viewModelScope.launch {
            try {
                val merchantId = activationRepo.getMerchantCode()
                require(merchantId.isNotBlank()) { "Merchant is not activated" }
                if (commandMerchant == null) { commandMerchant = merchantId; savedState["saleCommandMerchant"] = merchantId }
                check(commandMerchant == merchantId) { "Merchant changed; reopen the sale" }
                _uiState.update {
                    it.copy(isLoading = true, error = null)
                }

                val transaction = Transaction(
                    id = editingId ?: 0,
                    date = loadedSale?.date ?: commandDate,
                    customerId = customer.id,
                    amount = amount, // ✅ المبلغ الصحيح دائماً
                    isPaid = state.isPaid,
                    paymentType = state.paymentType,
                    paymentMethodId = state.selectedPaymentMethod?.id,
                    note = state.note,
                    paidAt = if (state.isPaid) loadedSale?.paidAt ?: commandDate else null,
                    dueDate = loadedSale?.dueDate,
                    reminderEnabled = loadedSale?.reminderEnabled ?: true,
                    hasItems = state.pendingLines.isNotEmpty()
                )

                if (editingId == null) {
                    val savedId = saleCommands.create(operationId, merchantId, transaction,
                        buildItems(0, state.pendingLines))
                    // Dispatch is driven by durable outbox observation after commit.
                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedTransactionId = savedId)
                    }
                } else {
                    val txId = editingId!!
                    val expected = loadedSale ?: error("Sale has not loaded")
                    saleCommands.edit(operationId, merchantId, expected, transaction,
                        if (state.userEditedLines) buildItems(txId, state.pendingLines, loadedItems) else null)
                    // Dispatch is driven by durable outbox observation after commit.

                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedTransactionId = txId)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
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
        lines.forEach { line ->
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

    private fun buildItems(transactionId: Long, lines: List<InvoiceLineItem>, oldItems: List<InvoiceItem> = emptyList()): List<InvoiceItem> {
        // ✅ خريطة productId:unitId → UUID القديم
        // إذا وجد نفس الصنف + الوحدة في العملية القديمة → نُعيد استخدام UUID
        // هذا يجعل Firebase يرى UPDATE بدل DELETE+INSERT → لا تكرار
        val oldIds = oldItems.groupBy { "${it.productId}:${it.unitId}" }
            .mapValues { (_, items) -> java.util.ArrayDeque(items.map { it.id }) }

        return lines.mapIndexed { index, line ->
            val key = "${line.product.product.id}:${line.selectedUnit.id}"
            val itemId = oldIds[key]?.pollFirst() ?: UUID.nameUUIDFromBytes("$operationId:$index:$key".toByteArray()).toString()

            val unitLabelDisplay = if (
                line.selectedUnit.unitType == UnitType.WEIGHT &&
                line.displayWeightUnit != SaleWeightUnit.KG
            ) "${line.selectedUnit.unitLabel} (${line.displayWeightUnit.labelAr})"
            else line.selectedUnit.unitLabel

            InvoiceItem(
                id = itemId,
                transactionId = transactionId,
                productId = line.product.product.id,
                productName = line.product.product.name,
                unitId = line.selectedUnit.id,
                unitLabel = unitLabelDisplay,
                quantity = line.quantity,
                pricePerUnit = line.effectivePrice,
                totalPrice = line.totalPrice,
                merchantId = commandMerchant.orEmpty()
            )
        }

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