package com.appenglish.data.remote.api

import com.appenglish.data.remote.dto.SubjectsResponse
import com.appenglish.data.remote.dto.TopicResponse
import com.appenglish.data.remote.dto.TestResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ContentApi {

    @GET("content/subjects")
    suspend fun getSubjects(): SubjectsResponse

    @GET("content/topics/{topicId}")
    suspend fun getTopic(@Path("topicId") topicId: String): TopicResponse

    @GET("content/topics/{topicId}/test")
    suspend fun getTest(
        @Path("topicId") topicId: String,
        @Query("count") count: Int = 20
    ): TestResponse
}
