package com.trader.core.domain.model

data class Product(
    val id: String = "",
    val barcode: String? = null,
    val name: String = "",
    val category: String = "",
    val imageUri: String? = null,
    val merchantId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

data class ProductUnit(
    val id: String = "",
    val productId: String = "",
    val unitType: UnitType = UnitType.PIECE,
    val unitLabel: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val quantityInStock: Double = 0.0,
    val itemsPerCarton: Int? = null,
    val lowStockThreshold: Double = 0.0,
    val isDefault: Boolean = false,
    // ✅ وحدة الوزن — كيلو أو وقية
    val weightUnit: WeightUnit = WeightUnit.KG,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {

    /**
     * هامش الربح لكل وحدة.
     * null إذا لم يُحدَّد سعر الشراء (costPrice == 0).
     */
    val profitMargin: Double?
    get() = if (costPrice > 0 && price > 0) price - costPrice else null

    /**
     * نسبة الربح %.
     * null إذا لم يُحدَّد سعر الشراء.
     */
    val profitPercent: Double?
    get() = if (costPrice > 0 && price > 0) ((price - costPrice) / costPrice) * 100 else null


    /** يحوّل الكمية المخزنة (بالكيلو دائماً) إلى وحدة العرض */
    fun displayQuantity(rawKg: Double): String = when {
        unitType != UnitType.WEIGHT -> rawKg.toInt().toString()
        else -> {
            val displayValue = rawKg / weightUnit.kgFactor
            if (displayValue == displayValue.toLong().toDouble())
                displayValue.toLong().toString()
            else
                String.format("%.1f", displayValue)
        }
    }
}

data class ProductWithUnits(
    val product: Product,
    val units: List<ProductUnit>
) {
    val defaultUnit: ProductUnit? get() = units.firstOrNull {
        it.isDefault
    } ?: units.firstOrNull()
    val isLowStock: Boolean get() = units.any {
        it.quantityInStock > 0 && it.quantityInStock <= it.lowStockThreshold
    }
    val isOutOfStock: Boolean get() = units.isNotEmpty() && units.all {
        it.quantityInStock <= 0
    }
}

enum class UnitType {
    PIECE, CARTON, WEIGHT
}

/** وحدات الوزن — الكميات تُخزّن دائماً بالكيلو ثم تُحوَّل للعرض */
enum class WeightUnit(val label: String, val kgFactor: Double) {
    KG ("كيلو", 1.0),
    OZ ("أوقية", 0.250),
    GRAM("غرام", 0.001) // 1 غرام = 0.001 كيلو
}
enum class SyncStatus {
    PENDING, SYNCED, CONFLICT
}