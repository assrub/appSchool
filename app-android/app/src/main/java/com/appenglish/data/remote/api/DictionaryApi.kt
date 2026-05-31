package com.appenglish.data.remote.api

import com.appenglish.data.remote.dto.DictionaryEntryRequest
import com.appenglish.data.remote.dto.DictionaryEntryResponse
import com.appenglish.data.remote.dto.DictionaryListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DictionaryApi {
    @POST("dictionary")
    suspend fun addEntry(@Body request: DictionaryEntryRequest): DictionaryEntryResponse

    @GET("dictionary/{deviceId}")
    suspend fun listEntries(
        @Path("deviceId") deviceId: String,
        @Query("sortBy") sortBy: String = "date",
        @Query("order") order: String = "desc",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): DictionaryListResponse

    @DELETE("dictionary/{deviceId}/{entryId}")
    suspend fun deleteEntry(
        @Path("deviceId") deviceId: String,
        @Path("entryId") entryId: Int
    ): Map<String, String>
}
