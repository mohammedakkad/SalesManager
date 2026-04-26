package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    /** للتحقق من تكرار رقم الهاتف قبل الحفظ */
    @Query("SELECT * FROM customers WHERE phone = :phone AND phone != '' AND id != :excludeId LIMIT 1")
    suspend fun getByPhone(phone: String, excludeId: Long = -999L): CustomerEntity?

    /** لمعرفة عدد العمليات المرتبطة قبل الحذف */
    @Query("SELECT COUNT(*) FROM transactions WHERE customerId = :customerId")
    suspend fun getTransactionCount(customerId: Long): Int


    // IGNORE: don't replace existing customers - REPLACE causes CASCADE delete of their transactions!
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    // Use this for explicit updates (name/phone changes)
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    // Upsert: insert if new, update if exists (safe - no CASCADE trigger)
    @Transaction
    suspend fun upsertCustomer(customer: CustomerEntity) {
        val inserted = insertCustomer(customer)
        if (inserted == -1L) updateCustomer(customer)
    }

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    /** حذف جميع الزبائن ماعدا الزبون الزائر (id=-1) — يُستدعى عند إلغاء التفعيل */
    @Query("DELETE FROM customers WHERE id != -1") suspend fun deleteAll()

    @Query("UPDATE customers SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markCustomerSynced(id: Long)
}