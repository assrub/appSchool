package com.appenglish.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_cache")
data class ContentCacheEntity(
    @PrimaryKey val cacheKey: String,
    val jsonData: String,
    val cachedAt: Long = System.currentTimeMillis()
)
