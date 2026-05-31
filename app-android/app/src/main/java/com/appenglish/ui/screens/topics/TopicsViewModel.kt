package com.appenglish.ui.screens.topics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.domain.model.TopicSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicsUiState(
    val isLoading: Boolean = true,
    val subjectName: String = "",
    val topics: List<TopicSummary> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TopicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val subjectId: String = savedStateHandle.get<String>("subjectId") ?: "english"

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init { loadTopics() }

    fun loadTopics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            contentRepository.getSubjects().fold(
                onSuccess = { response ->
                    val subject = response.subjects.find { it.id == subjectId }
                    if (subject != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            subjectName = subject.name,
                            topics = subject.topics.map { t ->
                                TopicSummary(
                                    id = t.id,
                                    name = t.name,
                                    order = t.order,
                                    difficulty = t.difficulty,
                                    icon = t.icon,
                                    isLocked = t.isLocked,
                                    completedUnits = t.progress?.completedUnits ?: 0,
                                    totalUnits = t.progress?.totalUnits ?: 0,
                                    percentComplete = t.progress?.percentComplete ?: 0.0
                                )
                            }
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Materia no encontrada")
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }
}
