package com.appenglish.ui.screens.topics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
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
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class TopicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val subjectId: String = savedStateHandle.get<String>("subjectId") ?: "english"

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init { loadTopics() }

    fun loadTopics() {
        viewModelScope.launch {
            if (_uiState.value.topics.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            contentRepository.getSubjects().fold(
                onSuccess = { response ->
                    val subject = response.subjects.find { it.id == subjectId }
                    if (subject != null) {
                        val topics = subject.topics.map { t ->
                            val localProgress = progressRepository.getTopicProgress(t.id)
                            val totalUnits = localProgress.size
                            val completedUnits = localProgress.count { it.completed }
                            TopicSummary(
                                id = t.id,
                                name = t.name,
                                order = t.order,
                                difficulty = t.difficulty,
                                icon = t.icon,
                                isLocked = t.isLocked,
                                completedUnits = completedUnits,
                                totalUnits = totalUnits,
                                percentComplete = if (totalUnits > 0) (completedUnits.toDouble() / totalUnits * 100) else 0.0
                            )
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false, subjectName = subject.name, topics = topics)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Materia no encontrada")
                    }
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error") }
            )
        }
    }
}
