package com.appenglish.ui.screens.exercise

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appenglish.data.repository.ContentRepository
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.domain.model.UnitTheory
import com.appenglish.domain.model.BlockTheory
import com.appenglish.domain.model.TheorySection
import com.appenglish.domain.model.TheoryBlock
import com.appenglish.domain.model.Tip
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.ExerciseItem
import com.appenglish.ui.components.SoundHelper
import com.appenglish.util.ApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
    val soundCorrectUrl: String? = null,
    val soundIncorrectUrl: String? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
    val currentBlockIndex: Int = 0,
    val currentItemIndex: Int = 0,
    val userInput: String = "",
    val feedback: Feedback? = null,
    val isCorrect: Boolean? = null,
    val showAcceptButton: Boolean = false,
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
    val retryIndex: Int = 0,
    val selectedTab: Int = 0,
    val currentBlockTheory: BlockTheory? = null
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
            try {
            contentRepository.getTopic(topicId).fold(
                onSuccess = { response ->
                    val unitDto = response.units.find { it.id == unitId }
                    if (unitDto == null) { _uiState.value = _uiState.value.copy(isLoading = false, error = "Unidad no encontrada"); return@fold }

                    val blocks = unitDto.blocks.map { b ->
                        val blockTheory = b.theory?.let { bt ->
                            BlockTheory(
                                text = bt.text,
                                sections = bt.sections?.map { TheorySection(it.title, it.text, it.examples ?: emptyList()) } ?: emptyList(),
                                headers = bt.table?.headers ?: emptyList(),
                                rows = bt.table?.rows ?: emptyList(),
                                tips = bt.tips?.map { Tip(it.emoji, it.text) } ?: emptyList(),
                                blocks = bt.blocks?.map { TheoryBlock(it.title, it.html) } ?: emptyList()
                            )
                        }
                        ExerciseBlock(b.title, b.items.map { ExerciseItem(it.sentence, it.answer, it.hint, it.itemType, it.inputMode, it.answers, it.options) }, blockTheory)
                    }
                    val totalItems = blocks.sumOf { it.items.size }

                    // Load saved progress from local DB
                    val savedProgress = progressRepository.getProgress(topicId, unitId)
                    val completedItems = savedProgress?.completedItems ?: 0
                    val savedScore = savedProgress?.score ?: 0

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

                    // Calculate starting position
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
                    if (completedItems >= totalItems && totalItems > 0) {
                        blockIdx = blocks.lastIndex
                        itemIdx = blocks.last().items.lastIndex
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false, unitTitle = unitDto.title, unitExplanation = unitDto.explanation,
                        soundCorrectUrl = unitDto.soundCorrectUrl, soundIncorrectUrl = unitDto.soundIncorrectUrl,
                        unitTheory = unitTheory, blocks = blocks, totalBlocks = blocks.size, totalItems = totalItems,
                        completedItems = completedItems, score = savedScore,
                        currentBlockIndex = blockIdx, currentItemIndex = itemIdx
                    )
                    updateCurrentBlockTheory()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error") }
            )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Error: ${e.message}")
            }
        }
    }

    fun selectOption(option: String) {
        checkAnswer(option)
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    private fun updateCurrentBlockTheory() {
        val state = _uiState.value
        val theory = if (state.retryMode) null else state.blocks.getOrNull(state.currentBlockIndex)?.theory
        _uiState.value = state.copy(currentBlockTheory = theory)
    }

    fun onInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(userInput = input, feedback = null, isCorrect = null)
    }

    fun checkTextAnswer() {
        val state = _uiState.value
        if (state.userInput.isBlank()) return
        checkAnswer(state.userInput)
    }

    private fun checkAnswer(userAnswer: String) {
        val state = _uiState.value
        if (state.isCorrect == true || state.showAcceptButton) return

        val currentItem = getCurrentItem() ?: return
        val correct = isAnswerCorrect(currentItem, userAnswer)

        if (correct) {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { playFeedbackSound(app, state.soundCorrectUrl, isCorrect = true) }
            val fullSentence = currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer)
            val newScore = state.score + 1
            val newCompleted = state.completedItems + 1
            _uiState.value = state.copy(
                userInput = userAnswer, isCorrect = true,
                feedback = Feedback("¡Muy bien! ✅", true),
                score = newScore, completedItems = newCompleted,
                showingAnswer = true
            )
            saveProgress()
            viewModelScope.launch {
                delay(600)
                _uiState.value = _uiState.value.copy(playingFullAudio = true, fullSentenceToPlay = fullSentence)
            }
        } else {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { playFeedbackSound(app, state.soundIncorrectUrl, isCorrect = false) }

            val wrong = WrongAnswer(
                blockIndex = state.currentBlockIndex, itemIndex = state.currentItemIndex,
                sentence = currentItem.sentence, givenAnswer = userAnswer, correctAnswer = currentItem.answer
            )

            _uiState.value = state.copy(
                userInput = userAnswer, isCorrect = false,
                feedback = Feedback("❌ Incorrecto", false),
                showAcceptButton = true,
                wrongItems = state.wrongItems + wrong
            )
        }
    }

    fun onAcceptClick() {
        val state = _uiState.value
        _uiState.value = state.copy(showAcceptButton = false)
        autoAdvance()
    }

    private fun autoAdvance() {
        val state = _uiState.value
        val currentBlock = state.blocks.getOrNull(state.currentBlockIndex) ?: return

        if (state.currentItemIndex + 1 < currentBlock.items.size) {
            _uiState.value = state.copy(
                currentItemIndex = state.currentItemIndex + 1,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = "", showAcceptButton = false
            )
        } else if (state.currentBlockIndex + 1 < state.blocks.size) {
            _uiState.value = state.copy(
                currentBlockIndex = state.currentBlockIndex + 1, currentItemIndex = 0,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = "", showAcceptButton = false
            )
        } else {
            _uiState.value = state.copy(isFinished = true)
            saveProgress(completed = true)
        }
        updateCurrentBlockTheory()
    }

    private fun isAnswerCorrect(item: ExerciseItem, userOption: String): Boolean {
        val answer = userOption.trim().lowercase()
        if (answer == item.answer.trim().lowercase()) return true
        item.answers?.forEach { if (answer == it.trim().lowercase()) return true }
        return false
    }

    fun onFullAudioFinished() { _uiState.value = _uiState.value.copy(playingFullAudio = false, readyForNext = true) }

    fun nextItem() {
        val state = _uiState.value
        val currentBlock = state.blocks.getOrNull(state.currentBlockIndex) ?: return

        if (state.currentItemIndex + 1 < currentBlock.items.size) {
            _uiState.value = state.copy(
                currentItemIndex = state.currentItemIndex + 1,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = "",
                showAcceptButton = false
            )
        }
        else if (state.currentBlockIndex + 1 < state.blocks.size) {
            _uiState.value = state.copy(
                currentBlockIndex = state.currentBlockIndex + 1,
                currentItemIndex = 0,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = "",
                showAcceptButton = false
            )
        }
        else {
            _uiState.value = state.copy(isFinished = true)
            saveProgress(completed = true)
        }
        updateCurrentBlockTheory()
    }

    fun startRetryWrongItems() {
        val wrongs = _uiState.value.wrongItems
        if (wrongs.isEmpty()) { _uiState.value = _uiState.value.copy(isFinished = true, retryMode = false, currentBlockTheory = null); return }
        _uiState.value = _uiState.value.copy(retryMode = true, retryIndex = 0, isFinished = false, currentBlockTheory = null)
    }

    fun retryWrongAnswer(selectedOption: String) {
        val state = _uiState.value
        if (state.showAcceptButton) return
        val wrong = state.wrongItems.getOrNull(state.retryIndex) ?: return
        val correct = selectedOption.trim().lowercase() == wrong.correctAnswer.trim().lowercase()

        if (correct) {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { playFeedbackSound(app, state.soundCorrectUrl, isCorrect = true) }
            val remainingWrongs = state.wrongItems.toMutableList()
            remainingWrongs.removeAt(state.retryIndex)
            _uiState.value = state.copy(
                userInput = selectedOption, isCorrect = true,
                showingAnswer = true,
                wrongItems = remainingWrongs, score = state.score + 1, completedItems = state.completedItems + 1
            )
            // Don't auto-advance — wait for user to tap modal button
        } else {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.Main) { playFeedbackSound(app, state.soundIncorrectUrl, isCorrect = false) }
            _uiState.value = state.copy(
                userInput = selectedOption, isCorrect = false,
                showAcceptButton = true
            )
        }
    }

    fun retryNextAfterCorrect() {
        val state = _uiState.value
        val nextIdx = state.retryIndex
        if (nextIdx < state.wrongItems.size) {
            _uiState.value = state.copy(
                retryIndex = nextIdx,
                userInput = "", isCorrect = null, showingAnswer = false,
                playingFullAudio = false, readyForNext = false, showAcceptButton = false,
                currentBlockTheory = null
            )
        } else {
            _uiState.value = state.copy(isFinished = true, retryMode = false)
            saveProgress(completed = true)
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
        if (state.retryMode) return "Errores (${state.retryIndex + 1}/${state.wrongItems.size})"
        return state.blocks.getOrNull(state.currentBlockIndex)?.title ?: ""
    }

    fun getOptions(): List<String> {
        if (_uiState.value.retryMode) {
            val wrong = _uiState.value.wrongItems.getOrNull(_uiState.value.retryIndex)
            return generateOptions(wrong?.correctAnswer ?: "am", _uiState.value.blocks.firstOrNull()?.items?.firstOrNull())
        }
        val item = getCurrentItem() ?: return listOf("am", "is", "are")
        if (item.options != null) {
            val filtered = item.options.filter { it.isNotBlank() }
            if (filtered.isNotEmpty()) return filtered
        }
        return generateOptions(item.answer, item)
    }

    private fun generateOptions(answer: String, item: ExerciseItem?): List<String> {
        val opts = if (item?.options != null && item.options.isNotEmpty()) item.options
        else {
            when (unitId) {
                "affirmative" -> listOf("am", "is", "are")
                "negative" -> listOf("am not", "isn't", "aren't")
                "interrogative" -> listOf("Am", "Is", "Are")
                "short-answers" -> {
                    val distractors = allAnswerOptions.filter { it != answer }.shuffled().take(2)
                    (listOf(answer) + distractors).shuffled()
                }
                else -> listOf("am", "is", "are")
            }
        }
        return if (opts.contains(answer)) opts else (opts + answer)
    }

    private val allAnswerOptions = listOf("I am","I'm not","he is","he isn't","she is","she isn't","it is","it isn't","you are","you aren't","we are","we aren't","they are","they aren't","am not","isn't","aren't","Am","Is","Are")

    private fun saveProgress(completed: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val items = if (completed) state.totalItems else state.completedItems
            progressRepository.saveProgress(
                topicId = topicId, unitId = unitId,
                completedItems = items,
                score = state.score, totalItems = state.totalItems, completed = completed
            )

            // Save per-block progress
            val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return@launch
            val blockScore = block.items.count { item ->
                state.wrongItems.none { it.sentence == item.sentence && it.correctAnswer == item.answer }
            }
            val blockCompleted = blockScore == block.items.size
            progressRepository.saveBlockProgress(
                topicId = topicId, unitId = unitId,
                blockIndex = state.currentBlockIndex,
                score = blockScore,
                totalItems = block.items.size,
                completed = blockCompleted
            )
            // Sync to backend
            try {
                progressRepository.syncProgress(
                    listOf(
                        com.appenglish.data.remote.dto.ProgressEntryDto(
                            topicId = topicId,
                            unitId = unitId,
                            completed = completed,
                            score = state.score,
                            totalItems = state.totalItems,
                            completedItems = items
                        )
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private suspend fun playFeedbackSound(app: Application, url: String?, isCorrect: Boolean) {
        if (url.isNullOrBlank()) {
            if (isCorrect) SoundHelper.playCorrect(app)
            else SoundHelper.playIncorrect(app)
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val fullUrl = if (url.startsWith("http")) url else "${ApiConfig.BASE_URL.removeSuffix("/api/v1/")}$url"
                val client = OkHttpClient()
                val request = Request.Builder().url(fullUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) { if (isCorrect) SoundHelper.playCorrect(app) else SoundHelper.playIncorrect(app); return@withContext }
                val bytes = response.body?.bytes() ?: return@withContext
                val tempFile = File(app.cacheDir, "sound_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { it.write(bytes) }
                withContext(Dispatchers.Main) {
                    MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener { release() }
                    }
                }
            } catch (_: Exception) { if (isCorrect) SoundHelper.playCorrect(app) else SoundHelper.playIncorrect(app) }
        }
    }
}
