package com.appenglish.ui.screens.exercise

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.PaddingValues
import com.appenglish.domain.model.BlockTheory
import com.appenglish.ui.components.TheoryHtmlView
import com.appenglish.ui.components.TtsButton
import com.appenglish.ui.components.TranslateableText
import com.appenglish.ui.components.translateWord
import com.appenglish.ui.theme.CorrectBackground
import com.appenglish.ui.theme.CorrectGreen
import com.appenglish.ui.theme.ErrorRed
import com.appenglish.ui.theme.InfoBlue
import com.appenglish.ui.theme.Primary
import com.appenglish.ui.theme.WarningBackground
import com.appenglish.ui.theme.WarningOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitExerciseScreen(
    onBackClick: () -> Unit,
    onTheoryClick: () -> Unit = {},
    viewModel: UnitExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context) { status -> }
    }

    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    // Back press confirmation
    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir del ejercicio?") },
            text = { Text("Tu progreso se guardará automáticamente. ¿Querés salir?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showExitDialog = false
                    onBackClick()
                }) {
                    Text("Salir", color = ErrorRed)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(uiState.playingFullAudio) {
        if (uiState.playingFullAudio && uiState.fullSentenceToPlay.isNotEmpty()) {
            tts.language = Locale.US
            tts.setSpeechRate(0.85f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { viewModel.onFullAudioFinished() }
                override fun onError(utteranceId: String?) { viewModel.onFullAudioFinished() }
            })
            delay(300)
            tts.speak(uiState.fullSentenceToPlay, TextToSpeech.QUEUE_FLUSH, null, "full_sentence")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.unitTitle.ifEmpty { "Ejercicios" }, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.blockCompleted) {
            BlockCompletionContent(
                blockTitle = viewModel.getCurrentBlockTitle(),
                blockScore = uiState.blockScore,
                blockTotalItems = uiState.blockTotalItems,
                wrongItems = uiState.wrongItems.filter { it.blockIndex == uiState.currentBlockIndex },
                onContinue = { viewModel.continueToNextBlock() },
                modifier = Modifier.padding(padding)
            )
        } else if (uiState.isFinished) {
            CompletionContent(
                uiState = uiState,
                onRetryClick = { viewModel.startRetryWrongItems() },
                onRedoClick = { viewModel.redoUnit() },
                onBackClick = onBackClick,
                modifier = Modifier.padding(padding)
            )
        } else {
            ExerciseContent(
                uiState = uiState,
                viewModel = viewModel,
                onTheoryClick = onTheoryClick,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun CompletionContent(
    uiState: UnitExerciseUiState,
    onRetryClick: () -> Unit,
    onRedoClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (uiState.totalItems > 0) (uiState.score.toFloat() / uiState.totalItems) * 100 else 0f
    val passed = percent >= 70f

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (passed) "🎉" else "📚", fontSize = 80.sp)
        Text(
            if (passed) "¡Unidad aprobada!" else "No aprobaste",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (passed) CorrectGreen else ErrorRed
        )
        Text("Puntuación: ${uiState.score} / ${uiState.totalItems}", style = MaterialTheme.typography.titleLarge)
        Text("${percent.toInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = if (passed) CorrectGreen else ErrorRed)
        if (!passed) {
            Text("Necesitás 70% para aprobar", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }

        if (uiState.wrongItems.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WarningBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Errores (${uiState.wrongItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WarningOrange)
                    }
                    Spacer(Modifier.height(12.dp))
                    uiState.wrongItems.take(5).forEach { wrong ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(wrong.sentence, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Row { Text("Vos: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text(wrong.givenAnswer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ErrorRed) }
                            }
                        }
                    }
                    if (uiState.wrongItems.size > 5) {
                        Text("... y ${uiState.wrongItems.size - 5} más", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (!passed && uiState.wrongItems.isNotEmpty()) {
            Button(onClick = onRetryClick, colors = ButtonDefaults.buttonColors(containerColor = WarningOrange), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Rehacer errores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (!passed) {
            Button(onClick = onRedoClick, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Rehacer unidad completa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = if (passed) Primary else Color.Gray), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (passed) "Continuar" else "Volver", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BlockCompletionContent(
    blockTitle: String,
    blockScore: Int,
    blockTotalItems: Int,
    wrongItems: List<WrongAnswer>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (blockTotalItems > 0) (blockScore.toFloat() / blockTotalItems) * 100 else 0f

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✅", fontSize = 80.sp)
        Text("Bloque completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Primary)
        if (blockTitle.isNotBlank()) {
            Text(blockTitle, style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        }
        Text("$blockScore / $blockTotalItems", style = MaterialTheme.typography.titleLarge)
        Text("${percent.toInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = if (percent >= 70) CorrectGreen else WarningOrange)

        if (wrongItems.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WarningBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Errores en este bloque (${wrongItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WarningOrange)
                    }
                    Spacer(Modifier.height(12.dp))
                    wrongItems.forEach { wrong ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(wrong.sentence, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Row { Text("Vos: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text(wrong.givenAnswer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ErrorRed) }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Continuar al siguiente bloque", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseContent(
    uiState: UnitExerciseUiState,
    viewModel: UnitExerciseViewModel,
    onTheoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem = viewModel.getCurrentItem()
    val options = viewModel.getOptions()
    val scope = rememberCoroutineScope()
    var showTranslation by remember { mutableStateOf(false) }
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    val inputMode = currentItem?.inputMode ?: "tap"
    val scrollState = rememberScrollState()

    var draggingWord by remember { mutableStateOf<String?>(null) }
    var isOverDropZone by remember { mutableStateOf(false) }

    var popupSentence by remember { mutableStateOf("") }
    var popupHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.isCorrect, uiState.showingAnswer) {
        if (uiState.isCorrect == true && uiState.showingAnswer && currentItem != null) {
            popupSentence = currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer)
            popupHint = currentItem.hint
        }
    }

    val selectedTab = uiState.selectedTab

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = { Text("EJERCICIOS", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = { Text("TEORÍA", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> {
                val itemType = viewModel.getItemType()
                if (itemType == "multiple-choice") {
                    MultipleChoiceCard(item = currentItem!!, isCorrect = uiState.isCorrect, showingAnswer = uiState.showingAnswer, onSelect = { idx -> viewModel.selectMcOption(idx) })
                } else if (itemType == "true-false") {
                    TrueFalseCard(item = currentItem!!, isCorrect = uiState.isCorrect, showingAnswer = uiState.showingAnswer, onSelect = { answer -> viewModel.selectTfAnswer(answer) })
                } else if (itemType == "reorder") {
                    ReorderCard(uiState = uiState, onSelectWord = { viewModel.selectReorderWord(it) }, onRemoveWord = { viewModel.removeReorderWord(it) }, onCheck = { viewModel.checkReorderAnswer() }, onInit = { viewModel.initReorder() })
                } else if (itemType == "matching") {
                    MatchingCard(item = currentItem!!, matchedPairs = uiState.matchedPairs, onMatch = { left, right -> viewModel.checkMatchingPair(left, right) }, onReset = { viewModel.resetAllMatched() })
                } else if (itemType == "listening") {
                    ListeningCard(item = currentItem!!, userInput = uiState.userInput, onInputChanged = { viewModel.onInputChanged(it) }, onCheck = { viewModel.checkTextAnswer() })
                } else {
                    ExerciseTabContent(
                    uiState = uiState,
                    currentItem = currentItem,
                    options = options,
                    scope = scope,
                    inputMode = inputMode,
                    scrollState = scrollState,
                    showTranslation = showTranslation,
                    translatedText = translatedText,
                    isTranslating = isTranslating,
                    draggingWord = draggingWord,
                    isOverDropZone = isOverDropZone,
                    blockCompleted = viewModel.getBlockCompletedItems(),
                    blockTotal = viewModel.getBlockTotalItems(),
                    onTranslationToggle = {
                        if (!isTranslating && translatedText == null) {
                            isTranslating = true
                            showTranslation = true
                            scope.launch {
                                translatedText = translateWord(currentItem?.sentence?.replace(Regex("_{2,}"), currentItem?.answer ?: "") ?: "")
                                isTranslating = false
                            }
                        } else showTranslation = !showTranslation
                    },
                    onDraggingWordChange = { draggingWord = it },
                    onIsOverDropZoneChange = { isOverDropZone = it },
                    getCurrentBlockTitle = { viewModel.getCurrentBlockTitle() },
                    onSelectOption = { viewModel.selectOption(it) },
                    onInputChanged = { viewModel.onInputChanged(it) },
                    onCheckTextAnswer = { viewModel.checkTextAnswer() }
                )
                }
            }
            1 -> {
                val blockTheory = uiState.currentBlockTheory
                val unitTheory = uiState.unitTheory
                if (blockTheory != null) {
                    ExerciseTheoryTabContent(theory = blockTheory)
                } else if (unitTheory != null) {
                    ExerciseTheoryTabContent(theory = BlockTheory(
                        text = unitTheory.text,
                        sections = unitTheory.sections,
                        headers = unitTheory.headers,
                        rows = unitTheory.rows,
                        tips = unitTheory.tips,
                        blocks = unitTheory.blocks
                    ))
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay teoría disponible", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Current feedback - positioned at the bottom
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AnimatedVisibility(
                visible = uiState.isCorrect == true && uiState.showingAnswer,
                enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(tween(100))
            ) {
                FeedbackBar(
                    isCorrect = true,
                    sentence = popupSentence,
                    hint = popupHint,
                    onContinue = {
                        scope.launch {
                            delay(250)
                            if (uiState.retryMode) viewModel.retryNextAfterCorrect()
                            else viewModel.nextItem()
                        }
                    },
                    buttonText = if (uiState.readyForNext) "Siguiente →" else "Continuar"
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AnimatedVisibility(
                visible = uiState.showAcceptButton,
                enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(tween(100))
            ) {
                FeedbackBar(
                    isCorrect = false,
                    userAnswer = uiState.userInput,
                    correctAnswer = if (uiState.retryMode) uiState.wrongItems.getOrNull(uiState.retryIndex)?.correctAnswer ?: currentItem?.answer ?: "" else currentItem?.answer ?: "",
                    onAccept = { viewModel.onAcceptClick() }
                )
            }
        }
    }
}

@Composable
private fun FeedbackBar(
    isCorrect: Boolean,
    sentence: String = "",
    hint: String? = null,
    userAnswer: String = "",
    correctAnswer: String = "",
    onContinue: () -> Unit = {},
    onAccept: () -> Unit = {},
    buttonText: String = "Continuar"
) {
    val bgColor = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCorrect) "✓" else "✗",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isCorrect) "¡Correcto!" else "Incorrecto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isCorrect && sentence.isNotBlank()) {
                    Text(
                        sentence,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
                if (!isCorrect) {
                    Text(
                        "Respuesta: ${userAnswer.ifBlank { "(vacío)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { if (isCorrect) onContinue() else onAccept() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = bgColor
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    if (isCorrect) buttonText else "Aceptar",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseTabContent(
    uiState: UnitExerciseUiState,
    currentItem: com.appenglish.domain.model.ExerciseItem?,
    options: List<String>,
    scope: kotlinx.coroutines.CoroutineScope,
    inputMode: String,
    scrollState: androidx.compose.foundation.ScrollState,
    showTranslation: Boolean,
    translatedText: String?,
    isTranslating: Boolean,
    draggingWord: String?,
    isOverDropZone: Boolean,
    blockCompleted: Int,
    blockTotal: Int,
    getCurrentBlockTitle: () -> String,
    onTranslationToggle: () -> Unit,
    onDraggingWordChange: (String?) -> Unit,
    onIsOverDropZoneChange: (Boolean) -> Unit,
    onSelectOption: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onCheckTextAnswer: () -> Unit
) {
    val dropZoneRectState = remember { mutableStateOf(Rect.Zero) }
    var dropZoneRect by dropZoneRectState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState, enabled = draggingWord == null),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(Modifier.padding(12.dp)) {
                Text(getCurrentBlockTitle(), style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("${blockCompleted} / ${blockTotal}", style = MaterialTheme.typography.titleLarge, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (blockTotal > 0) blockCompleted.toFloat() / blockTotal else 0f },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = Primary, trackColor = Primary.copy(alpha = 0.2f)
                )
            }
        }

        if (currentItem != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        val ttsText = if (uiState.showingAnswer) currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer) else currentItem.sentence
                        TtsButton(text = ttsText)
                        IconButton(onClick = onTranslationToggle, colors = IconButtonDefaults.iconButtonColors(contentColor = InfoBlue)) {
                            Icon(Icons.Default.RemoveRedEye, "Traducir", modifier = Modifier.size(26.dp))
                        }
                    }

                    val partsBefore = currentItem.sentence.split(Regex("_{2,}"), limit = 2)
                    val beforeText = partsBefore.getOrElse(0) { "" }.trimEnd()
                    val afterText = partsBefore.getOrElse(1) { "" }.trimStart()

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (beforeText.isNotEmpty()) {
                            TranslateableText(text = beforeText, fontSize = 24, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(Modifier.width(6.dp))

                        val dropBg = when {
                            uiState.showingAnswer && uiState.isCorrect == true -> CorrectBackground
                            uiState.showingAnswer && uiState.isCorrect == false -> com.appenglish.ui.theme.IncorrectBackground
                            isOverDropZone && draggingWord != null -> Primary.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                        val dropBorder = when {
                            uiState.showingAnswer && uiState.isCorrect == true -> CorrectGreen
                            uiState.showingAnswer && uiState.isCorrect == false -> ErrorRed
                            isOverDropZone && draggingWord != null -> Primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }

                        Box(
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color = dropBg, shape = RoundedCornerShape(12.dp))
                                .border(if (isOverDropZone && draggingWord != null) 3.dp else 2.dp, dropBorder, RoundedCornerShape(12.dp))
                                .onGloballyPositioned { coords -> dropZoneRect = coords.boundsInRoot() }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.showingAnswer) {
                                Text(
                                    currentItem.answer, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isCorrect == true) CorrectGreen else if (uiState.isCorrect == false) ErrorRed else Primary
                                )
                            } else if (inputMode == "type") {
                                // Inline text input for type mode
                                val focusRequester = remember { FocusRequester() }
                                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                                BasicTextField(
                                    value = uiState.userInput,
                                    onValueChange = onInputChanged,
                                    modifier = Modifier.focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { onCheckTextAnswer() }),
                                    decorationBox = { innerTextField ->
                                        if (uiState.userInput.isEmpty()) {
                                            Text(
                                                "______",
                                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            } else {
                                Text(
                                    "_____", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        Spacer(Modifier.width(6.dp))
                        if (afterText.isNotEmpty()) {
                            TranslateableText(text = afterText, fontSize = 24, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    // Corregir button for type mode
                    if (inputMode == "type" && uiState.isCorrect == null && uiState.userInput.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onCheckTextAnswer,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Corregir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            if (showTranslation && translatedText != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RemoveRedEye, null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                translatedText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }
            }

            if (inputMode == "type" && uiState.isCorrect == null) {
                Spacer(Modifier.height(8.dp))
                Text("Escribí tu respuesta directamente en el espacio", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                // No separate input field needed - user types inline
            }

            if (inputMode == "tap" && uiState.isCorrect == null && options.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Arrastrá la palabra al hueco", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    options.forEach { option ->
                        var dragDelta by remember { mutableStateOf(Offset.Zero) }
                        var localOrigin by remember { mutableStateOf(Rect.Zero) }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 150.dp)
                                .offset { IntOffset(dragDelta.x.roundToInt(), dragDelta.y.roundToInt()) }
                                .shadow(if (dragDelta != Offset.Zero) 14.dp else 6.dp, RoundedCornerShape(16.dp))
                                .onGloballyPositioned { coords ->
                                    if (draggingWord == null) {
                                        localOrigin = coords.boundsInRoot()
                                    }
                                }
                                .pointerInput(option) {
                                    detectDragGestures(
                                        onDragStart = { _ ->
                                            onDraggingWordChange(option)
                                            dragDelta = Offset.Zero
                                            onIsOverDropZoneChange(false)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDelta += dragAmount
                                            val wordRect = Rect(
                                                left = localOrigin.left + dragDelta.x,
                                                top = localOrigin.top + dragDelta.y,
                                                right = localOrigin.right + dragDelta.x,
                                                bottom = localOrigin.bottom + dragDelta.y
                                            )
                                            onIsOverDropZoneChange(wordRect.overlaps(dropZoneRectState.value))
                                        },
                                        onDragEnd = {
                                            if (dropZoneRectState.value != Rect.Zero) {
                                                val wordRect = Rect(
                                                    left = localOrigin.left + dragDelta.x,
                                                    top = localOrigin.top + dragDelta.y,
                                                    right = localOrigin.right + dragDelta.x,
                                                    bottom = localOrigin.bottom + dragDelta.y
                                                )
                                                if (wordRect.overlaps(dropZoneRectState.value)) {
                                                    onSelectOption(option)
                                                }
                                            }
                                            onDraggingWordChange(null)
                                            onIsOverDropZoneChange(false)
                                            dragDelta = Offset.Zero
                                        },
                                        onDragCancel = {
                                            onDraggingWordChange(null)
                                            onIsOverDropZoneChange(false)
                                            dragDelta = Offset.Zero
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Primary)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(option, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ExerciseTheoryTabContent(theory: BlockTheory) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Teoría del bloque",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                Spacer(Modifier.height(12.dp))

                if (theory.blocks.isNotEmpty()) {
                    theory.blocks.forEach { block ->
                        if (!block.title.isNullOrBlank()) {
                            Text(
                                block.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        if (!block.html.isNullOrBlank()) {
                            TheoryHtmlView(html = block.html)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else if (theory.text.isNotBlank()) {
                    Text(theory.text, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("No hay teoría para este bloque", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }

        if (theory.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WarningOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    theory.tips.forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tip.emoji, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(tip.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MultipleChoiceCard(item: com.appenglish.domain.model.ExerciseItem, isCorrect: Boolean?, showingAnswer: Boolean, onSelect: (Int) -> Unit) {
    val options = item.options?.filter { it.isNotBlank() } ?: emptyList()
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(item.question ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            options.forEachIndexed { idx, option ->
                val isCorrectOpt = option.trim().lowercase() == item.answer.trim().lowercase()
                val showResult = isCorrect != null || showingAnswer
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = isCorrect == null) { onSelect(idx) }, shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = when { showResult && isCorrectOpt -> CorrectBackground; showResult && !isCorrectOpt -> com.appenglish.ui.theme.IncorrectBackground; else -> Primary.copy(alpha = 0.1f) })) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("${'A' + idx}) ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary); Text(option, style = MaterialTheme.typography.bodyLarge) }
                }
            }
            if (item.hint != null && showingAnswer) { Spacer(Modifier.height(8.dp)); Text("\uD83D\uDCA1 ${item.hint}", style = MaterialTheme.typography.bodyMedium, color = WarningOrange) }
        }
    }
}

@Composable
private fun TrueFalseCard(item: com.appenglish.domain.model.ExerciseItem, isCorrect: Boolean?, showingAnswer: Boolean, onSelect: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.sentence, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSelect(true) }, colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen), modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), enabled = isCorrect == null) { Text("\u2705 Verdadero") }
                Button(onClick = { onSelect(false) }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), enabled = isCorrect == null) { Text("\u274C Falso") }
            }
            if (showingAnswer && !(item.isCorrect ?: false)) { Spacer(Modifier.height(12.dp)); Text("\u2705 ${item.answer}", style = MaterialTheme.typography.titleMedium, color = CorrectGreen, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReorderCard(uiState: UnitExerciseUiState, onSelectWord: (String) -> Unit, onRemoveWord: (Int) -> Unit, onCheck: () -> Unit, onInit: () -> Unit) {
    LaunchedEffect(Unit) { onInit() }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Ordena las palabras:", style = MaterialTheme.typography.titleMedium, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            // Slots with next indicator
            val nextEmptyIndex = uiState.reorderSlots.indexOfFirst { it == null }
            Row(modifier = Modifier.fillMaxWidth().border(2.dp, Primary, RoundedCornerShape(12.dp)).padding(8.dp), horizontalArrangement = Arrangement.Center) {
                if (uiState.reorderSlots.isEmpty() || uiState.reorderSlots.all { it == null }) {
                    Text("...", color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.reorderSlots.forEachIndexed { idx, w ->
                            val isNext = idx == nextEmptyIndex
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (w != null) Primary.copy(alpha = 0.1f)
                                        else if (isNext) Primary.copy(alpha = 0.05f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        if (isNext && w == null) 2.dp else 0.dp,
                                        if (isNext && w == null) Primary.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = w != null) { if (w != null) onRemoveWord(idx) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    w ?: if (isNext) "▸" else "_",
                                    color = if (w != null) Primary else if (isNext) Primary.copy(alpha = 0.5f) else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                uiState.reorderWords.forEach { w ->
                    Box(modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(10.dp)).clickable { onSelectWord(w) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(w, fontWeight = FontWeight.Bold, color = Primary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onCheck, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Corregir")
            }
        }
    }
}

@Composable
private fun MatchingCard(item: com.appenglish.domain.model.ExerciseItem, matchedPairs: Int, onMatch: (String, String) -> Unit, onReset: () -> Unit) {
    val pairs = item.pairs ?: emptyList()
    val leftItems = pairs.map { it.left }
    val rightItems = remember { pairs.map { it.right }.shuffled() }
    var selL by remember { mutableStateOf("") }
    var selR by remember { mutableStateOf("") }
    var matchedLeft by remember { mutableStateOf(setOf<String>()) }
    var matchedRight by remember { mutableStateOf(setOf<String>()) }
    var lastWrongLeft by remember { mutableStateOf<String?>(null) }
    var lastWrongRight by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Uni las columnas (${matchedPairs}/${pairs.size})", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leftItems.forEach { l ->
                        val isMatched = matchedLeft.contains(l)
                        val isWrong = lastWrongLeft == l
                        val isSelected = selL == l
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        isMatched -> CorrectGreen.copy(alpha = 0.2f)
                                        isWrong -> ErrorRed.copy(alpha = 0.2f)
                                        isSelected -> Primary.copy(alpha = 0.3f)
                                        else -> Color(0xFFE8F5E9)
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isMatched) {
                                    if (!isMatched) {
                                        selL = l
                                        lastWrongLeft = null
                                        lastWrongRight = null
                                        if (selR.isNotEmpty()) {
                                            val isCorrect = pairs.any { it.left == l && it.right == selR }
                                            if (isCorrect) {
                                                matchedLeft = matchedLeft + l
                                                matchedRight = matchedRight + selR
                                            } else {
                                                lastWrongLeft = l
                                                lastWrongRight = selR
                                            }
                                            onMatch(l, selR)
                                            selL = ""
                                            selR = ""
                                        }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isMatched) {
                                    Icon(Icons.Default.Check, null, tint = CorrectGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(l, fontWeight = FontWeight.Bold, color = if (isMatched) CorrectGreen else Color.Unspecified)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rightItems.forEach { r ->
                        val isMatched = matchedRight.contains(r)
                        val isWrong = lastWrongRight == r
                        val isSelected = selR == r
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        isMatched -> CorrectGreen.copy(alpha = 0.2f)
                                        isWrong -> ErrorRed.copy(alpha = 0.2f)
                                        isSelected -> Primary.copy(alpha = 0.3f)
                                        else -> Color(0xFFE3F2FD)
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isMatched) {
                                    if (!isMatched) {
                                        selR = r
                                        lastWrongLeft = null
                                        lastWrongRight = null
                                        if (selL.isNotEmpty()) {
                                            val isCorrect = pairs.any { it.left == selL && it.right == r }
                                            if (isCorrect) {
                                                matchedLeft = matchedLeft + selL
                                                matchedRight = matchedRight + r
                                            } else {
                                                lastWrongLeft = selL
                                                lastWrongRight = r
                                            }
                                            onMatch(selL, r)
                                            selL = ""
                                            selR = ""
                                        }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isMatched) {
                                    Icon(Icons.Default.Check, null, tint = CorrectGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(r, fontWeight = FontWeight.Bold, color = if (isMatched) CorrectGreen else Color.Unspecified)
                            }
                        }
                    }
                }
            }
            if (matchedPairs >= pairs.size) {
                Spacer(Modifier.height(12.dp))
                Text("¡Todos los pares encontrados! ✅", style = MaterialTheme.typography.bodyMedium, color = CorrectGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ListeningCard(item: com.appenglish.domain.model.ExerciseItem, userInput: String, onInputChanged: (String) -> Unit, onCheck: () -> Unit) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val tts = remember { TextToSpeech(context) { } }

    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    fun playAudio() {
        isPlaying = true
        tts.language = Locale.US
        tts.setSpeechRate(0.85f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { isPlaying = false }
            override fun onError(utteranceId: String?) { isPlaying = false }
        })
        tts.speak(item.sentence, TextToSpeech.QUEUE_FLUSH, null, "listening")
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔊", fontSize = 60.sp)
            Spacer(Modifier.height(8.dp))
            Text("Escucha y escribi", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(Modifier.height(12.dp))

            // Replay button
            Button(
                onClick = { playAudio() },
                colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Gray else InfoBlue),
                shape = RoundedCornerShape(12.dp),
                enabled = !isPlaying
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) "Reproduciendo..." else "Escuchar de nuevo", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChanged,
                label = { Text("Tu respuesta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCheck,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = userInput.isNotBlank()
            ) {
                Text("Corregir", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Auto-play on first load
    LaunchedEffect(Unit) { playAudio() }
}
