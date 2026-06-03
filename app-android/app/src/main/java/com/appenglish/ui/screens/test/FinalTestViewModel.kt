package com.appenglish.ui.screens.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.TestQuestion
import com.appenglish.ui.components.SoundHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestUiState(
    val isLoading: Boolean = true,
    val questions: List<TestQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val userInput: String = "",
    val feedback: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val answered: Boolean = false,
    val isFinished: Boolean = false,
    val error: String? = null,
    val showingAnswer: Boolean = false,
    val playingFullAudio: Boolean = false,
    val readyForNext: Boolean = false,
    val fullSentenceToPlay: String = ""
)

@HiltViewModel
class FinalTestViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository
) : AndroidViewModel(application) {

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: "verb-to-be"

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    init {
        loadTest()
    }

    fun loadTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            contentRepository.getTest(topicId, 20).fold(
                onSuccess = { response ->
                    val questions = response.questions.map { q ->
                        TestQuestion(q.id, q.unitId, q.sentence, q.answer, q.hint)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, questions = questions)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el test"
                    )
                }
            )
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.answered) return  // Already answered this question

        val current = state.questions.getOrNull(state.currentIndex) ?: return
        val userAnswer = option.trim().lowercase()
        val correctAnswer = current.answer.trim().lowercase()
        val correct = userAnswer == correctAnswer

        if (correct) {
            val app = getApplication<android.app.Application>()
            viewModelScope.launch(Dispatchers.Main) {
                SoundHelper.playCorrect(app)
            }
            val fullSentence = current.sentence.replace(Regex("_{2,}"), current.answer)
            _uiState.value = state.copy(
                answered = true,
                isCorrect = true,
                userInput = option,
                score = state.score + 1,
                feedback = "¡Correcto! ✅",
                showingAnswer = true,
                readyForNext = true
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
                answered = true,
                userInput = option,
                isCorrect = false,
                feedback = "Incorrecto. La respuesta es: ${current.answer}",
                showingAnswer = true,
                readyForNext = true
            )
        }
    }

    fun onFullAudioFinished() {
        _uiState.value = _uiState.value.copy(playingFullAudio = false, readyForNext = true)
    }

    fun onAnswerAnimationFinished() {
        val state = _uiState.value
        val current = state.questions.getOrNull(state.currentIndex)
        val fullSentence = current?.sentence?.replace(Regex("_{2,}"), current.answer) ?: ""
        _uiState.value = state.copy(playingFullAudio = true, fullSentenceToPlay = fullSentence)
    }

    fun getFullSentencePlay(): String = _uiState.value.fullSentenceToPlay

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.answered || !state.readyForNext) return
        if (state.currentIndex + 1 < state.questions.size) {
            _uiState.value = state.copy(
                currentIndex = state.currentIndex + 1,
                userInput = "",
                feedback = null,
                isCorrect = null,
                answered = false,
                showingAnswer = false,
                playingFullAudio = false,
                readyForNext = false,
                fullSentenceToPlay = ""
            )
        } else {
            _uiState.value = state.copy(isFinished = true)
            saveTestScore()
        }
    }

    fun getCurrentQuestion(): TestQuestion? {
        return _uiState.value.questions.getOrNull(_uiState.value.currentIndex)
    }

    fun getOptions(): List<String> {
        val current = getCurrentQuestion() ?: return listOf("am", "is", "are")
        val unitId = current.unitId

        return when (unitId) {
            "affirmative" -> listOf("am", "is", "are")
            "negative" -> listOf("am not", "isn't", "aren't")
            "interrogative" -> listOf("Am", "Is", "Are")
            "short-answers" -> {
                val correct = current.answer
                val distractors = listOf(
                    "I am", "I'm not", "he is", "he isn't",
                    "she is", "she isn't", "it is", "it isn't",
                    "you are", "you aren't", "we are", "we aren't",
                    "they are", "they aren't"
                ).filter { it != correct }.take(2)
                (listOf(correct) + distractors).shuffled()
            }
            else -> listOf("am", "is", "are")
        }
    }

    private fun saveTestScore() {
        viewModelScope.launch {
            val score = _uiState.value.score
            progressRepository.saveTestScore(topicId, score)
            try {
                val allProgress = progressRepository.getTopicProgress(topicId)
                val entries = allProgress.map { p ->
                    com.appenglish.data.remote.dto.ProgressEntryDto(
                        topicId = p.topicId,
                        unitId = p.unitId,
                        completed = p.completed,
                        score = p.score,
                        totalItems = p.totalItems,
                        completedItems = p.completedItems,
                        testScore = if (p.topicId == topicId) score else p.testScore
                    )
                }
                val blockEntries = mutableListOf<com.appenglish.data.remote.dto.BlockProgressEntryDto>()
                for (p in allProgress) {
                    val blockProgressList = progressRepository.getAllBlockProgress(topicId, p.unitId)
                    for (bp in blockProgressList) {
                        blockEntries.add(
                            com.appenglish.data.remote.dto.BlockProgressEntryDto(
                                topicId = bp.topicId,
                                unitId = bp.unitId,
                                blockIndex = bp.blockIndex,
                                completed = bp.completed,
                                score = bp.score,
                                totalItems = bp.totalItems
                            )
                        )
                    }
                }
                progressRepository.syncProgress(entries = entries, blockEntries = blockEntries)
            } catch (_: Exception) {}
        }
    }
}
