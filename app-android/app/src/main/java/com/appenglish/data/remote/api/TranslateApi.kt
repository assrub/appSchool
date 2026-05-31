package com.appenglish.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

interface TranslateApi {
    @POST("translate")
    suspend fun translate(@Body request: TranslateRequest): TranslateResponse
}

data class TranslateRequest(
    val text: String,
    val sourceLang: String = "en",
    val targetLang: String = "es"
)

data class TranslateResponse(
    val text: String,
    val translation: String,
    val sourceLang: String,
    val targetLang: String,
    val confidence: Double
)
