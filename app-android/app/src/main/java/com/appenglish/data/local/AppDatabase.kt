package com.appenglish.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appenglish.data.local.dao.ContentCacheDao
import com.appenglish.data.local.dao.DictionaryDao
import com.appenglish.data.local.dao.ProgressDao
import com.appenglish.data.local.entity.BlockProgressEntity
import com.appenglish.data.local.entity.ContentCacheEntity
import com.appenglish.data.local.entity.DictionaryEntry
import com.appenglish.data.local.entity.ProgressEntity

@Database(
    entities = [ProgressEntity::class, DictionaryEntry::class, BlockProgressEntity::class, ContentCacheEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun contentCacheDao(): ContentCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS block_progress (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        deviceId TEXT NOT NULL,
                        topicId TEXT NOT NULL,
                        unitId TEXT NOT NULL,
                        blockIndex INTEGER NOT NULL,
                        score INTEGER NOT NULL DEFAULT 0,
                        totalItems INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS content_cache (
                        cacheKey TEXT PRIMARY KEY NOT NULL,
                        jsonData TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appenglish.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
