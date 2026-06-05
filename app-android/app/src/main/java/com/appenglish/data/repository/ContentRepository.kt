package com.appenglish.data.repository

import com.appenglish.data.local.dao.ContentCacheDao
import com.appenglish.data.local.entity.ContentCacheEntity
import com.appenglish.data.remote.api.ContentApi
import com.appenglish.data.remote.dto.SubjectsResponse
import com.appenglish.data.remote.dto.TopicResponse
import com.appenglish.data.remote.dto.TestResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
            loadCache<SubjectsResponse>("subjects") ?: throw e
        }
    }

    suspend fun getTopic(topicId: String): Result<TopicResponse> = runCatching {
        try {
            api.getTopic(topicId).also { saveCache("topic_$topicId", it) }
        } catch (e: Exception) {
            loadCache<TopicResponse>("topic_$topicId") ?: throw e
        }
    }

    suspend fun getTest(topicId: String, count: Int = 20): Result<TestResponse> = runCatching {
        try {
            api.getTest(topicId, count).also { saveCache("test_$topicId", it) }
        } catch (e: Exception) {
            loadCache<TestResponse>("test_$topicId") ?: throw e
        }
    }

    private suspend fun <T> saveCache(key: String, data: T) {
        try {
            cacheDao.upsert(ContentCacheEntity(key, gson.toJson(data)))
        } catch (_: Exception) {}
    }

    private inline suspend fun <reified T> loadCache(key: String): T? {
        return try {
            val cached = cacheDao.get(key) ?: return null
            gson.fromJson<T>(cached.jsonData, object : TypeToken<T>() {}.type)
        } catch (_: Exception) {
            try { cacheDao.delete(key) } catch (_: Exception) {}
            null
        }
    }
}
