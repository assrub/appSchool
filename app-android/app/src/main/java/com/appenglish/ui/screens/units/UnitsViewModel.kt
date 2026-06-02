package com.appenglish.ui.screens.units

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.Unit as DomainUnit
import com.appenglish.domain.model.UnitTheory
import com.appenglish.domain.model.Theory
import com.appenglish.domain.model.Tip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitsUiState(
    val isLoading: Boolean = true,
    val topicName: String = "",
    val topicTheory: Theory? = null,
    val units: List<DomainUnit> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class UnitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: "verb-to-be"

    private val _uiState = MutableStateFlow(UnitsUiState())
    val uiState: StateFlow<UnitsUiState> = _uiState.asStateFlow()

    init { loadUnits() }

    fun loadUnits() {
        viewModelScope.launch {
            if (_uiState.value.units.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            contentRepository.getTopic(topicId).fold(
                onSuccess = { response ->
                    val localProgress = progressRepository.getTopicProgress(topicId)
                    val units = response.units.map { unitDto ->
                        val local = localProgress.find { it.unitId == unitDto.id }
                        val total = unitDto.blocks.sumOf { b -> b.items.size }
                        DomainUnit(
                            id = unitDto.id, title = unitDto.title, exerciseType = unitDto.exerciseType,
                            explanation = unitDto.explanation, inputMode = unitDto.inputMode,
                            isLocked = unitDto.isLocked,
                            icon = unitDto.icon,
                            blocks = emptyList(),
                            completedItems = local?.completedItems ?: unitDto.progress?.completedItems ?: 0,
                            totalItems = total
                        )
                    }
                    val topicTheory = Theory(
                        text = response.theory.text,
                        headers = response.theory.table?.headers ?: emptyList(),
                        rows = response.theory.table?.rows ?: emptyList(),
                        tips = response.theory.tips.map { Tip(it.emoji, it.text) }
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        topicName = response.name,
                        topicTheory = topicTheory,
                        units = units
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun resetUnitProgress(unitId: String) {
        viewModelScope.launch {
            progressRepository.resetUnitProgress(topicId, unitId)
            loadUnits()
        }
    }
}