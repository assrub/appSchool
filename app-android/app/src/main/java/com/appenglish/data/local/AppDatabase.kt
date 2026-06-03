package com.appenglish.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appenglish.data.local.dao.ContentCacheDao
import com.appenglish.data.local.dao.DictionaryDao
import com.appenglish.data.local.dao.PendingSyncDao
import com.appenglish.data.local.dao.ProgressDao
import com.appenglish.data.local.entity.BlockProgressEntity
import com.appenglish.data.local.entity.ContentCacheEntity
import com.appenglish.data.local.entity.DictionaryEntry
import com.appenglish.data.local.entity.PendingSyncEntity
import com.appenglish.data.local.entity.ProgressEntity

@Database(
    entities = [ProgressEntity::class, DictionaryEntry::class, BlockProgressEntity::class, ContentCacheEntity::class, PendingSyncEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun progressDao(): ProgressDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun contentCacheDao(): ContentCacheDao
    abstract fun pendingSyncDao(): PendingSyncDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Clean up duplicates before creating unique index
                db.execSQL("""
                    DELETE FROM progress WHERE id NOT IN (
                        SELECT MIN(id) FROM progress GROUP BY deviceId, topicId, unitId
                    )
                """)
                db.execSQL("""
                    DELETE FROM block_progress WHERE id NOT IN (
                        SELECT MIN(id) FROM block_progress GROUP BY deviceId, topicId, unitId, blockIndex
                    )
                """)
                // Create unique indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_progress_deviceId_topicId_unitId ON progress (deviceId, topicId, unitId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_block_progress_deviceId_topicId_unitId_blockIndex ON block_progress (deviceId, topicId, unitId, blockIndex)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_syncs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        syncType TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 5
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE progress ADD COLUMN accuracy REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress ADD COLUMN mastery REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress ADD COLUMN status TEXT NOT NULL DEFAULT 'not_started'")
                db.execSQL("ALTER TABLE progress ADD COLUMN itemsAttempted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress ADD COLUMN itemsMastered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress ADD COLUMN itemsCorrectFirst INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress ADD COLUMN timeSpentSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress ADD COLUMN lastActivityAt INTEGER")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE block_progress ADD COLUMN completedItems INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appenglish.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
