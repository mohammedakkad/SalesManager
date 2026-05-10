package com.trader.core.domain.repository

import com.trader.core.domain.model.Product
import com.trader.core.domain.model.ProductUnit
import com.trader.core.domain.model.ProductWithUnits
import kotlinx.coroutines.flow.Flow
import kotlin.collections.emptySet


interface ProductRepository {
    // ── استعلامات ────────────────────────────────────────────────
    fun getAllProducts(): Flow<List<ProductWithUnits>>
    fun searchProducts(query: String): Flow<List<ProductWithUnits>>
    fun getLowStockProducts(): Flow<List<ProductWithUnits>>
    fun getOutOfStockProducts(): Flow<List<ProductWithUnits>>
    suspend fun getProductByBarcode(barcode: String): ProductWithUnits?
    suspend fun getProductById(id: String): ProductWithUnits?

    /**
    * يُعيد اسم الصنف الذي يستخدم هذا الباركود، أو null إذا لا يوجد تعارض.
    * @param excludeProductId مُعرَّف الصنف الحالي (عند التعديل) لاستثنائه من الفحص.
    */
    suspend fun getBarcodeConflict(barcode: String, excludeProductId: String? = null): String?

    // ── إضافة وتعديل ─────────────────────────────────────────────
    suspend fun saveProduct(product: Product, units: List<ProductUnit>,
        deletedUnitIds: Set<String> = emptySet()): String
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(productId: String)

    // ── وحدات ────────────────────────────────────────────────────
    suspend fun saveUnit(unit: ProductUnit)
    suspend fun updateUnit(unit: ProductUnit)
    suspend fun deleteUnit(unitId: String)

    // ── مزامنة ───────────────────────────────────────────────────
    suspend fun syncPendingProducts()
}