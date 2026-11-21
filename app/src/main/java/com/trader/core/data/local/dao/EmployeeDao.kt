package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(employee: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<EmployeeEntity>)

    @Query("SELECT * FROM employees WHERE merchantId = :merchantId ORDER BY createdAt ASC")
    fun getAllByMerchant(merchantId: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE merchantId = :merchantId ORDER BY createdAt ASC")
    suspend fun getAllOnce(merchantId: String): List<EmployeeEntity>

    @Query("DELETE FROM employees WHERE merchantId = :merchantId")
    suspend fun deleteAllByMerchant(merchantId: String)

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE merchantId = :merchantId AND pinCode = :pin LIMIT 1")
    suspend fun getByPin(merchantId: String, pin: String): EmployeeEntity?

    @Query("SELECT COUNT(*) FROM employees WHERE merchantId = :merchantId")
    suspend fun countByMerchant(merchantId: String): Int

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM employees WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<EmployeeEntity>

    @Query("UPDATE employees SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT id FROM employees WHERE merchantId = :merchantId")
    suspend fun getAllIds(merchantId: String): List<String>
}
