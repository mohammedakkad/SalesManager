package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.PendingMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessageDao {
    @Query("SELECT * FROM pending_messages WHERE merchantId = :merchantId ORDER BY createdAt ASC")
    fun getAll(merchantId: String): Flow<List<PendingMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: PendingMessageEntity)

    @Query("UPDATE pending_messages SET isFailed = :failed WHERE tempId = :tempId")
    suspend fun setFailed(tempId: String, failed: Boolean)

    @Query("DELETE FROM pending_messages WHERE tempId = :tempId")
    suspend fun delete(tempId: String)

    @Query("DELETE FROM pending_messages WHERE merchantId = :merchantId")
    suspend fun deleteAll(merchantId: String)
}
