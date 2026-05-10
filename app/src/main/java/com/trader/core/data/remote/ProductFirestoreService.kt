package com.trader.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductFirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private fun merchantRef(merchantId: String) =
    db.collection("merchants").document(merchantId)

    // مسار الوحدات الجديد — subcollection داخل كل منتج
    private fun unitsRef(merchantId: String, productId: String) =
    merchantRef(merchantId)
    .collection("products")
    .document(productId)
    .collection("units")

    // ── رفع ──────────────────────────────────────────────────────

    suspend fun uploadProduct(merchantId: String, product: Product) {
        merchantRef(merchantId).collection("products").document(product.id)
        .set(product.toMap()).await()
    }

    suspend fun uploadUnit(merchantId: String, unit: ProductUnit) {
        // ✅ الموقع الجديد داخل المنتج
        unitsRef(merchantId, unit.productId).document(unit.id)
        .set(unit.toMap()).await()
    }

    suspend fun deleteProduct(merchantId: String, productId: String) {
        // احذف الوحدات أولاً ثم المنتج
        val units = unitsRef(merchantId, productId).get().await()
        if (units.documents.isNotEmpty()) {
            val batch = db.batch()
            units.documents.forEach {
                batch.delete(it.reference)
            }
            batch.commit().await()
        }
        merchantRef(merchantId).collection("products").document(productId).delete().await()
    }

    suspend fun deleteUnit(merchantId: String, unitId: String, productId: String) {
        unitsRef(merchantId, productId).document(unitId).delete().await()
    }

    suspend fun uploadMovement(merchantId: String, movement: StockMovement) {
        merchantRef(merchantId).collection("stock_movements").document(movement.id)
        .set(movement.toMap()).await()
    }

    suspend fun uploadInvoiceItems(merchantId: String, items: List<InvoiceItem>) {
        if (items.isEmpty()) return
        val batch = db.batch()
        items.forEach {
            item ->
            batch.set(
                merchantRef(merchantId).collection("invoice_items").document(item.id),
                item.toMap()
            )
        }
        batch.commit().await()
    }

    // ── سحب كامل عند إعادة التثبيت / التفعيل ────────────────────

    suspend fun fetchAllProducts(merchantId: String): List<Product> {
        return try {
            merchantRef(merchantId).collection("products")
            .get().await()
            .documents.mapNotNull {
                doc ->
                runCatching {
                    val d = doc.data ?: return@mapNotNull null
                    Product(
                        id = doc.id,
                        barcode = d["barcode"] as? String,
                        name = d["name"] as? String ?: return@mapNotNull null,
                        category = d["category"] as? String ?: "",
                        imageUri = d["imageUri"] as? String,
                        merchantId = d["merchantId"] as? String ?: merchantId,
                        createdAt = (d["createdAt"] as? Long) ?: 0L,
                        updatedAt = (d["updatedAt"] as? Long) ?: 0L,
                        syncStatus = SyncStatus.SYNCED
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ✅ جلب وحدات منتج معين من subcollection
    suspend fun fetchUnitsForProduct(merchantId: String, productId: String): List<ProductUnit> {
        return try {
            unitsRef(merchantId, productId).get().await()
            .documents.mapNotNull {
                doc ->
                runCatching {
                    val d = doc.data ?: return@mapNotNull null
                    ProductUnit(
                        id = doc.id,
                        productId = d["productId"] as? String ?: return@mapNotNull null,
                        unitType = runCatching {
                            UnitType.valueOf(d["unitType"] as? String ?: "")
                        }.getOrDefault(UnitType.PIECE),
                        unitLabel = d["unitLabel"] as? String ?: "",
                        price = (d["price"] as? Double) ?: 0.0,
                        quantityInStock = (d["quantityInStock"] as? Double) ?: 0.0,
                        itemsPerCarton = (d["itemsPerCarton"] as? Long)?.toInt(),
                        lowStockThreshold = (d["lowStockThreshold"] as? Double) ?: 0.0,
                        isDefault = d["isDefault"] as? Boolean ?: false,
                        weightUnit = runCatching {
                            WeightUnit.valueOf(d["weightUnit"] as? String ?: "")
                        }.getOrDefault(WeightUnit.KG),
                        createdAt = (d["createdAt"] as? Long) ?: 0L,
                        updatedAt = (d["updatedAt"] as? Long) ?: 0L,
                        syncStatus = SyncStatus.SYNCED
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── مراقبة real-time ──────────────────────────────────────────

    fun observeProducts(merchantId: String): Flow<List<Product>> = callbackFlow {
        val listener = merchantRef(merchantId).collection("products")
        .addSnapshotListener {
            snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull {
                doc ->
                runCatching {
                    val d = doc.data ?: return@mapNotNull null
                    Product(
                        id = doc.id,
                        barcode = d["barcode"] as? String,
                        name = d["name"] as? String ?: return@mapNotNull null,
                        category = d["category"] as? String ?: "",
                        imageUri = d["imageUri"] as? String,
                        merchantId = d["merchantId"] as? String ?: merchantId,
                        createdAt = (d["createdAt"] as? Long) ?: 0L,
                        updatedAt = (d["updatedAt"] as? Long) ?: 0L,
                        syncStatus = SyncStatus.SYNCED
                    )
                }.getOrNull()
            }
            trySend(list)
        }
        awaitClose {
            listener.remove()
        }
    }

    // ── كميات المخزون ─────────────────────────────────────────────

    suspend fun getRemoteQuantity(merchantId: String, unitId: String): Double? =
    merchantRef(merchantId).collection("product_units").document(unitId)
    .get().await().getDouble("quantityInStock")

    suspend fun updateRemoteQuantity(merchantId: String, unitId: String, qty: Double) {
        merchantRef(merchantId).collection("product_units").document(unitId)
        .update("quantityInStock", qty, "updatedAt", System.currentTimeMillis()).await()
    }

    // ── toMap ─────────────────────────────────────────────────────

    private fun Product.toMap() = mapOf(
        "id" to id, "barcode" to barcode, "name" to name, "category" to category,
        "imageUri" to imageUri, "merchantId" to merchantId,
        "createdAt" to createdAt, "updatedAt" to updatedAt,
        "syncStatus" to SyncStatus.SYNCED.name
    )

    private fun ProductUnit.toMap() = mapOf(
        "id" to id, "productId" to productId, "unitType" to unitType.name,
        "unitLabel" to unitLabel, "price" to price,
        "quantityInStock" to quantityInStock, "itemsPerCarton" to itemsPerCarton,
        "lowStockThreshold" to lowStockThreshold, "isDefault" to isDefault,
        "weightUnit" to weightUnit.name, "createdAt" to createdAt,
        "updatedAt" to updatedAt, "syncStatus" to SyncStatus.SYNCED.name
    )

    private fun StockMovement.toMap() = mapOf(
        "id" to id, "productId" to productId, "productName" to productName,
        "unitId" to unitId, "unitLabel" to unitLabel,
        "movementType" to movementType.name, "quantity" to quantity,
        "quantityBefore" to quantityBefore, "quantityAfter" to quantityAfter,
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