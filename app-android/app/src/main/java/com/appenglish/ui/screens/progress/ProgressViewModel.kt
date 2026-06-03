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

data class SubjectProgress(
    val subject: Subject,
    val topics: List<TopicProgress>
)

data class TopicProgress(
    val topic: TopicSummary,
    val units: List<UnitProgress>
)

data class UnitProgress(
    val unitId: String,
    val unitTitle: String,
    val progress: ProgressEntity?
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _subjectProgress = MutableStateFlow<List<SubjectProgress>>(emptyList())
    val subjectProgress: StateFlow<List<SubjectProgress>> = _subjectProgress.asStateFlow()

    private var subjectsCache: List<Subject> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            contentRepository.getSubjects().fold(
                onSuccess = { response ->
                    subjectsCache = response.subjects.map { dto ->
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
                    _uiState.value = _uiState.value.copy(isLoading = false, subjects = subjectsCache)

                    // Collect progress reactively so it auto-updates when Room changes
                    progressRepository.observeAllProgress().collect { entries ->
                        val subjectProgressList = buildSubjectProgress(subjectsCache, entries)
                        _subjectProgress.value = subjectProgressList
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    private fun buildSubjectProgress(
        subjects: List<Subject>,
        entries: List<ProgressEntity>
    ): List<SubjectProgress> {
        return subjects.map { subject ->
            SubjectProgress(
                subject = subject,
                topics = subject.topics.map { topic ->
                    val topicEntries = entries.filter { it.topicId == topic.id }
                    TopicProgress(
                        topic = topic,
                        units = topicEntries.map { entry ->
                            UnitProgress(
                                unitId = entry.unitId,
                                unitTitle = entry.unitId.replace("-", " ").replaceFirstChar { it.uppercase() },
                                progress = entry
                            )
                        }
                    )
                }
            )
        }
    }
}