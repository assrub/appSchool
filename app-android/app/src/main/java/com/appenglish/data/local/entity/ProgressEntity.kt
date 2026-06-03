package com.appenglish.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress",
    indices = [Index(value = ["deviceId", "topicId", "unitId"], unique = true)]
)
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,
    val topicId: String,
    val unitId: String,
    val completed: Boolean = false,
    val score: Int = 0,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val testScore: Int? = null,
    val completedAt: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    // New pedagogical metrics
    val accuracy: Float = 0f,       // % correct on first attempt (0-100)
    val mastery: Float = 0f,        // % mastered (0-100)
    val status: String = "not_started", // not_started, in_progress, completed, mastered
    val itemsAttempted: Int = 0,    // distinct items answered
    val itemsMastered: Int = 0,    // items mastered
    val itemsCorrectFirst: Int = 0, // correct on first attempt
    val timeSpentSeconds: Int = 0,  // total time in seconds
    val lastActivityAt: Long? = null
)
