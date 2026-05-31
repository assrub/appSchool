package com.appenglish.data.remote.dto

data class DictionaryEntryRequest(
    val deviceId: String,
    val word: String,
    val translation: String,
    val sourceLang: String = "en",
    val targetLang: String = "es"
)

data class DictionaryEntryResponse(
    val id: Int,
    val word: String,
    val translation: String,
    val timesLookedUp: Int,
    val createdAt: String
)

data class DictionaryListResponse(
    val entries: List<DictionaryEntryResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int
)
