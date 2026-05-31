package com.appenglish.ui.screens.blocks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.ExerciseItem
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
    val blocks: List<ExerciseBlock> = emptyList(),
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
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            unitName = unit.title,
                                            topicId = topic.id,
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
