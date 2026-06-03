package com.appenglish.ui.screens.blocks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.ExerciseItem
import com.appenglish.domain.model.BlockTheory
import com.appenglish.domain.model.TheoryBlock
import com.appenglish.domain.model.UnitTheory
import com.appenglish.domain.model.TheorySection
import com.appenglish.domain.model.Tip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockProgressData(
    val blockIndex: Int,
    val score: Int = 0,
    val totalItems: Int = 0,
    val completed: Boolean = false
)

data class BlocksUiState(
    val isLoading: Boolean = true,
    val unitName: String = "",
    val topicId: String = "",
    val unitId: String = "",
    val blocks: List<ExerciseBlock> = emptyList(),
    val blockProgress: Map<Int, BlockProgressData> = emptyMap(),
    val topicTheory: UnitTheory? = null,
    val unitTheory: UnitTheory? = null,
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class BlocksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val unitId: String = savedStateHandle.get<String>("unitId") ?: "affirmative"

    val getUnitId: String get() = unitId

    private val _uiState = MutableStateFlow(BlocksUiState())
    val uiState: StateFlow<BlocksUiState> = _uiState.asStateFlow()

    init { loadBlocks() }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun refreshProgress() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.blocks.isEmpty()) return@launch
            val blockProgressList = progressRepository.getAllBlockProgress(state.topicId, state.unitId)
            val blockProgress = blockProgressList.associate {
                it.blockIndex to BlockProgressData(
                    blockIndex = it.blockIndex,
                    score = it.score,
                    totalItems = it.totalItems,
                    completed = it.completed
                )
            }
            _uiState.value = state.copy(blockProgress = blockProgress)
        }
    }

    fun redoBlock(blockIndex: Int) {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.blocks.isNotEmpty()) {
                progressRepository.saveBlockProgress(
                    topicId = s.topicId, unitId = s.unitId,
                    blockIndex = blockIndex, score = 0,
                    totalItems = s.blocks[blockIndex].items.size, completed = false
                )
                refreshProgress()
            }
        }
    }

    fun loadBlocks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            contentRepository.getSubjects().fold(
                onSuccess = { response ->
                    for (subject in response.subjects) {
                        for (topicSummary in subject.topics) {
                            contentRepository.getTopic(topicSummary.id).fold(
                                onSuccess = { topic ->
                                    val unit = topic.units.find { it.id == unitId }
                                    if (unit != null) {
                                        val blockProgressList = progressRepository.getAllBlockProgress(topic.id, unit.id)
                                        val blockProgress = blockProgressList.associate {
                                            it.blockIndex to BlockProgressData(
                                                blockIndex = it.blockIndex,
                                                score = it.score,
                                                totalItems = it.totalItems,
                                                completed = it.completed
                                            )
                                        }
                                        val topicTheory = UnitTheory(
                                            text = topic.theory.text,
                                            sections = topic.theory.blocks?.map { TheorySection(it.title ?: "", it.html ?: "", emptyList()) } ?: emptyList(),
                                            headers = topic.theory.table?.headers ?: emptyList(),
                                            rows = topic.theory.table?.rows ?: emptyList(),
                                            tips = topic.theory.tips.map { Tip(it.emoji, it.text) },
                                            blocks = topic.theory.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList()
                                        )
                                        val unitTheory = unit.theory?.let { ut ->
                                            UnitTheory(
                                                text = ut.text,
                                                sections = ut.sections?.map { TheorySection(it.title, it.text, it.examples ?: emptyList()) } ?: emptyList(),
                                                headers = ut.table?.headers ?: emptyList(),
                                                rows = ut.table?.rows ?: emptyList(),
                                                tips = ut.tips?.map { Tip(it.emoji, it.text) } ?: emptyList(),
                                                blocks = ut.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList()
                                            )
                                        }
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            unitName = unit.title,
                                            topicId = topic.id,
                                            unitId = unit.id,
                                            topicTheory = topicTheory,
                                            unitTheory = unitTheory,
                                            blockProgress = blockProgress,
                                            blocks = unit.blocks.mapIndexed { idx, b ->
                                                val blockTheory = b.theory?.let { bt ->
                                                    BlockTheory(
                                                        text = bt.text,
                                                        sections = bt.sections?.map { TheorySection(it.title, it.text, it.examples ?: emptyList()) } ?: emptyList(),
                                                        headers = bt.table?.headers ?: emptyList(),
                                                        rows = bt.table?.rows ?: emptyList(),
                                                        tips = bt.tips?.map { Tip(it.emoji, it.text) } ?: emptyList(),
                                                        blocks = bt.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList()
                                                    )
                                                }
                                                ExerciseBlock(b.title, b.items.map { ExerciseItem(it.sentence, it.answer, it.hint, it.itemType, it.inputMode, it.answers, it.options, it.question, it.words, it.correctOrder, it.audioUrl, it.pairs?.map { p -> com.appenglish.domain.model.ExercisePair(p.left, p.right) }, it.isCorrect) }, blockTheory)
                                            }
                                        )
                                        return@launch
                                    }
                                },
                                onFailure = {}
                            )
                        }
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Unidad no encontrada")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }
}