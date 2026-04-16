package com.trader.core.data.local.entity

import androidx.room.*
import com.trader.core.domain.model.*

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val barcode: String?,
    val name: String,
    val category: String,
    val imageUri: String?,
    val merchantId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String
) {
    fun toDomain() = Product(
        id = id, barcode = barcode, name = name, category = category,
        imageUri = imageUri, merchantId = merchantId,
        createdAt = createdAt, updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

fun Product.toEntity() = ProductEntity(
    id = id, barcode = barcode, name = name, category = category,
    imageUri = imageUri, merchantId = merchantId,
    createdAt = createdAt, updatedAt = updatedAt,
    syncStatus = syncStatus.name
)

@Entity(
    tableName = "product_units",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId")]
)
data class ProductUnitEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val unitType: String,
    val unitLabel: String,
    val price: Double,
    val quantityInStock: Double,
    val itemsPerCarton: Int?,
    val lowStockThreshold: Double,
    val isDefault: Boolean,
    val weightUnit: String = WeightUnit.KG.name,  // ← v2.1
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String
) {
    fun toDomain() = ProductUnit(
        id = id, productId = productId,
        unitType = UnitType.valueOf(unitType),
        unitLabel = unitLabel, price = price,
        quantityInStock = quantityInStock,
        itemsPerCarton = itemsPerCarton,
        lowStockThreshold = lowStockThreshold,
        isDefault = isDefault,
        weightUnit = runCatching { WeightUnit.valueOf(weightUnit) }.getOrDefault(WeightUnit.KG),
        createdAt = createdAt, updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

fun ProductUnit.toEntity() = ProductUnitEntity(
    id = id, productId = productId,
    unitType = unitType.name, unitLabel = unitLabel,
    price = price, quantityInStock = quantityInStock,
    itemsPerCarton = itemsPerCarton,
    lowStockThreshold = lowStockThreshold,
    isDefault = isDefault,
    weightUnit = weightUnit.name,
    createdAt = createdAt, updatedAt = updatedAt,
    syncStatus = syncStatus.name
)
