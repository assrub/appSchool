package com.appenglish.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary",
    indices = [Index(value = ["deviceId", "word"], unique = true)]
)
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,
    val word: String,
    val translation: String,
    val sourceLang: String = "en",
    val targetLang: String = "es",
    val timesLookedUp: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
