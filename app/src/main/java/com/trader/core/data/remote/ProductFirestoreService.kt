package com.trader.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.domain.model.*
import kotlinx.coroutines.tasks.await

class ProductFirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private fun merchantRef(merchantId: String) = db.collection("merchants").document(merchantId)

    suspend fun uploadProduct(merchantId: String, product: Product) {
        merchantRef(merchantId).collection("products").document(product.id).set(product.toMap()).await()
    }

    suspend fun uploadUnit(merchantId: String, unit: ProductUnit) {
        merchantRef(merchantId).collection("product_units").document(unit.id).set(unit.toMap()).await()
    }

    suspend fun deleteProduct(merchantId: String, productId: String) {
        merchantRef(merchantId).collection("products").document(productId).delete().await()
    }

    suspend fun deleteUnit(merchantId: String, unitId: String) {
        merchantRef(merchantId).collection("product_units").document(unitId).delete().await()
    }

    suspend fun uploadMovement(merchantId: String, movement: StockMovement) {
        merchantRef(merchantId).collection("stock_movements").document(movement.id).set(movement.toMap()).await()
    }

    suspend fun uploadInvoiceItems(merchantId: String, items: List<InvoiceItem>) {
        if (items.isEmpty()) return
        val batch = db.batch()
        items.forEach { item ->
            batch.set(merchantRef(merchantId).collection("invoice_items").document(item.id), item.toMap())
        }
        batch.commit().await()
    }

    suspend fun getRemoteQuantity(merchantId: String, unitId: String): Double? =
        merchantRef(merchantId).collection("product_units").document(unitId).get().await().getDouble("quantityInStock")

    suspend fun updateRemoteQuantity(merchantId: String, unitId: String, qty: Double) {
        merchantRef(merchantId).collection("product_units").document(unitId)
            .update("quantityInStock", qty, "updatedAt", System.currentTimeMillis()).await()
    }

    private fun Product.toMap() = mapOf(
        "id" to id, "barcode" to barcode, "name" to name, "category" to category,
        "imageUri" to imageUri, "merchantId" to merchantId,
        "createdAt" to createdAt, "updatedAt" to updatedAt, "syncStatus" to SyncStatus.SYNCED.name
    )

    private fun ProductUnit.toMap() = mapOf(
        "id" to id, "productId" to productId, "unitType" to unitType.name,
        "unitLabel" to unitLabel, "price" to price, "quantityInStock" to quantityInStock,
        "itemsPerCarton" to itemsPerCarton, "lowStockThreshold" to lowStockThreshold,
        "isDefault" to isDefault, "createdAt" to createdAt, "updatedAt" to updatedAt,
        "syncStatus" to SyncStatus.SYNCED.name
    )

    private fun StockMovement.toMap() = mapOf(
        "id" to id, "productId" to productId, "productName" to productName,
        "unitId" to unitId, "unitLabel" to unitLabel, "movementType" to movementType.name,
        "quantity" to quantity, "quantityBefore" to quantityBefore, "quantityAfter" to quantityAfter,
        "relatedTransactionId" to relatedTransactionId, "note" to note,
        "merchantId" to merchantId, "createdAt" to createdAt
    )

    private fun InvoiceItem.toMap() = mapOf(
        "id" to id, "transactionId" to transactionId,
        "productId" to productId, "productName" to productName,
        "unitId" to unitId, "unitLabel" to unitLabel,
        "quantity" to quantity, "pricePerUnit" to pricePerUnit,
        "totalPrice" to totalPrice, "merchantId" to merchantId
    )
}
