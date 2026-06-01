package com.appenglish.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.domain.model.Subject
import com.appenglish.domain.model.TopicSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectsUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            contentRepository.getSubjects().fold(
                onSuccess = { response ->
                    val subjects = response.subjects.map { dto ->
                        Subject(
                            id = dto.id,
                            name = dto.name,
                            icon = dto.icon,
                            color = dto.color,
                            topicsCount = dto.topicsCount,
                            topics = dto.topics.map { topic ->
                                TopicSummary(
                                    id = topic.id,
                                    name = topic.name,
                                    order = topic.order,
                                    difficulty = topic.difficulty,
                                    icon = topic.icon,
                                    isLocked = topic.isLocked,
                                    completedUnits = topic.progress?.completedUnits ?: 0,
                                    totalUnits = topic.progress?.totalUnits ?: 0,
                                    percentComplete = topic.progress?.percentComplete ?: 0.0
                                )
                            }
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        subjects = subjects
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar materias"
                    )
                }
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }
}
