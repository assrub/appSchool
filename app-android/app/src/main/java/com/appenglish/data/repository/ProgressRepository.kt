package com.appenglish.data.repository

import com.appenglish.data.local.dao.ProgressDao
import com.appenglish.data.local.entity.BlockProgressEntity
import com.appenglish.data.local.entity.ProgressEntity
import com.appenglish.data.remote.api.AuthInterceptor
import com.appenglish.data.remote.api.ProgressApi
import com.appenglish.data.remote.dto.AnswerBatchRequest
import com.appenglish.data.remote.dto.AnswerEntryDto
import com.appenglish.data.remote.dto.BlockProgressEntryDto
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
    private val userId: String
        get() = AuthInterceptor.userId?.toString() ?: "0"

    fun getDeviceId(): String = userId

    suspend fun syncProgress(
        entries: List<ProgressEntryDto>,
        blockEntries: List<BlockProgressEntryDto> = emptyList()
    ): Result<Unit> = runCatching {
        api.syncProgress(ProgressSyncRequest(userId, entries, blockEntries))
    }

    suspend fun loadRemoteProgress(): Result<Unit> = runCatching {
        val response = api.getProgress(userId)
        for (subject in response.subjects) {
            for (topic in subject.topics) {
                for (unit in topic.units) {
                    val existing = dao.getProgress(userId, topic.topicId, unit.unitId)
                    val remoteScore = unit.score
                    val remoteCompleted = unit.completed
                    if (existing == null || (!existing.completed && remoteCompleted)) {
                        dao.upsert(
                            ProgressEntity(
                                deviceId = userId,
                                topicId = topic.topicId,
                                unitId = unit.unitId,
                                completed = remoteCompleted,
                                score = remoteScore,
                                totalItems = unit.totalItems,
                                completedItems = unit.completedItems,
                                testScore = unit.testScore ?: existing?.testScore
                            )
                        )
                    }
                }
            }
        }
        for (bp in response.blockProgress) {
            val existing = dao.getBlockProgress(userId, bp.topicId, bp.unitId, bp.blockIndex)
            if (existing == null || bp.score > existing.score) {
                dao.upsertBlockProgress(
                    BlockProgressEntity(
                        deviceId = userId,
                        topicId = bp.topicId,
                        unitId = bp.unitId,
                        blockIndex = bp.blockIndex,
                        score = bp.score,
                        totalItems = bp.totalItems,
                        completed = bp.completed
                    )
                )
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
        val existing = dao.getProgress(userId, topicId, unitId)
        if (existing != null) {
            dao.updateProgress(
                deviceId = userId,
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
        completed: Boolean
    ) {
        val existing = dao.getBlockProgress(userId, topicId, unitId, blockIndex)
        val bestScore = if (existing != null) maxOf(existing.score, score) else score
        dao.upsertBlockProgress(
            BlockProgressEntity(
                deviceId = userId,
                topicId = topicId,
                unitId = unitId,
                blockIndex = blockIndex,
                score = bestScore,
                totalItems = totalItems,
                completed = completed,
                completedAt = if (completed) System.currentTimeMillis() else existing?.completedAt
            )
        )
    }

    suspend fun getAllBlockProgress(topicId: String, unitId: String): List<BlockProgressEntity> {
        return dao.getAllBlockProgress(userId, topicId, unitId)
    }

    suspend fun recordAnswers(answers: List<AnswerEntryDto>): Result<Unit> = runCatching {
        api.recordAnswers(AnswerBatchRequest(userId, answers))
    }

    suspend fun resetAllProgress() {
        dao.deleteAllProgress(userId)
        dao.deleteAllBlockProgress(userId)
    }

    suspend fun resetUnitProgress(topicId: String, unitId: String) {
        dao.deleteUnitProgress(userId, topicId, unitId)
        dao.deleteUnitBlockProgress(userId, topicId, unitId)
    }
}
