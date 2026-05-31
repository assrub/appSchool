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
    val fullSentenceToPlay: String = ""
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

    init {
        loadUnit()
    }

    fun loadUnit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            contentRepository.getTopic(topicId).fold(
                onSuccess = { response ->
                    val unitDto = response.units.find { it.id == unitId }
                    if (unitDto == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Unidad no encontrada")
                        return@fold
                    }

                    val localProgress = progressRepository.getTopicProgress(topicId)
                    val localUnit = localProgress.find { it.unitId == unitId }
                    val blocks = unitDto.blocks.map { blockDto ->
                        ExerciseBlock(
                            title = blockDto.title,
                            items = blockDto.items.map { itemDto ->
                                ExerciseItem(itemDto.sentence, itemDto.answer, itemDto.hint)
                            }
                        )
                    }
                    val totalItems = blocks.sumOf { it.items.size }
                    val completedItems = localUnit?.completedItems ?: unitDto.progress?.completedItems ?: 0

                    val unitTheory = unitDto.theory?.let { t ->
                        UnitTheory(
                            text = t.text,
                            sections = t.sections?.map { s ->
                                TheorySection(s.title, s.text, s.examples ?: emptyList())
                            } ?: emptyList(),
                            headers = t.table?.headers ?: emptyList(),
                            rows = t.table?.rows ?: emptyList(),
                            tips = t.tips?.map { Tip(it.emoji, it.text) } ?: emptyList()
                        )
                    }

                    var remaining = completedItems
                    var blockIdx = 0
                    var itemIdx = 0
                    for ((i, block) in blocks.withIndex()) {
                        if (remaining >= block.items.size) {
                            remaining -= block.items.size
                        } else {
                            blockIdx = i
                            itemIdx = remaining
                            break
                        }
                    }
                    if (completedItems >= totalItems) {
                        blockIdx = blocks.lastIndex
                        itemIdx = blocks.last().items.lastIndex
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        unitTitle = unitDto.title,
                        unitExplanation = unitDto.explanation,
                        unitTheory = unitTheory,
                        blocks = blocks,
                        currentBlockIndex = blockIdx,
                        currentItemIndex = itemIdx,
                        totalBlocks = blocks.size,
                        totalItems = totalItems,
                        completedItems = completedItems
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar"
                    )
                }
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
            val app = getApplication<android.app.Application>()
            viewModelScope.launch(Dispatchers.Main) {
                SoundHelper.playCorrect(app)
            }
            val fullSentence = currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer)
            _uiState.value = state.copy(
                userInput = option,
                isCorrect = true,
                feedback = Feedback("¡Muy bien! ✅", true),
                score = state.score + 1,
                completedItems = state.completedItems + 1,
                showingAnswer = true
            )
            viewModelScope.launch {
                delay(600)
                _uiState.value = _uiState.value.copy(
                    playingFullAudio = true,
                    fullSentenceToPlay = fullSentence
                )
            }
        } else {
            val app = getApplication<android.app.Application>()
            viewModelScope.launch(Dispatchers.Main) {
                SoundHelper.playIncorrect(app)
            }
            _uiState.value = state.copy(
                userInput = option,
                isCorrect = false,
                feedback = Feedback("Intentá de nuevo ❌", false)
            )
            viewModelScope.launch {
                delay(1200)
                _uiState.value = _uiState.value.copy(
                    userInput = "",
                    isCorrect = null,
                    feedback = null
                )
            }
        }
    }

    fun onFullAudioFinished() {
        _uiState.value = _uiState.value.copy(
            playingFullAudio = false,
            readyForNext = true
        )
    }

    fun onAnswerAnimationFinished() {
        val state = _uiState.value
        val currentItem = getCurrentItem()
        val fullSentence = currentItem?.sentence?.replace(Regex("_{2,}"), currentItem.answer) ?: ""

        _uiState.value = state.copy(
            playingFullAudio = true,
            fullSentenceToPlay = fullSentence
        )
    }

    fun getFullSentencePlay(): String {
        return _uiState.value.fullSentenceToPlay
    }

    fun nextItem() {
        val state = _uiState.value
        val currentBlock = _uiState.value.blocks.getOrNull(state.currentBlockIndex) ?: return

        if (state.currentItemIndex + 1 < currentBlock.items.size) {
            _uiState.value = state.copy(
                currentItemIndex = state.currentItemIndex + 1,
                userInput = "",
                feedback = null,
                isCorrect = null,
                showingAnswer = false,
                playingFullAudio = false,
                readyForNext = false,
                fullSentenceToPlay = ""
            )
        } else if (state.currentBlockIndex + 1 < _uiState.value.blocks.size) {
            _uiState.value = state.copy(
                currentBlockIndex = state.currentBlockIndex + 1,
                currentItemIndex = 0,
                userInput = "",
                feedback = null,
                isCorrect = null,
                showingAnswer = false,
                playingFullAudio = false,
                readyForNext = false,
                fullSentenceToPlay = ""
            )
        } else {
            _uiState.value = state.copy(isFinished = true)
            saveProgress(completed = true)
        }
    }

    fun getCurrentItem(): ExerciseItem? {
        val block = _uiState.value.blocks.getOrNull(_uiState.value.currentBlockIndex) ?: return null
        return block.items.getOrNull(_uiState.value.currentItemIndex)
    }

    fun getCurrentBlockTitle(): String {
        return _uiState.value.blocks.getOrNull(_uiState.value.currentBlockIndex)?.title ?: ""
    }

    fun getOptions(): List<String> {
        return when (unitId) {
            "affirmative" -> listOf("am", "is", "are")
            "negative" -> listOf("am not", "isn't", "aren't")
            "interrogative" -> listOf("Am", "Is", "Are")
            "short-answers" -> {
                val current = getCurrentItem() ?: return listOf("Yes", "No")
                val correct = current.answer
                val distractors = shortAnswerDistractors(correct)
                (listOf(correct) + distractors).shuffled()
            }
            else -> listOf("am", "is", "are")
        }
    }

    private fun shortAnswerDistractors(correct: String): List<String> {
        val all = listOf(
            "I am", "I'm not", "he is", "he isn't",
            "she is", "she isn't", "it is", "it isn't",
            "you are", "you aren't", "we are", "we aren't",
            "they are", "they aren't"
        )
        return all.filter { it != correct }.take(2)
    }

    private fun saveProgress(completed: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            progressRepository.saveProgress(
                topicId = topicId,
                unitId = unitId,
                completedItems = if (completed) state.totalItems else state.completedItems,
                score = state.score,
                totalItems = state.totalItems,
                completed = completed
            )
        }
    }
}
