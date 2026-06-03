package com.appenglish.data.repository

import com.appenglish.data.local.dao.ContentCacheDao
import com.appenglish.data.local.entity.ContentCacheEntity
import com.appenglish.data.remote.api.ContentApi
import com.appenglish.data.remote.dto.SubjectsResponse
import com.appenglish.data.remote.dto.TopicResponse
import com.appenglish.data.remote.dto.TestResponse
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val api: ContentApi,
    private val cacheDao: ContentCacheDao
) {
    private val gson = Gson()

    suspend fun getSubjects(): Result<SubjectsResponse> = runCatching {
        try {
            api.getSubjects().also { saveCache("subjects", it) }
        } catch (e: Exception) {
            loadCache("subjects", SubjectsResponse::class.java) ?: throw e
        }
    }

    suspend fun getTopic(topicId: String): Result<TopicResponse> = runCatching {
        try {
            api.getTopic(topicId).also { saveCache("topic_$topicId", it) }
        } catch (e: Exception) {
            loadCache("topic_$topicId", TopicResponse::class.java) ?: throw e
        }
    }

    suspend fun getTest(topicId: String, count: Int = 20): Result<TestResponse> = runCatching {
        try {
            api.getTest(topicId, count).also { saveCache("test_$topicId", it) }
        } catch (e: Exception) {
            loadCache("test_$topicId", TestResponse::class.java) ?: throw e
        }
    }

    private suspend fun <T> saveCache(key: String, data: T) {
        try {
            cacheDao.upsert(ContentCacheEntity(key, gson.toJson(data)))
        } catch (_: Exception) {}
    }

    private suspend fun <T> loadCache(key: String, clazz: Class<T>): T? {
        return try {
            val cached = cacheDao.get(key) ?: return null
            gson.fromJson(cached.jsonData, clazz)
        } catch (_: Exception) { 
            // Cache is corrupted or incompatible - delete it
            try { cacheDao.delete(key) } catch (_: Exception) {}
            null
        }
    }
}
