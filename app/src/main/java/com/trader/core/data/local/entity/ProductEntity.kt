package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trader.core.domain.model.Product
import com.trader.core.domain.model.ProductUnit
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.model.UnitType

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
    val syncStatus: String   // SyncStatus.name
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
    val unitType: String,       // UnitType.name
    val unitLabel: String,
    val price: Double,
    val quantityInStock: Double,
    val itemsPerCarton: Int?,
    val lowStockThreshold: Double,
    val isDefault: Boolean,
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
    createdAt = createdAt, updatedAt = updatedAt,
    syncStatus = syncStatus.name
)
