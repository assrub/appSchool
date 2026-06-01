package com.appenglish.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_progress")
data class BlockProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,
    val topicId: String,
    val unitId: String,
    val blockIndex: Int,
    val score: Int = 0,
    val totalItems: Int = 0,
    val completed: Boolean = false,
    val completedAt: Long? = null
)