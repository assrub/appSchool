package com.appenglish.data.remote.dto

data class ProgressSyncRequest(
    val progress: List<ProgressEntryDto>,
    val blockProgress: List<BlockProgressEntryDto> = emptyList()
)

data class ProgressEntryDto(
    val topicId: String,
    val unitId: String,
    val completed: Boolean = false,
    val score: Int = 0,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val testScore: Int? = null,
    val completedAt: String? = null,
    // New pedagogical metrics
    val accuracy: Float = 0f,
    val mastery: Float = 0f,
    val status: String = "not_started",
    val itemsAttempted: Int = 0,
    val itemsMastered: Int = 0,
    val itemsCorrectFirst: Int = 0,
    val timeSpentSeconds: Int = 0
)

data class BlockProgressEntryDto(
    val topicId: String,
    val unitId: String,
    val blockIndex: Int,
    val completed: Boolean = false,
    val score: Int = 0,
    val totalItems: Int = 0,
    val completedAt: String? = null,
    val completedItems: Int = 0
)

data class ProgressSyncResponse(
    val status: String,
    val syncedAt: String,
    val syncedCount: Int
)

data class ProgressResponse(
    val deviceId: String,
    val subjects: List<ProgressSubjectDto>,
    val blockProgress: List<BlockProgressDto> = emptyList(),
    val lastSyncedAt: String? = null
)

data class ProgressSubjectDto(
    val subjectId: String,
    val topics: List<ProgressTopicDto>
)

data class ProgressTopicDto(
    val topicId: String,
    val units: List<ProgressUnitDto>,
    val testScore: Int? = null
)

data class ProgressUnitDto(
    val unitId: String,
    val completed: Boolean,
    val score: Int,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val testScore: Int? = null,
    // New pedagogical metrics
    val accuracy: Float = 0f,
    val mastery: Float = 0f,
    val status: String = "not_started",
    val itemsAttempted: Int = 0,
    val itemsMastered: Int = 0,
    val itemsCorrectFirst: Int = 0,
    val timeSpentSeconds: Int = 0
)

data class BlockProgressDto(
    val topicId: String,
    val unitId: String,
    val blockIndex: Int,
    val completed: Boolean,
    val score: Int,
    val totalItems: Int,
    val completedAt: String? = null,
    val completedItems: Int = 0
)

data class AnswerEntryDto(
    val topicId: String,
    val unitId: String,
    val givenAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean
)

data class AnswerBatchRequest(
    val answers: List<AnswerEntryDto>
)

data class AnswerBatchResponse(
    val status: String = "",
    val recorded: Int = 0
)
