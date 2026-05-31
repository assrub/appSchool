package com.appenglish.data.repository

import com.appenglish.data.local.dao.DictionaryDao
import com.appenglish.data.local.entity.DictionaryEntry
import com.appenglish.data.remote.api.AuthInterceptor
import com.appenglish.data.remote.api.DictionaryApi
import com.appenglish.data.remote.dto.DictionaryEntryRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    private val api: DictionaryApi,
    private val dao: DictionaryDao
) {
    private val userId: String
        get() = AuthInterceptor.userId?.toString() ?: "0"

    fun getDeviceId(): String = userId

    suspend fun addEntry(word: String, translation: String): DictionaryEntry {
        val existing = dao.getEntry(userId, word)
        if (existing != null) {
            dao.incrementLookup(userId, word)
            return existing.copy(timesLookedUp = existing.timesLookedUp + 1)
        }

        val entry = DictionaryEntry(
            deviceId = userId,
            word = word,
            translation = translation
        )
        dao.upsert(entry)

        try {
            api.addEntry(
                DictionaryEntryRequest(
                    deviceId = userId,
                    word = word,
                    translation = translation
                )
            )
        } catch (_: Exception) { }

        return entry
    }

    fun observeAll(): Flow<List<DictionaryEntry>> = dao.observeAll(userId)

    suspend fun getMostLookedUp(): List<DictionaryEntry> = dao.getMostLookedUp(userId)

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
