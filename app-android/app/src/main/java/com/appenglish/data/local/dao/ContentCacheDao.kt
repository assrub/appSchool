package com.appenglish.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appenglish.data.local.entity.ContentCacheEntity

@Dao
interface ContentCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: ContentCacheEntity)

    @Query("SELECT * FROM content_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): ContentCacheEntity?

    @Query("DELETE FROM content_cache WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM content_cache WHERE cachedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
