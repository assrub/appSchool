package com.appenglish.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TtsApi {

    @POST("tts")
    suspend fun generateSpeech(@Body request: TtsRequest): ResponseBody

    @GET("tts/voices")
    suspend fun getVoices(): TtsVoicesResponse
}

data class TtsRequest(
    val text: String,
    val voice: String = "en-US-ChristopherNeural",
    val rate: String = "+0%",
    val pitch: String = "+0Hz"
)

data class TtsVoicesResponse(
    val voices: List<TtsVoice>
)

data class TtsVoice(
    val id: String,
    val gender: String,
    val locale: String
)
