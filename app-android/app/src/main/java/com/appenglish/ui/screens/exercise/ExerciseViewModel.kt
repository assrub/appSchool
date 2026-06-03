package com.appenglish.ui.screens.exercise

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appenglish.data.remote.dto.AnswerEntryDto
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val currentBlockTheory: BlockTheory? = null,
    val blockCompleted: Boolean = false,
    val blockScore: Int = 0,
    val blockTotalItems: Int = 0,
    val unitPassed: Boolean = false,
    val mcSelectedIndex: Int? = null,
    val reorderWords: List<String> = emptyList(),
    val reorderSlots: List<String?> = emptyList(),
    val matchedPairs: Int = 0,
    val tfAnswer: Boolean? = null,
    // New pedagogical metrics
    val attemptedItems: Set<String> = emptySet(),  // "blockIndex_itemIndex" keys
    val firstCorrectItems: Set<String> = emptySet(), // correct on first attempt
    val masteredItems: Set<String> = emptySet(),   // mastered (first correct OR retried correct)
    val sessionStartTime: Long = System.currentTimeMillis(),
    val timeSpentSeconds: Int = 0,
    // Cached options for current item
    val cachedOptions: List<String> = emptyList(),
    val cachedOptionsKey: String = ""
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

    private var syncDebounceJob: kotlinx.coroutines.Job? = null

    private val topicId: String = savedStateHandle.get<String>("topicId") ?: "verb-to-be"
    private val unitId: String = savedStateHandle.get<String>("unitId") ?: "affirmative"
    private val startBlockIndex: Int = savedStateHandle.get<Int>("startBlockIndex") ?: 0

    private val pendingAnswers = mutableListOf<AnswerEntryDto>()
    private val pendingAnswersMutex = Mutex()

    private val _uiState = MutableStateFlow(UnitExerciseUiState())
    val uiState: StateFlow<UnitExerciseUiState> = _uiState.asStateFlow()

    init {
        loadRemoteAndLocalProgress()
        loadUnit()
    }

    private fun loadRemoteAndLocalProgress() {
        viewModelScope.launch {
            try { progressRepository.loadRemoteProgress() } catch (_: Exception) {}
        }
    }

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
                        ExerciseBlock(b.title, b.items.map {
                            ExerciseItem(
                                it.sentence, it.answer, it.hint, it.itemType, it.inputMode,
                                it.answers, it.options, it.question, it.words, it.correctOrder,
                                it.audioUrl, it.pairs?.map { p -> com.appenglish.domain.model.ExercisePair(p.left, p.right) }, it.isCorrect
                            )
                        }, blockTheory)
                    }
                    val totalItems = blocks.sumOf { it.items.size }

                    // Load saved progress from local DB
                    val savedProgress = progressRepository.getProgress(topicId, unitId)
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

                    // Always start from the specified block
                    val blockIdx = startBlockIndex.coerceIn(0, blocks.lastIndex)
                    val itemIdx = 0

                    _uiState.value = _uiState.value.copy(
                        isLoading = false, unitTitle = unitDto.title, unitExplanation = unitDto.explanation,
                        soundCorrectUrl = unitDto.soundCorrectUrl, soundIncorrectUrl = unitDto.soundIncorrectUrl,
                        unitTheory = unitTheory, blocks = blocks, totalBlocks = blocks.size, totalItems = totalItems,
                        completedItems = 0, score = 0,
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
        handleAnswer(correct, currentItem, userAnswer)
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
            val wrongInBlock = state.wrongItems.count { it.blockIndex == state.currentBlockIndex }
            val blockScore = (currentBlock.items.size - wrongInBlock).coerceAtLeast(0)
            _uiState.value = state.copy(
                blockCompleted = true,
                blockScore = blockScore,
                blockTotalItems = currentBlock.items.size,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = "", showAcceptButton = false
            )
            saveProgress()
        } else {
            val percent = if (state.totalItems > 0) (state.score.toFloat() / state.totalItems) * 100 else 0f
            val passed = percent >= 70f
            _uiState.value = state.copy(isFinished = true, unitPassed = passed)
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
            val wrongInBlock = state.wrongItems.count { it.blockIndex == state.currentBlockIndex }
            val blockScore = (currentBlock.items.size - wrongInBlock).coerceAtLeast(0)
            _uiState.value = state.copy(
                blockCompleted = true,
                blockScore = blockScore,
                blockTotalItems = currentBlock.items.size,
                userInput = "", feedback = null, isCorrect = null,
                showingAnswer = false, playingFullAudio = false,
                readyForNext = false, fullSentenceToPlay = "",
                showAcceptButton = false
            )
            saveProgress()
        }
        else {
            val percent = if (state.totalItems > 0) (state.score.toFloat() / state.totalItems) * 100 else 0f
            val passed = percent >= 70f
            _uiState.value = state.copy(isFinished = true, unitPassed = passed)
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
            // Track mastered on retry
            val itemKey = "${wrong.blockIndex}_${wrong.itemIndex}"
            val newMastered = state.masteredItems + itemKey
            _uiState.value = state.copy(
                userInput = selectedOption, isCorrect = true,
                showingAnswer = true,
                wrongItems = remainingWrongs, score = state.score + 1,
                masteredItems = newMastered
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

    fun continueToNextBlock() {
        val state = _uiState.value
        _uiState.value = state.copy(
            blockCompleted = false,
            currentBlockIndex = state.currentBlockIndex + 1,
            currentItemIndex = 0,
            userInput = "", feedback = null, isCorrect = null,
            showingAnswer = false, playingFullAudio = false,
            readyForNext = false, fullSentenceToPlay = "",
            showAcceptButton = false
        )
        updateCurrentBlockTheory()
    }

    fun redoUnit() {
        viewModelScope.launch {
            // Reset all block progress on backend
            for (i in _uiState.value.blocks.indices) {
                progressRepository.resetBlockProgress(
                    topicId = topicId, unitId = unitId,
                    blockIndex = i, totalItems = _uiState.value.blocks[i].items.size
                )
            }
            // Reset unit progress
            progressRepository.resetUnitProgress(topicId, unitId)
            _uiState.value = UnitExerciseUiState(
                unitTitle = _uiState.value.unitTitle,
                unitExplanation = _uiState.value.unitExplanation,
                unitTheory = _uiState.value.unitTheory,
                soundCorrectUrl = _uiState.value.soundCorrectUrl,
                soundIncorrectUrl = _uiState.value.soundIncorrectUrl,
                blocks = _uiState.value.blocks,
                totalBlocks = _uiState.value.totalBlocks,
                totalItems = _uiState.value.totalItems,
                isLoading = false
            )
            updateCurrentBlockTheory()
        }
    }

    fun getCurrentItem(): ExerciseItem? {
        val state = _uiState.value
        if (state.retryMode) {
            val wrong = state.wrongItems.getOrNull(state.retryIndex) ?: return null
            val originalItem = state.blocks.getOrNull(wrong.blockIndex)?.items?.getOrNull(wrong.itemIndex)
            return originalItem?.copy(sentence = wrong.sentence, answer = wrong.correctAnswer)
                ?: ExerciseItem(wrong.sentence, wrong.correctAnswer, null)
        }
        val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return null
        return block.items.getOrNull(state.currentItemIndex)
    }

    fun getItemType(): String = getCurrentItem()?.itemType ?: "fill-blank"

    fun selectMcOption(index: Int) {
        val state = _uiState.value
        if (state.isCorrect != null) return
        val item = getCurrentItem() ?: return
        val options = item.options?.filter { it.isNotBlank() } ?: emptyList()
        val selected = options.getOrNull(index) ?: return
        _uiState.value = state.copy(mcSelectedIndex = index)
        checkAnswer(selected)
    }

    fun initReorder() {
        val item = getCurrentItem() ?: return
        val words = item.words?.shuffled() ?: emptyList()
        _uiState.value = _uiState.value.copy(reorderWords = words, reorderSlots = List(words.size) { null })
    }

    fun selectReorderWord(word: String) {
        val state = _uiState.value
        val slots = state.reorderSlots.toMutableList()
        val firstEmpty = slots.indexOfFirst { it == null }
        if (firstEmpty >= 0) {
            slots[firstEmpty] = word
            _uiState.value = state.copy(reorderSlots = slots, reorderWords = state.reorderWords - word)
        }
    }

    fun removeReorderWord(index: Int) {
        val state = _uiState.value
        val slots = state.reorderSlots.toMutableList()
        val removed = slots[index] ?: return
        slots[index] = null
        _uiState.value = state.copy(reorderSlots = slots, reorderWords = state.reorderWords + removed)
    }

    fun checkReorderAnswer() {
        val item = getCurrentItem() ?: return
        val userOrder: List<String> = _uiState.value.reorderSlots.filterNotNull()
        val correctOrder: List<String> = item.correctOrder ?: emptyList()
        val correct = userOrder == correctOrder
        handleAnswer(correct, item, userOrder.joinToString(" "))
    }

    fun selectTfAnswer(answer: Boolean) {
        val state = _uiState.value
        if (state.isCorrect != null) return
        val item = getCurrentItem() ?: return
        val actualCorrect = item.isCorrect ?: (item.answer.isBlank())
        val correct = answer == actualCorrect
        _uiState.value = state.copy(tfAnswer = answer)
        handleAnswer(correct, item, if (answer) "Verdadero" else "Falso")
    }

    fun checkMatchingPair(left: String, right: String) {
        val item = getCurrentItem() ?: return
        val pairs = item.pairs ?: return
        val isMatch = pairs.any { (it.left == left && it.right == right) || (it.left == right && it.right == left) }
        if (isMatch) {
            val newCount = _uiState.value.matchedPairs + 1
            _uiState.value = _uiState.value.copy(matchedPairs = newCount)
            if (newCount >= pairs.size) handleAnswer(true, item, "")
        } else handleAnswer(false, item, "$left ↔ $right")
    }

    fun resetAllMatched() { _uiState.value = _uiState.value.copy(matchedPairs = 0) }

    private fun handleAnswer(correct: Boolean, item: ExerciseItem, userAnswer: String) {
        val state = _uiState.value
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.Main) { playFeedbackSound(app, if (correct) state.soundCorrectUrl else state.soundIncorrectUrl, correct) }

        viewModelScope.launch {
            pendingAnswersMutex.withLock {
                pendingAnswers.add(AnswerEntryDto(
                    topicId = topicId,
                    unitId = unitId,
                    givenAnswer = userAnswer,
                    correctAnswer = item.answer,
                    isCorrect = correct
                ))
            }
        }

        // Track pedagogical metrics
        val itemKey = "${state.currentBlockIndex}_${state.currentItemIndex}"
        val newAttempted = state.attemptedItems + itemKey
        val newFirstCorrect = if (correct) state.firstCorrectItems + itemKey else state.firstCorrectItems
        val newMastered = if (correct) state.masteredItems + itemKey else state.masteredItems
        val timeSpent = ((System.currentTimeMillis() - state.sessionStartTime) / 1000).toInt()

        val newCompleted = state.completedItems + 1
        if (correct) {
            val newScore = state.score + 1
            _uiState.value = state.copy(
                userInput = userAnswer, isCorrect = true,
                feedback = Feedback("¡Muy bien! ✅", true),
                score = newScore, completedItems = newCompleted, showingAnswer = true,
                attemptedItems = newAttempted, firstCorrectItems = newFirstCorrect,
                masteredItems = newMastered, timeSpentSeconds = timeSpent
            )
            saveProgressLocal()
            triggerDebouncedSync()
            if (item.itemType == "fill-blank") {
                val fullSentence = item.sentence.replace(Regex("_{2,}"), item.answer)
                viewModelScope.launch { delay(600); _uiState.value = _uiState.value.copy(playingFullAudio = true, fullSentenceToPlay = fullSentence) }
            }
        } else {
            val wrong = WrongAnswer(blockIndex = state.currentBlockIndex, itemIndex = state.currentItemIndex, sentence = item.sentence, givenAnswer = userAnswer, correctAnswer = item.answer)
            _uiState.value = state.copy(
                isCorrect = false, feedback = Feedback("❌ Incorrecto", false),
                showAcceptButton = true, wrongItems = state.wrongItems + wrong,
                completedItems = newCompleted, userInput = userAnswer,
                attemptedItems = newAttempted, timeSpentSeconds = timeSpent
            )
            saveProgressLocal()
            triggerDebouncedSync()
        }
    }

    fun getCurrentBlockTitle(): String {
        val state = _uiState.value
        if (state.retryMode) return "Errores (${state.retryIndex + 1}/${state.wrongItems.size})"
        val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return ""
        return block.title
    }

    fun getBlockCompletedItems(): Int {
        val state = _uiState.value
        var before = 0
        for (i in 0 until state.currentBlockIndex) {
            before += state.blocks.getOrNull(i)?.items?.size ?: 0
        }
        val inBlock = (state.completedItems - before).coerceIn(0, state.blocks.getOrNull(state.currentBlockIndex)?.items?.size ?: 0)
        return inBlock
    }

    fun getBlockTotalItems(): Int {
        val state = _uiState.value
        return state.blocks.getOrNull(state.currentBlockIndex)?.items?.size ?: 0
    }

    fun getOptions(): List<String> {
        val state = _uiState.value
        val item = getCurrentItem() ?: return listOf("am", "is", "are")

        // Create a cache key based on current item
        val cacheKey = "${state.currentBlockIndex}_${state.currentItemIndex}_${state.retryMode}_${state.retryIndex}"

        // Return cached options if available and matching
        if (state.cachedOptionsKey == cacheKey && state.cachedOptions.isNotEmpty()) {
            return state.cachedOptions
        }

        // Generate new options
        val options = if (state.retryMode) {
            val wrong = state.wrongItems.getOrNull(state.retryIndex)
            generateOptions(wrong?.correctAnswer ?: "am", state.blocks.firstOrNull()?.items?.firstOrNull())
        } else {
            if (item.options != null) {
                val filtered = item.options.filter { it.isNotBlank() }
                if (filtered.isNotEmpty()) filtered else generateOptions(item.answer, item)
            } else {
                generateOptions(item.answer, item)
            }
        }

        // Cache the options
        _uiState.value = state.copy(cachedOptions = options, cachedOptionsKey = cacheKey)
        return options
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

    private fun calculateMetrics(): PedagogicalMetrics {
        val state = _uiState.value
        val totalItems = state.totalItems
        val attempted = state.attemptedItems.size
        val firstCorrect = state.firstCorrectItems.size
        val mastered = state.masteredItems.size
        val timeSpent = ((System.currentTimeMillis() - state.sessionStartTime) / 1000).toInt()

        val accuracy = if (attempted > 0) (firstCorrect.toFloat() / attempted * 100) else 0f
        val mastery = if (totalItems > 0) (mastered.toFloat() / totalItems * 100) else 0f

        val status = when {
            attempted == 0 -> "not_started"
            mastered >= totalItems && totalItems > 0 -> "mastered"
            state.isFinished -> "completed"
            else -> "in_progress"
        }

        return PedagogicalMetrics(
            accuracy = accuracy,
            mastery = mastery,
            status = status,
            itemsAttempted = attempted,
            itemsMastered = mastered,
            itemsCorrectFirst = firstCorrect,
            timeSpentSeconds = timeSpent
        )
    }

    private data class PedagogicalMetrics(
        val accuracy: Float,
        val mastery: Float,
        val status: String,
        val itemsAttempted: Int,
        val itemsMastered: Int,
        val itemsCorrectFirst: Int,
        val timeSpentSeconds: Int
    )

    private fun triggerDebouncedSync() {
        syncDebounceJob?.cancel()
        syncDebounceJob = viewModelScope.launch {
            delay(3000)
            val s = _uiState.value
            val items = s.completedItems
            val metrics = calculateMetrics()
            try {
                val existing = progressRepository.getProgress(topicId, unitId)
                progressRepository.syncProgress(
                    entries = listOf(
                        com.appenglish.data.remote.dto.ProgressEntryDto(
                            topicId = topicId,
                            unitId = unitId,
                            completed = false,
                            score = s.score,
                            totalItems = s.totalItems,
                            completedItems = items,
                            testScore = existing?.testScore,
                            accuracy = metrics.accuracy,
                            mastery = metrics.mastery,
                            status = metrics.status,
                            itemsAttempted = metrics.itemsAttempted,
                            itemsMastered = metrics.itemsMastered,
                            itemsCorrectFirst = metrics.itemsCorrectFirst,
                            timeSpentSeconds = metrics.timeSpentSeconds
                        )
                    )
                )
            } catch (_: Exception) {}
            val answersToSend = pendingAnswersMutex.withLock {
                val copy = pendingAnswers.toList()
                pendingAnswers.clear()
                copy
            }
            if (answersToSend.isNotEmpty()) {
                try {
                    progressRepository.recordAnswers(answersToSend)
                } catch (_: Exception) {}
            }
        }
    }

    private fun saveProgressLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val metrics = calculateMetrics()
            progressRepository.saveProgress(
                topicId = topicId, unitId = unitId,
                completedItems = state.completedItems,
                score = state.score, totalItems = state.totalItems, completed = false
            )
            // Save pedagogical metrics
            progressRepository.saveMetrics(
                topicId = topicId, unitId = unitId,
                accuracy = metrics.accuracy,
                mastery = metrics.mastery,
                status = metrics.status,
                itemsAttempted = metrics.itemsAttempted,
                itemsMastered = metrics.itemsMastered,
                itemsCorrectFirst = metrics.itemsCorrectFirst,
                timeSpentSeconds = metrics.timeSpentSeconds
            )
            var itemsBeforeBlock = 0
            for ((blockIdx, block) in state.blocks.withIndex()) {
                if (blockIdx > state.currentBlockIndex) break
                val completedInBlock = if (blockIdx < state.currentBlockIndex) {
                    block.items.size
                } else {
                    (state.completedItems - itemsBeforeBlock).coerceIn(0, block.items.size)
                }
                val wrongInBlock = state.wrongItems.count { it.blockIndex == blockIdx }
                val blockScore = (completedInBlock - wrongInBlock).coerceAtLeast(0)
                val blockCompleted = blockIdx < state.currentBlockIndex || (blockIdx == state.currentBlockIndex && completedInBlock == block.items.size)
                progressRepository.saveBlockProgress(
                    topicId = topicId, unitId = unitId,
                    blockIndex = blockIdx, score = blockScore,
                    totalItems = block.items.size, completed = blockCompleted
                )
                itemsBeforeBlock += block.items.size
            }
        }
    }

    private fun saveProgress(completed: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val items = if (completed) state.totalItems else state.completedItems
            val metrics = calculateMetrics()
            val blockEntries = mutableListOf<com.appenglish.data.remote.dto.BlockProgressEntryDto>()
            var itemsBeforeBlock = 0
            for ((blockIdx, block) in state.blocks.withIndex()) {
                if (blockIdx > state.currentBlockIndex) break
                val completedInBlock = if (blockIdx < state.currentBlockIndex) {
                    block.items.size
                } else {
                    (state.completedItems - itemsBeforeBlock).coerceIn(0, block.items.size)
                }
                val wrongInBlock = state.wrongItems.count { it.blockIndex == blockIdx }
                val blockScore = (completedInBlock - wrongInBlock).coerceAtLeast(0)
                val blockCompleted = blockIdx < state.currentBlockIndex || (blockIdx == state.currentBlockIndex && completedInBlock == block.items.size)
                blockEntries.add(
                    com.appenglish.data.remote.dto.BlockProgressEntryDto(
                        topicId = topicId,
                        unitId = unitId,
                        blockIndex = blockIdx,
                        completed = blockCompleted,
                        score = blockScore,
                        totalItems = block.items.size,
                        completedAt = if (blockCompleted) java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()) else null
                    )
                )
            }

            try {
                val existingProgress = progressRepository.getProgress(topicId, unitId)
                progressRepository.syncProgress(
                    entries = listOf(
                        com.appenglish.data.remote.dto.ProgressEntryDto(
                            topicId = topicId,
                            unitId = unitId,
                            completed = completed,
                            score = state.score,
                            totalItems = state.totalItems,
                            completedItems = items,
                            testScore = existingProgress?.testScore,
                            completedAt = if (completed) java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()) else null,
                            accuracy = metrics.accuracy,
                            mastery = metrics.mastery,
                            status = if (completed && metrics.status == "in_progress") "completed" else metrics.status,
                            itemsAttempted = metrics.itemsAttempted,
                            itemsMastered = metrics.itemsMastered,
                            itemsCorrectFirst = metrics.itemsCorrectFirst,
                            timeSpentSeconds = metrics.timeSpentSeconds
                        )
                    ),
                    blockEntries = blockEntries
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
                val client = com.appenglish.util.HttpClientFactory.getInstance()
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
