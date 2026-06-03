package com.appenglish.domain.model

data class Subject(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val topicsCount: Int,
    val topics: List<TopicSummary>
)

data class TopicSummary(
    val id: String,
    val name: String,
    val order: Int,
    val difficulty: Int,
    val icon: String,
    val isLocked: Boolean = false,
    val completedUnits: Int = 0,
    val totalUnits: Int = 0,
    val percentComplete: Double = 0.0
)

data class Topic(
    val id: String,
    val name: String,
    val subjectId: String,
    val order: Int,
    val difficulty: Int,
    val icon: String,
    val theory: Theory,
    val units: List<com.appenglish.domain.model.Unit>,
    val testConfig: TestConfig
)

data class Theory(
    val text: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val tips: List<Tip>,
    val blocks: List<TheoryBlock> = emptyList()
)

data class Tip(
    val emoji: String,
    val text: String
)

data class UnitTheory(
    val text: String,
    val sections: List<TheorySection>,
    val headers: List<String>,
    val rows: List<List<String>>,
    val tips: List<Tip>,
    val blocks: List<TheoryBlock> = emptyList()
)

data class BlockTheory(
    val text: String,
    val sections: List<TheorySection>,
    val headers: List<String>,
    val rows: List<List<String>>,
    val tips: List<Tip>,
    val blocks: List<TheoryBlock> = emptyList()
)

data class TheoryBlock(
    val title: String?,
    val html: String?
)

data class TheorySection(
    val title: String,
    val text: String,
    val examples: List<String>
)

data class Unit(
    val id: String,
    val title: String,
    val exerciseType: String,
    val explanation: String,
    val inputMode: String = "tap",
    val isLocked: Boolean = false,
    val icon: String = "",
    val soundCorrectUrl: String? = null,
    val soundIncorrectUrl: String? = null,
    val theory: UnitTheory? = null,
    val blocks: List<ExerciseBlock>,
    val completedItems: Int = 0,
    val totalItems: Int = 0
) {
    val percent: Double
        get() = if (totalItems > 0) (completedItems.toDouble() / totalItems) * 100 else 0.0
}

data class ExerciseBlock(
    val title: String,
    val items: List<ExerciseItem>,
    val theory: BlockTheory? = null
)

data class ExerciseItem(
    val sentence: String,
    val answer: String,
    val hint: String?,
    val itemType: String = "fill-blank",
    val inputMode: String? = null,
    val answers: List<String>? = null,
    val options: List<String>? = null,
    val question: String? = null,
    val words: List<String>? = null,
    val correctOrder: List<String>? = null,
    val audioUrl: String? = null,
    val pairs: List<ExercisePair>? = null,
    val isCorrect: Boolean? = null
)

data class ExercisePair(
    val left: String = "",
    val right: String = ""
)

data class TestConfig(
    val totalQuestions: Int,
    val shuffle: Boolean,
    val includeUnits: List<String>
)

data class TestQuestion(
    val id: String,
    val unitId: String,
    val sentence: String,
    val answer: String,
    val hint: String?
)
