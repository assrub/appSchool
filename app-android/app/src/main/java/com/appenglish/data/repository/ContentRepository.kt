package com.appenglish.data.repository

import com.appenglish.data.remote.api.ContentApi
import com.appenglish.data.remote.dto.SubjectsResponse
import com.appenglish.data.remote.dto.TopicResponse
import com.appenglish.data.remote.dto.TestResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val api: ContentApi
) {
    suspend fun getSubjects(): Result<SubjectsResponse> = runCatching {
        api.getSubjects()
    }

    suspend fun getTopic(topicId: String): Result<TopicResponse> = runCatching {
        api.getTopic(topicId)
    }

    suspend fun getTest(topicId: String, count: Int = 20): Result<TestResponse> = runCatching {
        api.getTest(topicId, count)
    }
}
