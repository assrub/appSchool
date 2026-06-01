package com.appenglish.ui.screens.blocks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.ExerciseItem
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

data class BlocksUiState(
    val isLoading: Boolean = true,
    val unitName: String = "",
    val topicId: String = "",
    val unitId: String = "",
    val blocks: List<ExerciseBlock> = emptyList(),
    val topicTheory: UnitTheory? = null,
    val unitTheory: UnitTheory? = null,
    val error: String? = null
)

@HiltViewModel
class BlocksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val unitId: String = savedStateHandle.get<String>("unitId") ?: "affirmative"

    val getUnitId: String get() = unitId

    private val _uiState = MutableStateFlow(BlocksUiState())
    val uiState: StateFlow<BlocksUiState> = _uiState.asStateFlow()

    init { loadBlocks() }

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
                                        val topicTheory = UnitTheory(
                                            text = topic.theory.text,
                                            sections = emptyList(),
                                            headers = topic.theory.table?.headers ?: emptyList(),
                                            rows = topic.theory.table?.rows ?: emptyList(),
                                            tips = topic.theory.tips.map { Tip(it.emoji, it.text) }
                                        )
                                        val unitTheory = unit.theory?.let { ut ->
                                            UnitTheory(
                                                text = ut.text,
                                                sections = emptyList(),
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
                                            blocks = unit.blocks.map { b ->
                                                ExerciseBlock(b.title, b.items.map { ExerciseItem(it.sentence, it.answer, it.hint) })
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
