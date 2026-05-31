package com.appenglish.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
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
    val startedAt: Long = System.currentTimeMillis()
)
