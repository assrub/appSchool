package com.appenglish.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appenglish.data.local.entity.BlockProgressEntity
import com.appenglish.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE deviceId = :deviceId AND topicId = :topicId AND unitId = :unitId")
    suspend fun getProgress(deviceId: String, topicId: String, unitId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE deviceId = :deviceId AND topicId = :topicId")
    suspend fun getTopicProgress(deviceId: String, topicId: String): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE deviceId = :deviceId")
    suspend fun getAllProgress(deviceId: String): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE deviceId = :deviceId")
    fun observeAllProgress(deviceId: String): Flow<List<ProgressEntity>>

    @Query("UPDATE progress SET completedItems = :completedItems, score = :score, completed = :completed, completedAt = :completedAt WHERE deviceId = :deviceId AND topicId = :topicId AND unitId = :unitId")
    suspend fun updateProgress(
        deviceId: String,
        topicId: String,
        unitId: String,
        completedItems: Int,
        score: Int,
        completed: Boolean,
        completedAt: Long?
    )

    @Query("UPDATE progress SET testScore = :testScore WHERE deviceId = :deviceId AND topicId = :topicId")
    suspend fun updateTestScore(deviceId: String, topicId: String, testScore: Int)

    @Query("DELETE FROM progress WHERE deviceId = :deviceId")
    suspend fun deleteAllProgress(deviceId: String)

    @Query("DELETE FROM block_progress WHERE deviceId = :deviceId")
    suspend fun deleteAllBlockProgress(deviceId: String)

    @Query("DELETE FROM progress WHERE deviceId = :deviceId AND topicId = :topicId AND unitId = :unitId")
    suspend fun deleteUnitProgress(deviceId: String, topicId: String, unitId: String)

    @Query("DELETE FROM block_progress WHERE deviceId = :deviceId AND topicId = :topicId AND unitId = :unitId")
    suspend fun deleteUnitBlockProgress(deviceId: String, topicId: String, unitId: String)

    // ── Block Progress ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockProgress(blockProgress: BlockProgressEntity)

    @Query("SELECT * FROM block_progress WHERE deviceId = :deviceId AND topicId = :topicId AND unitId = :unitId AND blockIndex = :blockIndex")
    suspend fun getBlockProgress(deviceId: String, topicId: String, unitId: String, blockIndex: Int): BlockProgressEntity?

    @Query("SELECT * FROM block_progress WHERE deviceId = :deviceId AND topicId = :topicId AND unitId = :unitId ORDER BY blockIndex")
    suspend fun getAllBlockProgress(deviceId: String, topicId: String, unitId: String): List<BlockProgressEntity>
}
