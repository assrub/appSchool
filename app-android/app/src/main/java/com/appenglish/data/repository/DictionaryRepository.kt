package com.appenglish.data.repository

import com.appenglish.data.local.dao.DictionaryDao
import com.appenglish.data.local.entity.DictionaryEntry
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
    private val deviceId: String = java.util.UUID.randomUUID().toString().take(8)

    fun getDeviceId(): String = deviceId

    suspend fun addEntry(word: String, translation: String): DictionaryEntry {
        val existing = dao.getEntry(deviceId, word)
        if (existing != null) {
            dao.incrementLookup(deviceId, word)
            return existing.copy(timesLookedUp = existing.timesLookedUp + 1)
        }

        val entry = DictionaryEntry(
            deviceId = deviceId,
            word = word,
            translation = translation
        )
        dao.upsert(entry)

        try {
            api.addEntry(
                DictionaryEntryRequest(
                    deviceId = deviceId,
                    word = word,
                    translation = translation
                )
            )
        } catch (_: Exception) { }

        return entry
    }

    fun observeAll(): Flow<List<DictionaryEntry>> = dao.observeAll(deviceId)

    suspend fun getMostLookedUp(): List<DictionaryEntry> = dao.getMostLookedUp(deviceId)

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
