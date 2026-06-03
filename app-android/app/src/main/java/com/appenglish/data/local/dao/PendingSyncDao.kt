package com.appenglish.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appenglish.data.local.entity.PendingSyncEntity

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sync: PendingSyncEntity)

    @Query("SELECT * FROM pending_syncs ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingSyncEntity>

    @Query("DELETE FROM pending_syncs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_syncs SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM pending_syncs WHERE retryCount >= maxRetries")
    suspend fun deleteFailed()

    @Query("SELECT COUNT(*) FROM pending_syncs")
    suspend fun count(): Int

    @Query("DELETE FROM pending_syncs WHERE syncType = 'progress' AND topicId = :topicId AND unitId = :unitId")
    suspend fun deleteByUnit(topicId: String, unitId: String)

    @Query("DELETE FROM pending_syncs WHERE syncType = 'progress' AND topicId = :topicId")
    suspend fun deleteByTopic(topicId: String)

    @Query("DELETE FROM pending_syncs WHERE syncType = 'progress'")
    suspend fun deleteAllProgressSyncs()

    @Query("DELETE FROM pending_syncs")
    suspend fun deleteAll()
}
