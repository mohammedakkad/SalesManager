package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.ProductEntity
import com.trader.core.data.local.entity.ProductUnitEntity
import kotlinx.coroutines.flow.Flow

data class ProductWithUnitsRelation(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val units: List<ProductUnitEntity>
)

@Dao
interface ProductDao {
    // ── Products ─────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllWithUnitsOnce(): List<ProductWithUnitsRelation>

    @Transaction
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllWithUnits(): Flow<List<ProductWithUnitsRelation>>

    @Transaction
    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchWithUnits(query: String): Flow<List<ProductWithUnitsRelation>>

    @Transaction
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductWithUnitsRelation?

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProductWithUnitsRelation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("SELECT * FROM products WHERE syncStatus = 'PENDING'")
    suspend fun getPendingProducts(): List<ProductEntity>

    @Query("UPDATE products SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markProductSynced(id: String)

    // ── Units ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: ProductUnitEntity)

    @Update
    suspend fun updateUnit(unit: ProductUnitEntity)

    @Query("DELETE FROM product_units WHERE id = :id")
    suspend fun deleteUnit(id: String)

    @Query("SELECT * FROM product_units WHERE productId = :productId")
    suspend fun getUnitsForProduct(productId: String): List<ProductUnitEntity>

    @Query("SELECT * FROM product_units WHERE syncStatus = 'PENDING'")
    suspend fun getPendingUnits(): List<ProductUnitEntity>

    @Query("UPDATE product_units SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markUnitSynced(id: String)


    // ── Stock updates ─────────────────────────────────────────────

    @Query("UPDATE product_units SET quantityInStock = :qty, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :unitId")
    suspend fun updateQuantity(unitId: String, qty: Double, now: Long = System.currentTimeMillis())

    @Query("SELECT quantityInStock FROM product_units WHERE id = :unitId")
    suspend fun getQuantity(unitId: String): Double?

    @Query("DELETE FROM products") suspend fun deleteAllProducts()
}