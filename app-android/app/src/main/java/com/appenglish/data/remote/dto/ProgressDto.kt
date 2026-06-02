package com.appenglish.data.remote.dto

data class ProgressSyncRequest(
    val deviceId: String,
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
    val completedAt: String? = null
)

data class BlockProgressEntryDto(
    val topicId: String,
    val unitId: String,
    val blockIndex: Int,
    val completed: Boolean = false,
    val score: Int = 0,
    val totalItems: Int = 0,
    val completedAt: String? = null
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
    val testScore: Int? = null
)

data class BlockProgressDto(
    val topicId: String,
    val unitId: String,
    val blockIndex: Int,
    val completed: Boolean,
    val score: Int,
    val totalItems: Int,
    val completedAt: String? = null
)
