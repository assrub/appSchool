package com.appenglish.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.local.entity.ProgressEntity
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.Subject
import com.appenglish.domain.model.TopicSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val progressEntries: List<ProgressEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            contentRepository.getSubjects().fold(
                onSuccess = { response ->
                    val subjects = response.subjects.map { dto ->
                        Subject(
                            id = dto.id,
                            name = dto.name,
                            icon = dto.icon,
                            color = dto.color,
                            topicsCount = dto.topicsCount,
                            topics = dto.topics.map { t ->
                                TopicSummary(t.id, t.name, t.order, t.difficulty, t.icon, t.isLocked)
                            }
                        )
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, subjects = subjects)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )

            progressRepository.observeAllProgress().collect { entries ->
                _uiState.value = _uiState.value.copy(progressEntries = entries)
            }
        }
    }
}
