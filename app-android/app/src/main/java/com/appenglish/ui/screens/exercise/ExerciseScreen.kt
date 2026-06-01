package com.appenglish.ui.screens.exercise

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appenglish.ui.components.TtsButton
import com.appenglish.ui.components.TranslateableText
import com.appenglish.ui.components.translateWord
import com.appenglish.ui.screens.topic.UnitTheoryView
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitExerciseScreen(
    onBackClick: () -> Unit,
    onTheoryClick: () -> Unit = {},
    viewModel: UnitExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val tts = remember {
        TextToSpeech(context) { status -> }
    }

    LaunchedEffect(uiState.playingFullAudio) {
        if (uiState.playingFullAudio && uiState.fullSentenceToPlay.isNotEmpty()) {
            tts.language = Locale.US
            tts.setSpeechRate(0.85f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    viewModel.onFullAudioFinished()
                }
                override fun onError(utteranceId: String?) {
                    viewModel.onFullAudioFinished()
                }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.isFinished) {
            CompletionContent(
                uiState = uiState,
                onRetryClick = { viewModel.startRetryWrongItems() },
                onBackClick = onBackClick
            )
        } else {
            ExerciseContent(
                uiState = uiState,
                viewModel = viewModel,
                focusManager = focusManager
            )
        }
    }
}

@Composable
private fun CompletionContent(
    uiState: UnitExerciseUiState,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("¡Unidad completada!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Puntuación: ${uiState.score} / ${uiState.totalItems}", style = MaterialTheme.typography.titleMedium)
            val percent = if (uiState.totalItems > 0) (uiState.score.toFloat() / uiState.totalItems) * 100 else 0f
            Text("${percent.toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (percent >= 70) CorrectGreen else ErrorRed)
        }

        if (uiState.wrongItems.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = WarningBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Errores para revisar (${uiState.wrongItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WarningOrange)
                        Spacer(Modifier.height(8.dp))
                        uiState.wrongItems.forEach { wrong ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(wrong.sentence, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Respondiste: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(wrong.givenAnswer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onRetryClick,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Rehacer solo los errores", color = Color.White) }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver", color = Color.White)
            }
        }
    }
}

@Composable
private fun ExerciseContent(
    uiState: UnitExerciseUiState,
    viewModel: UnitExerciseViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val currentItem = viewModel.getCurrentItem()
    val options = viewModel.getOptions()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("${uiState.completedItems} / ${uiState.totalItems} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { if (uiState.totalItems > 0) uiState.completedItems.toFloat() / uiState.totalItems else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(viewModel.getCurrentBlockTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
        }

        if (currentItem != null) {
            item {
                val scope = rememberCoroutineScope()
                var showTranslation by remember { mutableStateOf(false) }
                var translatedText by remember { mutableStateOf<String?>(null) }
                var isTranslating by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Completá el espacio:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
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
                                    modifier = Modifier.size(40.dp),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = InfoBlue)
                                ) {
                                    Icon(Icons.Default.RemoveRedEye, "Ver traducción", modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        val partsBefore = currentItem.sentence.split(Regex("_{2,}"), limit = 2)
                        val beforeText = partsBefore.getOrElse(0) { "" }.trimEnd()
                        val afterText = partsBefore.getOrElse(1) { "" }.trimStart()

                        Row(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (beforeText.isNotEmpty()) {
                                TranslateableText(text = beforeText, fontSize = 22, modifier = Modifier)
                            }

                            Spacer(Modifier.width(6.dp))

                            AnimatedContent(
                                targetState = uiState.showingAnswer,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInVertically(tween(400)) { it } + fadeIn(tween(400))).togetherWith(fadeOut(tween(200)))
                                    } else {
                                        (fadeIn(tween(200))).togetherWith(fadeOut(tween(200)))
                                    }.using(SizeTransform(clip = false))
                                },
                                label = "answer"
                            ) { showing ->
                                if (showing) {
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.medium)
                                            .border(2.dp, CorrectGreen, MaterialTheme.shapes.medium)
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(currentItem.answer, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold, color = CorrectGreen)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.medium)
                                            .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("______", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            if (afterText.isNotEmpty()) {
                                TranslateableText(text = afterText, fontSize = 22, modifier = Modifier)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        val inputMode = currentItem.inputMode ?: "tap"

                        if (uiState.isCorrect == null) {
                            if (inputMode == "type") {
                                OutlinedTextField(
                                    value = uiState.userInput,
                                    onValueChange = { viewModel.onInputChanged(it) },
                                    label = { Text("Escribí tu respuesta") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            viewModel.checkTextAnswer()
                                            focusManager.clearFocus()
                                        }
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.checkTextAnswer(); focusManager.clearFocus() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Corregir", color = Color.White) }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(options) { option ->
                                        FilterChip(
                                            selected = uiState.userInput == option,
                                            onClick = { viewModel.selectOption(option) },
                                            label = { Text(option, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) },
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isCorrect == true) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                items(options) { option ->
                                    val isCorrectOpt = option.trim().lowercase() == currentItem.answer.trim().lowercase()
                                    val wasSelected = uiState.userInput == option
                                    FilterChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text(option, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) },
                                        enabled = false,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = when {
                                                isCorrectOpt -> CorrectBackground
                                                wasSelected && !isCorrectOpt -> IncorrectBackground
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            selectedContainerColor = Color.Transparent,
                                            labelColor = when {
                                                isCorrectOpt -> CorrectGreen
                                                wasSelected && !isCorrectOpt -> ErrorRed
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        if (uiState.isCorrect == false) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                items(options) { option ->
                                    val wasSelected = uiState.userInput == option
                                    FilterChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text(option, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) },
                                        enabled = false,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = if (wasSelected) IncorrectBackground else MaterialTheme.colorScheme.surfaceVariant,
                                            selectedContainerColor = Color.Transparent,
                                            labelColor = if (wasSelected) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        if (uiState.playingFullAudio) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("🔊 Reproduciendo...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                if (uiState.isCorrect == true && uiState.showingAnswer) {
                    val isRetry = uiState.retryMode
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (isRetry) viewModel.retryNextAfterCorrect()
                                    else viewModel.nextItem()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.medium
                            ) { Text(if (uiState.readyForNext) "Siguiente →" else "🎉 Continuar", color = Color.White) }
                        },
                        title = {
                            Text("🎉 ¡Muy bien!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CorrectGreen)
                        },
                        text = {
                            Column {
                                val fullSentence = currentItem.sentence.replace(Regex("_{2,}"), currentItem.answer)
                                Text(fullSentence, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                if (currentItem.hint != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("💡 ${currentItem.hint}", style = MaterialTheme.typography.bodyMedium, color = WarningOrange)
                                }
                            }
                        },
                        containerColor = Color.White,
                        shape = MaterialTheme.shapes.large
                    )
                }

                if (uiState.showAcceptButton) {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {
                            Button(
                                onClick = { viewModel.onAcceptClick() },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Aceptar →", color = Color.White) }
                        },
                        title = {
                            Text("❌ Incorrecto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                        },
                        text = {
                            Column {
                                Text("Respondiste: ${uiState.userInput}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                val answerToShow = if (uiState.retryMode) {
                                    val wrong = uiState.wrongItems.getOrNull(uiState.retryIndex)
                                    wrong?.correctAnswer ?: currentItem?.answer ?: ""
                                } else currentItem?.answer ?: ""
                                Text("Respuesta correcta: $answerToShow", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        containerColor = Color.White,
                        shape = MaterialTheme.shapes.large
                    )
                }

                AnimatedContent(
                    targetState = showTranslation,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "translation"
                ) { visible ->
                    if (visible) {
                        Spacer(Modifier.height(4.dp))
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = InfoBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isTranslating) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = InfoBlue)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Traduciendo...", style = MaterialTheme.typography.bodySmall)
                                    }
                                } else if (translatedText != null) {
                                    Text("Traducción:", style = MaterialTheme.typography.labelSmall, color = InfoBlue)
                                    Spacer(Modifier.height(2.dp))
                                    Text(translatedText!!, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}