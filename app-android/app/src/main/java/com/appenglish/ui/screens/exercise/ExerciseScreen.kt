package com.appenglish.ui.screens.exercise

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.appenglish.ui.components.TtsButton
import com.appenglish.ui.components.TranslateableText
import com.appenglish.ui.components.translateWord
import com.appenglish.ui.theme.CorrectBackground
import com.appenglish.ui.theme.CorrectGreen
import com.appenglish.ui.theme.ErrorRed
import com.appenglish.ui.theme.IncorrectBackground
import com.appenglish.ui.theme.InfoBlue
import com.appenglish.ui.theme.InfoBackground
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

    val tts = remember {
        TextToSpeech(context) { status -> }
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
                    IconButton(onClick = onBackClick) {
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
        } else if (uiState.isFinished) {
            CompletionContent(
                uiState = uiState,
                onRetryClick = { viewModel.startRetryWrongItems() },
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🎉", fontSize = 80.sp)
        Text("¡Unidad completada!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Primary)
        Text("Puntuación: ${uiState.score} / ${uiState.totalItems}", style = MaterialTheme.typography.titleLarge)
        val percent = if (uiState.totalItems > 0) (uiState.score.toFloat() / uiState.totalItems) * 100 else 0f
        Text("${percent.toInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = if (percent >= 70) CorrectGreen else ErrorRed)

        if (uiState.wrongItems.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WarningBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Errores para revisar (${uiState.wrongItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WarningOrange)
                    }
                    Spacer(Modifier.height(12.dp))
                    uiState.wrongItems.forEach { wrong ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(wrong.sentence, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Row { Text("Vos: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text(wrong.givenAnswer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ErrorRed) }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetryClick, colors = ButtonDefaults.buttonColors(containerColor = WarningOrange), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text("Rehacer errores", style = MaterialTheme.typography.titleMedium, color = Color.White) }
                }
            }
        }

        Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Volver", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White) }
    }
}

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
    val density = LocalDensity.current
    var showTranslation by remember { mutableStateOf(false) }
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    val inputMode = currentItem?.inputMode ?: "tap"
    val scrollState = rememberScrollState()

    var draggingWord by remember { mutableStateOf<String?>(null) }
    var isOverDropZone by remember { mutableStateOf(false) }
    var dropZoneRect by remember { mutableStateOf(Rect.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState, enabled = draggingWord == null),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            if (uiState.unitTheory != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CorrectBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    onClick = onTheoryClick
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📖", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ver explicación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CorrectGreen)
                            Text("Revisá la teoría antes de ejercitar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text("→", fontSize = 24.sp, color = CorrectGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${uiState.completedItems} / ${uiState.totalItems}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(viewModel.getCurrentBlockTitle(), style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (uiState.totalItems > 0) uiState.completedItems.toFloat() / uiState.totalItems else 0f },
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
                            IconButton(
                                onClick = {
                                    if (!isTranslating && translatedText == null) {
                                        isTranslating = true; showTranslation = true
                                        scope.launch {
                                            translatedText = translateWord(currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer))
                                            isTranslating = false
                                        }
                                    } else showTranslation = !showTranslation
                                },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = InfoBlue)
                            ) { Icon(Icons.Default.RemoveRedEye, "Traducir", modifier = Modifier.size(26.dp)) }
                        }

                        val partsBefore = currentItem.sentence.split(Regex("_{2,}"), limit = 2)
                        val beforeText = partsBefore.getOrElse(0) { "" }.trimEnd()
                        val afterText = partsBefore.getOrElse(1) { "" }.trimStart()

                        Row(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (beforeText.isNotEmpty()) TranslateableText(text = beforeText, fontSize = 24, modifier = Modifier)

                            Spacer(Modifier.width(6.dp))

                            val dropBg = when {
                                uiState.showingAnswer && uiState.isCorrect == true -> CorrectBackground
                                uiState.showingAnswer && uiState.isCorrect == false -> IncorrectBackground
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
                                    .widthIn(min = 100.dp, max = 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color = dropBg, shape = RoundedCornerShape(12.dp))
                                    .border(if (isOverDropZone && draggingWord != null) 3.dp else 2.dp, dropBorder, RoundedCornerShape(12.dp))
                                    .onGloballyPositioned { coords -> dropZoneRect = coords.boundsInRoot() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.showingAnswer) {
                                    Text(
                                        currentItem.answer, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isCorrect == true) CorrectGreen else if (uiState.isCorrect == false) ErrorRed else Primary
                                    )
                                } else {
                                    Text(
                                        "_____", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))
                            if (afterText.isNotEmpty()) TranslateableText(text = afterText, fontSize = 24, modifier = Modifier)
                        }

                        if (uiState.playingFullAudio) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Reproduciendo audio...", style = MaterialTheme.typography.bodySmall, color = Primary)
                            }
                        }

                        AnimatedVisibility(visible = showTranslation && translatedText != null) {
                            Spacer(Modifier.height(12.dp))
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = InfoBackground)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.RemoveRedEye, null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(translatedText ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            if (inputMode == "type" && uiState.isCorrect == null) {
                OutlinedTextField(
                    value = uiState.userInput, onValueChange = { viewModel.onInputChanged(it) },
                    label = { Text("Escribí tu respuesta") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.checkTextAnswer() })
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.checkTextAnswer() }, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Corregir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (inputMode == "tap" && uiState.isCorrect == null && options.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Arrastrá la palabra al hueco", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    options.forEach { option ->
                        var cardRootBounds by remember { mutableStateOf(Rect.Zero) }
                        var dragDelta by remember { mutableStateOf(Offset.Zero) }

                        Card(
                            modifier = Modifier
                                .offset { IntOffset(dragDelta.x.roundToInt(), dragDelta.y.roundToInt()) }
                                .shadow(if (dragDelta != Offset.Zero) 14.dp else 6.dp, RoundedCornerShape(16.dp))
                                .onGloballyPositioned { coords -> cardRootBounds = coords.boundsInRoot() }
                                .pointerInput(option) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { _ ->
                                            draggingWord = option
                                            dragDelta = Offset.Zero
                                            isOverDropZone = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDelta += dragAmount
                                            val fingerRootPos = Offset(
                                                cardRootBounds.center.x + dragDelta.x,
                                                cardRootBounds.center.y + dragDelta.y
                                            )
                                            isOverDropZone = dropZoneRect.contains(fingerRootPos)
                                        },
                                        onDragEnd = {
                                            if (isOverDropZone && draggingWord != null) {
                                                viewModel.selectOption(draggingWord!!)
                                                viewModel.checkTextAnswer()
                                            }
                                            draggingWord = null
                                            isOverDropZone = false
                                            dragDelta = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingWord = null
                                            isOverDropZone = false
                                            dragDelta = Offset.Zero
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Primary)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                                Text(option, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            if (inputMode == "tap" && uiState.isCorrect != null) {
                options.forEach { option ->
                    val isCorrectOpt = option.trim().lowercase() == currentItem?.answer?.trim()?.lowercase()
                    val wasSelected = uiState.userInput == option
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = when { isCorrectOpt -> CorrectBackground; wasSelected -> IncorrectBackground; else -> Color.White })
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (isCorrectOpt) { Icon(Icons.Default.Check, null, tint = CorrectGreen, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)) }
                            else if (wasSelected) { Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)) }
                            Text(option, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = when { isCorrectOpt -> CorrectGreen; wasSelected -> ErrorRed; else -> MaterialTheme.colorScheme.onSurfaceVariant })
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        AnimatedVisibility(
            visible = uiState.isCorrect == true && uiState.showingAnswer,
            enter = fadeIn(tween(300)) + scaleIn(tween(400), initialScale = 0.6f),
            exit = fadeOut(tween(200))
        ) {
            ResultPopup(
                isCorrect = true,
                sentence = currentItem?.sentence?.replace(Regex("_{2,}"), currentItem.answer) ?: "",
                hint = currentItem?.hint,
                onContinue = { if (uiState.retryMode) viewModel.retryNextAfterCorrect() else viewModel.nextItem() },
                buttonText = if (uiState.readyForNext) "Siguiente →" else "Continuar"
            )
        }

        AnimatedVisibility(
            visible = uiState.showAcceptButton,
            enter = fadeIn(tween(300)) + scaleIn(tween(400), initialScale = 0.6f),
            exit = fadeOut(tween(200))
        ) {
            ResultPopup(
                isCorrect = false,
                userAnswer = uiState.userInput,
                correctAnswer = if (uiState.retryMode) uiState.wrongItems.getOrNull(uiState.retryIndex)?.correctAnswer ?: currentItem?.answer ?: "" else currentItem?.answer ?: "",
                onAccept = { viewModel.onAcceptClick() }
            )
        }
    }
}

@Composable
private fun ResultPopup(
    isCorrect: Boolean,
    sentence: String = "",
    hint: String? = null,
    userAnswer: String = "",
    correctAnswer: String = "",
    onContinue: () -> Unit = {},
    onAccept: () -> Unit = {},
    buttonText: String = "Continuar"
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val emoji = if (isCorrect) "🎉" else "❌"
                val title = if (isCorrect) "¡Muy bien!" else "Incorrecto"
                val titleColor = if (isCorrect) CorrectGreen else ErrorRed

                Text(emoji, fontSize = 80.sp)
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = titleColor)
                Spacer(Modifier.height(24.dp))

                if (isCorrect) {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CorrectBackground)) {
                        Text(sentence, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp), color = CorrectGreen)
                    }
                    if (hint != null) {
                        Spacer(Modifier.height(14.dp))
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = WarningBackground)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(hint, style = MaterialTheme.typography.bodyLarge, color = WarningOrange, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = IncorrectBackground)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Respondiste:", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(userAnswer.ifBlank { "(vacío)" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CorrectBackground)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = CorrectGreen, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Correcto:", style = MaterialTheme.typography.titleMedium, color = CorrectGreen)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(correctAnswer, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CorrectGreen)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { if (isCorrect) onContinue() else onAccept() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isCorrect) CorrectGreen else Primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) { Text(buttonText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
    }
}