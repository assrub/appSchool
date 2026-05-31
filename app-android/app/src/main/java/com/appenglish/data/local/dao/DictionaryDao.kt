package com.appenglish.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appenglish.data.local.entity.DictionaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DictionaryEntry)

    @Query("SELECT * FROM dictionary WHERE deviceId = :deviceId AND word = :word")
    suspend fun getEntry(deviceId: String, word: String): DictionaryEntry?

    @Query("SELECT * FROM dictionary WHERE deviceId = :deviceId ORDER BY createdAt DESC")
    fun observeAll(deviceId: String): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary WHERE deviceId = :deviceId ORDER BY timesLookedUp DESC")
    suspend fun getMostLookedUp(deviceId: String): List<DictionaryEntry>

    @Query("UPDATE dictionary SET timesLookedUp = timesLookedUp + 1, updatedAt = :updatedAt WHERE deviceId = :deviceId AND word = :word")
    suspend fun incrementLookup(deviceId: String, word: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM dictionary WHERE id = :id")
    suspend fun delete(id: Long)
}
