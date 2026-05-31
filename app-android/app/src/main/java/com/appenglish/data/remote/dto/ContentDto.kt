package com.appenglish.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubjectsResponse(
    val subjects: List<SubjectDto>
)

data class SubjectDto(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val topicsCount: Int,
    val topics: List<TopicSummaryDto>
)

data class TopicSummaryDto(
    val id: String,
    val name: String,
    val order: Int,
    val difficulty: Int,
    val icon: String,
    val isLocked: Boolean = false,
    val progress: TopicProgressDto?
)

data class TopicProgressDto(
    val completedUnits: Int = 0,
    val totalUnits: Int = 0,
    val percentComplete: Double = 0.0
)

data class TopicResponse(
    val id: String,
    val name: String,
    val subjectId: String,
    val order: Int,
    val difficulty: Int,
    val icon: String,
    val theory: TheoryDto,
    val units: List<UnitDto>,
    val testConfig: TestConfigDto
)

data class TheoryDto(
    val text: String,
    val table: TableDto?,
    val tips: List<TipDto>
)

data class TableDto(
    val headers: List<String>,
    val rows: List<List<String>>
)

data class TipDto(
    val emoji: String,
    val text: String
)

data class UnitDto(
    val id: String,
    val title: String,
    val exerciseType: String,
    val explanation: String,
    val isLocked: Boolean = false,
    val progress: UnitProgressDto?,
    val theory: UnitTheoryDto?,
    val blocks: List<ExerciseBlockDto>
)

data class UnitTheoryDto(
    val text: String,
    val sections: List<TheorySectionDto>?,
    val table: TableDto?,
    val tips: List<TipDto>?
)

data class TheorySectionDto(
    val title: String,
    val text: String,
    val examples: List<String>?
)

data class UnitProgressDto(
    val completedItems: Int = 0,
    val totalItems: Int = 0
) {
    val percent: Double
        get() = if (totalItems > 0) (completedItems.toDouble() / totalItems) * 100 else 0.0
}

data class ExerciseBlockDto(
    val title: String,
    val items: List<ExerciseItemDto>
)

data class ExerciseItemDto(
    val sentence: String,
    val answer: String,
    val hint: String?
)

data class TestConfigDto(
    val totalQuestions: Int,
    val shuffle: Boolean,
    val includeUnits: List<String>
)

data class TestResponse(
    val topicId: String,
    val questions: List<TestQuestionDto>
)

data class TestQuestionDto(
    val id: String,
    val unitId: String,
    val sentence: String,
    val answer: String,
    val hint: String?
)
