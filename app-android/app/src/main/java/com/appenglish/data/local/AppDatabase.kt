package com.appenglish.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.appenglish.data.local.dao.DictionaryDao
import com.appenglish.data.local.dao.ProgressDao
import com.appenglish.data.local.entity.DictionaryEntry
import com.appenglish.data.local.entity.ProgressEntity

@Database(
    entities = [ProgressEntity::class, DictionaryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appenglish.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
