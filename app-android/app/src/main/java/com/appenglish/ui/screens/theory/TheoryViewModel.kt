package com.appenglish.ui.screens.theory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.domain.model.TheoryBlock
import com.appenglish.domain.model.Tip
import com.appenglish.domain.model.TheorySection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TheoryUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val theoryBlocks: List<TheoryBlock> = emptyList(),
    val theoryText: String = "",
    val theorySections: List<TheorySection> = emptyList(),
    val tips: List<Tip> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TheoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: ""
    private val unitId: String? = savedStateHandle.get<String>("unitId")

    private val _uiState = MutableStateFlow(TheoryUiState())
    val uiState: StateFlow<TheoryUiState> = _uiState.asStateFlow()

    init { loadTheory() }

    fun loadTheory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            contentRepository.getTopic(topicId).fold(
                onSuccess = { response ->
                    if (unitId != null) {
                        val unit = response.units.find { it.id == unitId }
                        if (unit?.theory != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                title = unit.title,
                                theoryBlocks = unit.theory.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList(),
                                theoryText = unit.theory.text,
                                theorySections = unit.theory.sections?.map { TheorySection(it.title, it.text, it.examples ?: emptyList()) } ?: emptyList(),
                                tips = unit.theory.tips?.map { Tip(it.emoji, it.text) } ?: emptyList()
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Esta unidad no tiene teoría"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            title = response.name,
                            theoryBlocks = response.theory.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList(),
                            theoryText = response.theory.text,
                            theorySections = emptyList(),
                            tips = response.theory.tips.map { Tip(it.emoji, it.text) }
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }
}