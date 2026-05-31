package com.appenglish.data.repository

import com.appenglish.data.local.dao.ProgressDao
import com.appenglish.data.local.entity.ProgressEntity
import com.appenglish.data.remote.api.ProgressApi
import com.appenglish.data.remote.dto.ProgressEntryDto
import com.appenglish.data.remote.dto.ProgressSyncRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val api: ProgressApi,
    private val dao: ProgressDao
) {
    private val deviceId: String = "android-${java.util.UUID.randomUUID().toString().take(8)}"

    fun getDeviceId(): String = deviceId

    suspend fun syncProgress(entries: List<ProgressEntryDto>): Result<Unit> = runCatching {
        api.syncProgress(ProgressSyncRequest(deviceId, entries))
    }

    suspend fun loadRemoteProgress(): Result<Unit> = runCatching {
        val response = api.getProgress(deviceId)
        for (subject in response.subjects) {
            for (topic in subject.topics) {
                for (unit in topic.units) {
                    val existing = dao.getProgress(deviceId, topic.topicId, unit.unitId)
                    if (existing == null || !existing.completed) {
                        dao.upsert(
                            ProgressEntity(
                                deviceId = deviceId,
                                topicId = topic.topicId,
                                unitId = unit.unitId,
                                completed = unit.completed,
                                score = unit.score
                            )
                        )
                    }
                }
            }
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
        val existing = dao.getProgress(deviceId, topicId, unitId)
        if (existing != null) {
            dao.updateProgress(
                deviceId = deviceId,
                topicId = topicId,
                unitId = unitId,
                completedItems = completedItems,
                score = score,
                completed = completed,
                completedAt = if (completed) System.currentTimeMillis() else null
            )
        } else {
            dao.upsert(
                ProgressEntity(
                    deviceId = deviceId,
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
        dao.updateTestScore(deviceId, topicId, testScore)
    }

    suspend fun getProgress(topicId: String, unitId: String): ProgressEntity? {
        return dao.getProgress(deviceId, topicId, unitId)
    }

    suspend fun getTopicProgress(topicId: String): List<ProgressEntity> {
        return dao.getTopicProgress(deviceId, topicId)
    }

    fun observeAllProgress(): Flow<List<ProgressEntity>> {
        return dao.observeAllProgress(deviceId)
    }
}
