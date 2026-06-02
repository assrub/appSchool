package com.appenglish.data.remote.api

import com.appenglish.data.remote.dto.AnswerBatchRequest
import com.appenglish.data.remote.dto.ProgressSyncRequest
import com.appenglish.data.remote.dto.ProgressSyncResponse
import com.appenglish.data.remote.dto.ProgressResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProgressApi {
    @POST("progress/sync")
    suspend fun syncProgress(@Body request: ProgressSyncRequest): ProgressSyncResponse

    @GET("progress/{deviceId}")
    suspend fun getProgress(@Path("deviceId") deviceId: String): ProgressResponse

    @POST("progress/answer")
    suspend fun recordAnswers(@Body request: AnswerBatchRequest): Map<String, Any>
}
