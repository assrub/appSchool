package com.appenglish.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_progress",
    indices = [Index(value = ["deviceId", "topicId", "unitId", "blockIndex"], unique = true)]
)
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
    val completedItems: Int = 0,
    val completedAt: Long? = null,
    val wrongItemIndices: String = ""
)