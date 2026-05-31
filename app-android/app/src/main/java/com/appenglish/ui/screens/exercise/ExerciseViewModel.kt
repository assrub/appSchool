package com.appenglish.ui.screens.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.UnitTheory
import com.appenglish.domain.model.TheorySection
import com.appenglish.domain.model.Tip
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.ExerciseItem
import com.appenglish.ui.components.SoundHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WrongAnswer(
    val blockIndex: Int,
    val itemIndex: Int,
    val sentence: String,
    val givenAnswer: String,
    val correctAnswer: String
)

data class UnitExerciseUiState(
    val isLoading: Boolean = true,
    val unitTitle: String = "",
    val unitExplanation: String = "",
    val unitTheory: UnitTheory? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
    val currentBlockIndex: Int = 0,
    val currentItemIndex: Int = 0,
    val userInput: String = "",
    val feedback: Feedback? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val totalBlocks: Int = 0,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val isFinished: Boolean = false,
    val error: String? = null,
    val showingAnswer: Boolean = false,
    val playingFullAudio: Boolean = false,
    val readyForNext: Boolean = false,
    val fullSentenceToPlay: String = "",
    val wrongItems: List<WrongAnswer> = emptyList(),
    val retryMode: Boolean = false,
    val retryIndex: Int = 0
)

data class Feedback(
    val message: String,
    val isCorrect: Boolean
)

@HiltViewModel
class UnitExerciseViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : AndroidViewModel(application) {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: "verb-to-be"
    private val unitId: String = savedStateHandle.get<String>("unitId") ?: "affirmative"

    private val _uiState = MutableStateFlow(UnitExerciseUiState())
    val uiState: StateFlow<UnitExerciseUiState> = _uiState.asStateFlow()

    init { loadUnit() }

    fun loadUnit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            contentRepository.getTopic(topicId).fold(
                onSuccess = { response ->
                    val unitDto = response.units.find { it.id == unitId }
                    if (unitDto == null) { _uiState.value = _uiState.value.copy(isLoading = false, error = "Unidad no encontrada"); return@fold }

                    val blocks = unitDto.blocks.map { b -> ExerciseBlock(b.title, b.items.map { ExerciseItem(it.sentence, it.answer, it.hint) }) }
                    val totalItems = blocks.sumOf { it.items.size }

                    val unitTheory = unitDto.theory?.let { t ->
                        UnitTheory(
                            text = t.text,
                            sections = t.sections?.map { TheorySection(it.title, it.text, it.examples ?: emptyList()) } ?: emptyList(),
                            headers = t.table?.headers ?: emptyList(),
                            rows = t.table?.rows ?: emptyList(),
                            tips = t.tips?.map { Tip(it.emoji, it.text) } ?: emptyList(),
                            blocks = t.blocks?.map { com.appenglish.domain.model.TheoryBlock(it.title, it.html) } ?: emptyList()
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false, unitTitle = unitDto.title, unitExplanation = unitDto.explanation,
                        unitTheory = unitTheory, blocks = blocks, totalBlocks = blocks.size, totalItems = totalItems
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error") }
            )
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.isCorrect == true) return

        val currentItem = getCurrentItem() ?: return
        val userAnswer = option.trim().lowercase()
        val correctAnswer = currentItem.answer.trim().lowercase()
        val correct = userAnswer == correctAnswer

        if (correct) {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { SoundHelper.playCorrect(app) }
            val fullSentence = currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer)
            _uiState.value = state.copy(
                userInput = option, isCorrect = true,
                feedback = Feedback("¡Muy bien! ✅", true),
                score = state.score + 1, completedItems = state.completedItems + 1,
                showingAnswer = true
            )
            viewModelScope.launch {
                delay(600)
                _uiState.value = _uiState.value.copy(playingFullAudio = true, fullSentenceToPlay = fullSentence)
            }
        } else {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { SoundHelper.playIncorrect(app) }

            val wrong = WrongAnswer(
                blockIndex = state.currentBlockIndex,
                itemIndex = state.currentItemIndex,
                sentence = currentItem.sentence,
                givenAnswer = option,
                correctAnswer = currentItem.answer
            )

            _uiState.value = state.copy(
                userInput = option, isCorrect = false,
                feedback = Feedback("❌ La respuesta era: ${currentItem.answer}", false),
                wrongItems = state.wrongItems + wrong
            )

            viewModelScope.launch {
                delay(1200)
                autoAdvance()
            }
        }
    }

    private fun autoAdvance() {
        val state = _uiState.value
        val currentBlock = state.blocks.getOrNull(state.currentBlockIndex) ?: return

        if (state.currentItemIndex + 1 < currentBlock.items.size) {
            _uiState.value = state.copy(
                currentItemIndex = state.currentItemIndex + 1,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = ""
            )
        } else if (state.currentBlockIndex + 1 < state.blocks.size) {
            _uiState.value = state.copy(
                currentBlockIndex = state.currentBlockIndex + 1, currentItemIndex = 0,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = ""
            )
        } else {
            _uiState.value = state.copy(isFinished = true)
            saveProgress(completed = true)
        }
    }

    fun onFullAudioFinished() {
        _uiState.value = _uiState.value.copy(playingFullAudio = false, readyForNext = true)
    }

    fun nextItem() {
        val state = _uiState.value
        val currentBlock = state.blocks.getOrNull(state.currentBlockIndex) ?: return

        if (state.currentItemIndex + 1 < currentBlock.items.size) {
            _uiState.value = state.copy(
                currentItemIndex = state.currentItemIndex + 1,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = ""
            )
        } else if (state.currentBlockIndex + 1 < state.blocks.size) {
            _uiState.value = state.copy(
                currentBlockIndex = state.currentBlockIndex + 1, currentItemIndex = 0,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = ""
            )
        } else {
            _uiState.value = state.copy(isFinished = true)
            saveProgress(completed = true)
        }
    }

    fun startRetryWrongItems() {
        val wrongs = _uiState.value.wrongItems
        if (wrongs.isEmpty()) {
            _uiState.value = _uiState.value.copy(isFinished = true, retryMode = false)
            return
        }
        _uiState.value = _uiState.value.copy(retryMode = true, retryIndex = 0, isFinished = false)
    }

    fun retryWrongAnswer(selectedOption: String) {
        val state = _uiState.value
        val wrong = state.wrongItems.getOrNull(state.retryIndex) ?: return
        val correct = selectedOption.trim().lowercase() == wrong.correctAnswer.trim().lowercase()

        if (correct) {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { SoundHelper.playCorrect(app) }
            val remainingWrongs = state.wrongItems.toMutableList()
            remainingWrongs.removeAt(state.retryIndex)
            _uiState.value = state.copy(
                userInput = selectedOption, isCorrect = true,
                feedback = Feedback("¡Corregido! ✅", true),
                wrongItems = remainingWrongs,
                score = state.score + 1, completedItems = state.completedItems + 1
            )
            viewModelScope.launch {
                delay(800)
                val nextIdx = state.retryIndex
                if (nextIdx < remainingWrongs.size) {
                    _uiState.value = _uiState.value.copy(
                        retryIndex = nextIdx,
                        userInput = "", feedback = null, isCorrect = null,
                        showingAnswer = false, playingFullAudio = false,
                        readyForNext = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isFinished = true, retryMode = false)
                    saveProgress(completed = true)
                }
            }
        } else {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { SoundHelper.playIncorrect(app) }
            _uiState.value = state.copy(
                userInput = selectedOption, isCorrect = false,
                feedback = Feedback("❌ La respuesta era: ${wrong.correctAnswer}", false)
            )
            viewModelScope.launch {
                delay(1200)
                val nextIdx = state.retryIndex + 1
                if (nextIdx < state.wrongItems.size) {
                    _uiState.value = _uiState.value.copy(
                        retryIndex = nextIdx,
                        userInput = "", feedback = null, isCorrect = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isFinished = true, retryMode = false)
                    saveProgress(completed = true)
                }
            }
        }
    }

    fun getCurrentItem(): ExerciseItem? {
        val state = _uiState.value
        if (state.retryMode) {
            val wrong = state.wrongItems.getOrNull(state.retryIndex)
            return wrong?.let { ExerciseItem(it.sentence, it.correctAnswer, null) }
        }
        val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return null
        return block.items.getOrNull(state.currentItemIndex)
    }

    fun getCurrentBlockTitle(): String {
        val state = _uiState.value
        if (state.retryMode) return "Errores para revisar (${state.retryIndex + 1}/${state.wrongItems.size})"
        return state.blocks.getOrNull(state.currentBlockIndex)?.title ?: ""
    }

    fun getOptions(): List<String> {
        if (_uiState.value.retryMode) {
            val wrong = _uiState.value.wrongItems.getOrNull(_uiState.value.retryIndex)
            val correct = wrong?.correctAnswer ?: "am"
            val distractors = allAnswerOptions.filter { it != correct }.shuffled().take(2)
            return (listOf(correct) + distractors).shuffled()
        }
        return when (unitId) {
            "affirmative" -> listOf("am", "is", "are")
            "negative" -> listOf("am not", "isn't", "aren't")
            "interrogative" -> listOf("Am", "Is", "Are")
            "short-answers" -> {
                val current = getCurrentItem() ?: return listOf("Yes", "No")
                val distractors = allAnswerOptions.filter { it != current.answer }.take(2)
                (listOf(current.answer) + distractors).shuffled()
            }
            else -> listOf("am", "is", "are")
        }
    }

    private val allAnswerOptions = listOf(
        "I am", "I'm not", "he is", "he isn't", "she is", "she isn't",
        "it is", "it isn't", "you are", "you aren't", "we are", "we aren't",
        "they are", "they aren't", "am not", "isn't", "aren't", "Am", "Is", "Are"
    )

    private fun saveProgress(completed: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            progressRepository.saveProgress(
                topicId = topicId, unitId = unitId,
                completedItems = if (completed) state.totalItems else state.completedItems,
                score = state.score, totalItems = state.totalItems, completed = completed
            )
        }
    }
}
