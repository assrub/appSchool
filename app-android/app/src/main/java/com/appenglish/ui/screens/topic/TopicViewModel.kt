package com.appenglish.ui.screens.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.Topic
import com.appenglish.domain.model.Theory
import com.appenglish.domain.model.Tip
import com.appenglish.domain.model.UnitTheory
import com.appenglish.domain.model.BlockTheory
import com.appenglish.domain.model.TheorySection
import com.appenglish.domain.model.TheoryBlock
import com.appenglish.domain.model.Unit
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.ExerciseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicUiState(
    val isLoading: Boolean = true,
    val topic: Topic? = null,
    val error: String? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: "verb-to-be"

    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()

    init {
        loadTopic()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadTopic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            contentRepository.getTopic(topicId).fold(
                onSuccess = { response ->
                    val localProgress = progressRepository.getTopicProgress(topicId)

                    val units = response.units.map { unitDto ->
                        val local = localProgress.find { it.unitId == unitDto.id }
                        val total = unitDto.blocks.sumOf { b -> b.items.size }

                        val unitTheory = unitDto.theory?.let { t ->
                            UnitTheory(
                                text = t.text,
                                sections = t.sections?.map { s ->
                                    TheorySection(s.title, s.text, s.examples ?: emptyList())
                                } ?: emptyList(),
                                headers = t.table?.headers ?: emptyList(),
                                rows = t.table?.rows ?: emptyList(),
                                tips = t.tips?.map { Tip(it.emoji, it.text) } ?: emptyList(),
                                blocks = t.blocks?.map { com.appenglish.domain.model.TheoryBlock(it.title, it.html) } ?: emptyList()
                            )
                        }

                        Unit(
                            id = unitDto.id,
                            title = unitDto.title,
                            exerciseType = unitDto.exerciseType,
                            explanation = unitDto.explanation,
                            isLocked = unitDto.isLocked,
                            icon = unitDto.icon,
                            soundCorrectUrl = unitDto.soundCorrectUrl,
                            soundIncorrectUrl = unitDto.soundIncorrectUrl,
                            theory = unitTheory,
                            blocks = unitDto.blocks.map { blockDto ->
                                val blockTheory = blockDto.theory?.let { bt ->
                                    BlockTheory(
                                        text = bt.text,
                                        sections = bt.sections?.map { TheorySection(it.title, it.text, it.examples ?: emptyList()) } ?: emptyList(),
                                        headers = bt.table?.headers ?: emptyList(),
                                        rows = bt.table?.rows ?: emptyList(),
                                        tips = bt.tips?.map { Tip(it.emoji, it.text) } ?: emptyList(),
                                        blocks = bt.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList()
                                    )
                                }
                                ExerciseBlock(
                                    title = blockDto.title,
                                    items = blockDto.items.map { itemDto ->
                                        ExerciseItem(
                                            sentence = itemDto.sentence,
                                            answer = itemDto.answer,
                                            hint = itemDto.hint,
                                            itemType = itemDto.itemType,
                                            inputMode = itemDto.inputMode,
                                            answers = itemDto.answers,
                                            options = itemDto.options,
                                            question = itemDto.question,
                                            words = itemDto.words,
                                            correctOrder = itemDto.correctOrder,
                                            audioUrl = itemDto.audioUrl,
                                            pairs = itemDto.pairs,
                                            isCorrect = itemDto.isCorrect
                                        )
                                    },
                                    theory = blockTheory
                                )
                            },
                            completedItems = local?.completedItems ?: unitDto.progress?.completedItems ?: 0,
                            totalItems = total
                        )
                    }

                    val theory = Theory(
                        text = response.theory.text,
                        headers = response.theory.table?.headers ?: emptyList(),
                        rows = response.theory.table?.rows ?: emptyList(),
                        tips = response.theory.tips.map { Tip(it.emoji, it.text) }
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        topic = Topic(
                            id = response.id,
                            name = response.name,
                            subjectId = response.subjectId,
                            order = response.order,
                            difficulty = response.difficulty,
                            icon = response.icon,
                            theory = theory,
                            units = units,
                            testConfig = com.appenglish.domain.model.TestConfig(
                                totalQuestions = response.testConfig.totalQuestions,
                                shuffle = response.testConfig.shuffle,
                                includeUnits = response.testConfig.includeUnits
                            )
                        )
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el tema"
                    )
                }
            )
        }
    }
}
