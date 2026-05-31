package com.appenglish.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
