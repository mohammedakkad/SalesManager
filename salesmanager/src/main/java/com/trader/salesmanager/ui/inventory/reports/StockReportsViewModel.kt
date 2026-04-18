package com.trader.salesmanager.ui.inventory.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class StockReportItem(
    val unitId: String, // ← مفتاح فريد لـ LazyColumn
    val productName: String,
    val unitLabel: String,
    val currentQty: Double,
    val unitType: UnitType,
    val totalSoldQty: Double, // كمية البيع في الفترة
    val totalSoldValue: Double, // قيمة البيع
    val movementsCount: Int
)

data class CashSummary(
    val totalCash: Double,
    val totalDebt: Double,
    val todayCash: Double,
    val weekCash: Double,
    val monthCash: Double
)

data class StockReportsUiState(
    val stockItems: List<StockReportItem> = emptyList(),
    val lowStockItems: List<ProductWithUnits> = emptyList(),
    val outOfStockItems: List<ProductWithUnits> = emptyList(),
    val cashSummary: CashSummary = CashSummary(0.0, 0.0, 0.0, 0.0, 0.0),
    val totalProductsValue: Double = 0.0, // قيمة المخزن الكلية
    val isLoading: Boolean = true
)

class StockReportsViewModel(
    private val productRepo: ProductRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<StockReportsUiState> = combine(
        productRepo.getAllProducts(),
        buildCashFlow()
    ) {
        products, cashSummary ->
        val stockItems = products.flatMap {
            p ->
            p.units.map {
                unit ->
                StockReportItem(
                    unitId = unit.id,
                    productName = p.product.name,
                    unitLabel = unit.unitLabel,
                    currentQty = unit.quantityInStock,
                    unitType = unit.unitType,
                    totalSoldQty = 0.0,
                    totalSoldValue = 0.0,
                    movementsCount = 0
                )
            }
        }

        // قيمة المخزن = مجموع (كمية × سعر) لكل وحدة
        val totalValue = products.sumOf {
            p ->
            p.units.sumOf {
                unit -> unit.quantityInStock * unit.price
            }
        }

        StockReportsUiState(
            stockItems = stockItems,
            lowStockItems = products.filter {
                it.isLowStock
            },
            outOfStockItems = products.filter {
                it.isOutOfStock
            },
            cashSummary = cashSummary,
            totalProductsValue = totalValue,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockReportsUiState())

    private fun buildCashFlow(): Flow<CashSummary> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // اليوم
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        val todayEnd = cal.timeInMillis

        // الأسبوع
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekStart = cal.timeInMillis

        // الشهر
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val monthStart = cal.timeInMillis

        return transactionRepo.getAllTransactions().map {
            txs ->
            val cashTxs = txs.filter {
                it.paymentType == PaymentType.CASH && it.isPaid
            }
            val debtTxs = txs.filter {
                it.paymentType == PaymentType.DEBT
            }

            CashSummary(
                totalCash = cashTxs.sumOf {
                    it.amount
                },
                totalDebt = debtTxs.filter {
                    !it.isPaid
                }.sumOf {
                    it.amount
                },
                todayCash = cashTxs.filter {
                    it.date in todayStart..todayEnd
                }.sumOf {
                    it.amount
                },
                weekCash = cashTxs.filter {
                    it.date >= weekStart
                }.sumOf {
                    it.amount
                },
                monthCash = cashTxs.filter {
                    it.date >= monthStart
                }.sumOf {
                    it.amount
                }
            )
        }
    }
}