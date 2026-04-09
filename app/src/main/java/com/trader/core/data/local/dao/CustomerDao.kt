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

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
