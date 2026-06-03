package com.appenglish.data.repository

import android.content.Context
import com.appenglish.data.local.dao.PendingSyncDao
import com.appenglish.data.local.dao.ProgressDao
import com.appenglish.data.local.entity.BlockProgressEntity
import com.appenglish.data.local.entity.PendingSyncEntity
import com.appenglish.data.local.entity.ProgressEntity
import com.appenglish.data.remote.api.AuthInterceptor
import com.appenglish.data.remote.api.ProgressApi
import com.appenglish.data.remote.dto.AnswerBatchRequest
import com.appenglish.data.remote.dto.AnswerEntryDto
import com.appenglish.data.remote.dto.BlockProgressEntryDto
import com.appenglish.data.remote.dto.ProgressEntryDto
import com.appenglish.data.remote.dto.ProgressSyncRequest
import com.appenglish.workers.SyncWorker
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val api: ProgressApi,
    private val dao: ProgressDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson,
    private val context: Context
) {
    private val userId: String
        get() = AuthInterceptor.userId?.toString() ?: "0"

    fun getDeviceId(): String = userId

    suspend fun syncProgress(
        entries: List<ProgressEntryDto>,
        blockEntries: List<BlockProgressEntryDto> = emptyList()
    ): Result<Unit> {
        val request = ProgressSyncRequest(entries, blockEntries)
        return try {
            api.syncProgress(request)
            Result.success(Unit)
        } catch (e: Exception) {
            // Save to pending sync queue for retry
            val payload = gson.toJson(request)
            pendingSyncDao.insert(
                PendingSyncEntity(
                    syncType = "progress",
                    payload = payload
                )
            )
            // Enqueue WorkManager to retry
            SyncWorker.enqueue(context)
            Result.failure(e)
        }
    }

    suspend fun recordAnswers(answers: List<AnswerEntryDto>): Result<Unit> {
        val request = AnswerBatchRequest(answers)
        return try {
            api.recordAnswers(request)
            Result.success(Unit)
        } catch (e: Exception) {
            // Save to pending sync queue for retry
            val payload = gson.toJson(request)
            pendingSyncDao.insert(
                PendingSyncEntity(
                    syncType = "answers",
                    payload = payload
                )
            )
            // Enqueue WorkManager to retry
            SyncWorker.enqueue(context)
            Result.failure(e)
        }
    }

    suspend fun loadRemoteProgress(): Result<Unit> = runCatching {
        val response = api.getProgress()
        for (subject in response.subjects) {
            for (topic in subject.topics) {
                for (unit in topic.units) {
                    val existing = dao.getProgress(userId, topic.topicId, unit.unitId)
                    val remoteScore = unit.score
                    val remoteCompleted = unit.completed

                    if (existing == null) {
                        // No local record - create from remote
                        dao.upsert(
                            ProgressEntity(
                                deviceId = userId,
                                topicId = topic.topicId,
                                unitId = unit.unitId,
                                completed = remoteCompleted,
                                score = remoteScore,
                                totalItems = unit.totalItems,
                                completedItems = unit.completedItems,
                                testScore = unit.testScore,
                                startedAt = System.currentTimeMillis(),
                                // New pedagogical metrics from remote
                                accuracy = unit.accuracy,
                                mastery = unit.mastery,
                                status = unit.status,
                                itemsAttempted = unit.itemsAttempted,
                                itemsMastered = unit.itemsMastered,
                                itemsCorrectFirst = unit.itemsCorrectFirst,
                                timeSpentSeconds = unit.timeSpentSeconds
                            )
                        )
                    }
                    // If local exists, always keep local - user is actively working
                    // Local will sync TO backend, not the other way around
                }
            }
        }
        for (bp in response.blockProgress) {
            val existing = dao.getBlockProgress(userId, bp.topicId, bp.unitId, bp.blockIndex)
            if (existing == null) {
                // Only create from remote if no local record exists
                val remoteCompletedAt = bp.completedAt?.let { parseTimestamp(it) }
                dao.upsertBlockProgress(
                    BlockProgressEntity(
                        deviceId = userId,
                        topicId = bp.topicId,
                        unitId = bp.unitId,
                        blockIndex = bp.blockIndex,
                        score = bp.score,
                        totalItems = bp.totalItems,
                        completed = bp.completed,
                        completedAt = remoteCompletedAt
                    )
                )
            }
            // If local exists, always keep local (user is actively working)
        }
    }

    private fun parseTimestamp(timestamp: String): Long? {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(timestamp)?.time
        } catch (_: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .parse(timestamp)?.time
            } catch (_: Exception) { null }
        }
    }

    suspend fun saveProgress(
        topicId: String,
        unitId: String,
        completedItems: Int,
        score: Int,
        totalItems: Int,
        completed: Boolean
    ) {
        val existing = dao.getProgress(userId, topicId, unitId)
        if (existing != null) {
            dao.updateProgress(
                deviceId = userId,
                topicId = topicId,
                unitId = unitId,
                completedItems = completedItems,
                score = score,
                totalItems = totalItems,
                completed = completed,
                completedAt = if (completed) System.currentTimeMillis() else null
            )
        } else {
            dao.upsert(
                ProgressEntity(
                    deviceId = userId,
                    topicId = topicId,
                    unitId = unitId,
                    completedItems = completedItems,
                    score = score,
                    totalItems = totalItems,
                    completed = completed,
                    completedAt = if (completed) System.currentTimeMillis() else null
                )
            )
        }
    }

    suspend fun saveTestScore(topicId: String, testScore: Int) {
        dao.updateTestScore(userId, topicId, testScore)
    }

    suspend fun getProgress(topicId: String, unitId: String): ProgressEntity? {
        return dao.getProgress(userId, topicId, unitId)
    }

    suspend fun getTopicProgress(topicId: String): List<ProgressEntity> {
        return dao.getTopicProgress(userId, topicId)
    }

    fun observeAllProgress(): Flow<List<ProgressEntity>> {
        return dao.observeAllProgress(userId)
    }

    suspend fun getBlockProgress(topicId: String, unitId: String, blockIndex: Int): BlockProgressEntity? {
        return dao.getBlockProgress(userId, topicId, unitId, blockIndex)
    }

    suspend fun saveBlockProgress(
        topicId: String,
        unitId: String,
        blockIndex: Int,
        score: Int,
        totalItems: Int,
        completed: Boolean,
        completedItems: Int = 0
    ) {
        val existing = dao.getBlockProgress(userId, topicId, unitId, blockIndex)
        val bestScore = if (existing != null) maxOf(existing.score, score) else score
        val maxCompletedItems = if (existing != null) maxOf(existing.completedItems, completedItems) else completedItems
        dao.upsertBlockProgress(
            BlockProgressEntity(
                deviceId = userId,
                topicId = topicId,
                unitId = unitId,
                blockIndex = blockIndex,
                score = bestScore,
                totalItems = totalItems,
                completed = completed,
                completedItems = maxCompletedItems,
                completedAt = if (completed) System.currentTimeMillis() else existing?.completedAt
            )
        )
    }

    suspend fun getAllBlockProgress(topicId: String, unitId: String): List<BlockProgressEntity> {
        return dao.getAllBlockProgress(userId, topicId, unitId)
    }

    suspend fun saveMetrics(
        topicId: String,
        unitId: String,
        accuracy: Float,
        mastery: Float,
        status: String,
        itemsAttempted: Int,
        itemsMastered: Int,
        itemsCorrectFirst: Int,
        timeSpentSeconds: Int
    ) {
        dao.updateMetrics(
            deviceId = userId,
            topicId = topicId,
            unitId = unitId,
            accuracy = accuracy,
            mastery = mastery,
            status = status,
            itemsAttempted = itemsAttempted,
            itemsMastered = itemsMastered,
            itemsCorrectFirst = itemsCorrectFirst,
            timeSpentSeconds = timeSpentSeconds,
            lastActivityAt = System.currentTimeMillis()
        )
    }

    suspend fun resetAllProgress() {
        dao.deleteAllProgress(userId)
        dao.deleteAllBlockProgress(userId)
        // Sync reset to backend - send empty sync to clear server data
        try {
            api.syncProgress(ProgressSyncRequest(
                progress = emptyList(),
                blockProgress = emptyList()
            ))
        } catch (_: Exception) {}
    }

    suspend fun resetUnitProgress(topicId: String, unitId: String) {
        dao.deleteUnitProgress(userId, topicId, unitId)
        dao.deleteUnitBlockProgress(userId, topicId, unitId)
        // Sync reset to backend for this unit
        try {
            api.syncProgress(ProgressSyncRequest(
                progress = listOf(
                    ProgressEntryDto(
                        topicId = topicId,
                        unitId = unitId,
                        completed = false,
                        score = 0,
                        totalItems = 0,
                        completedItems = 0,
                        status = "not_started"
                    )
                ),
                blockProgress = emptyList()
            ))
        } catch (_: Exception) {}
    }

    suspend fun resetBlockProgress(topicId: String, unitId: String, blockIndex: Int, totalItems: Int) {
        // Reset local
        dao.upsertBlockProgress(
            BlockProgressEntity(
                deviceId = userId,
                topicId = topicId,
                unitId = unitId,
                blockIndex = blockIndex,
                score = 0,
                totalItems = totalItems,
                completed = false,
                completedAt = null
            )
        )
        // Sync reset to backend
        try {
            api.syncProgress(ProgressSyncRequest(
                progress = emptyList(),
                blockProgress = listOf(
                    BlockProgressEntryDto(
                        topicId = topicId,
                        unitId = unitId,
                        blockIndex = blockIndex,
                        completed = false,
                        score = 0,
                        totalItems = totalItems
                    )
                )
            ))
        } catch (_: Exception) {}
    }
}
