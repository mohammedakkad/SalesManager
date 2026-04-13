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
    val unitLabel: String = "",          // "حبة" / "كرتون" / "كيلو"
    val price: Double = 0.0,
    val quantityInStock: Double = 0.0,
    val itemsPerCarton: Int? = null,     // فقط لو CARTON
    val lowStockThreshold: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// صنف كامل مع وحداته — للعرض في الـ UI
data class ProductWithUnits(
    val product: Product,
    val units: List<ProductUnit>
) {
    val defaultUnit: ProductUnit? get() = units.firstOrNull { it.isDefault } ?: units.firstOrNull()
    val isLowStock: Boolean get() = units.any { it.quantityInStock > 0 && it.quantityInStock <= it.lowStockThreshold }
    val isOutOfStock: Boolean get() = units.all { it.quantityInStock <= 0 }
}

enum class UnitType { PIECE, CARTON, WEIGHT }

enum class SyncStatus { PENDING, SYNCED, CONFLICT }
