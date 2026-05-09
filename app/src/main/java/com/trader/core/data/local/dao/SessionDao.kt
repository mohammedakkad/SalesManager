package com.trader.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.trader.core.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY loginDate DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY lastActive DESC LIMIT 1")
    fun observeLatestSession(): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Transaction
    suspend fun upsertAtomic(session: SessionEntity) {
        val inserted = insert(session)
        if (inserted == -1L) {
            update(session)
        }
    }

    @Query("UPDATE sessions SET lastActive = :lastActive WHERE id = :sessionId")
    suspend fun updateLastActive(sessionId: String, lastActive: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
