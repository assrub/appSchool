package com.appenglish.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appenglish.data.local.entity.DictionaryEntry
import com.appenglish.data.remote.api.TranslateApi
import com.appenglish.data.remote.api.TranslateRequest
import com.appenglish.data.repository.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DictionaryUiState(
    val entries: List<DictionaryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val translationText: String = "",
    val translatedWord: String = "",
    val isTranslating: Boolean = false
)

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val translateApi: TranslateApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            dictionaryRepository.observeAll().collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
            }
        }
    }

    fun translateWord(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTranslating = true)
            try {
                val response = translateApi.translate(TranslateRequest(word))
                dictionaryRepository.addEntry(word, response.translation)
                _uiState.value = _uiState.value.copy(
                    translatedWord = response.translation,
                    isTranslating = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    translatedWord = "Error: ${e.message}",
                    isTranslating = false
                )
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            dictionaryRepository.delete(id)
        }
    }

    fun onTranslationTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(translationText = text)
    }
}
