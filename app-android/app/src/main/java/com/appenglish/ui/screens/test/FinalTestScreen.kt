package com.appenglish.ui.screens.test

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveRedEye
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appenglish.ui.components.TtsButton
import com.appenglish.ui.components.TranslateableText
import com.appenglish.ui.components.translateWord
import com.appenglish.ui.theme.CorrectGreen
import com.appenglish.ui.theme.IncorrectRed
import com.appenglish.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalTestScreen(
    onBackClick: () -> Unit,
    viewModel: FinalTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val tts = remember { TextToSpeech(context) { } }

    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    LaunchedEffect(uiState.playingFullAudio) {
        if (uiState.playingFullAudio && uiState.fullSentenceToPlay.isNotEmpty()) {
            tts.language = Locale.US
            tts.setSpeechRate(0.85f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { viewModel.onFullAudioFinished() }
                override fun onError(id: String?) { viewModel.onFullAudioFinished() }
            })
            delay(300)
            tts.speak(uiState.fullSentenceToPlay, TextToSpeech.QUEUE_FLUSH, null, "test_full")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Final", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else if (uiState.isFinished) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 64.sp); Spacer(Modifier.height(16.dp))
                    Text("Test finalizado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Puntuación: ${uiState.score} / ${uiState.questions.size}", style = MaterialTheme.typography.titleMedium)
                    val pct = if (uiState.questions.isNotEmpty()) (uiState.score.toFloat() / uiState.questions.size) * 100 else 0f
                    Text("${pct.toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (pct >= 70) CorrectGreen else IncorrectRed)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp)) { Text("Volver", color = Color.White) }
                }
            }
        } else {
            val question = viewModel.getCurrentQuestion()
            val options = viewModel.getOptions()

            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pregunta ${uiState.currentIndex + 1} de ${uiState.questions.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Score: ${uiState.score}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { if (uiState.questions.isNotEmpty()) (uiState.currentIndex + 1).toFloat() / uiState.questions.size else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 4.dp), color = Primary, trackColor = Color(0xFFE0E0E0)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (question != null) {
                    item {
                        val scope = rememberCoroutineScope()
                        var showTranslation by remember { mutableStateOf(false) }
                        var translatedText by remember { mutableStateOf<String?>(null) }
                        var isTranslating by remember { mutableStateOf(false) }

                        val sentenceParts = question.sentence.split(Regex("_{2,}"), limit = 2)
                        val beforeBlank = sentenceParts.getOrElse(0) { "" }
                        val afterBlank = sentenceParts.getOrElse(1) { "" }

                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(question.unitId, style = MaterialTheme.typography.labelMedium, color = Primary)
                                    Row {
                                        val ttsText = if (uiState.showingAnswer) question.sentence.replace(Regex("_{2,}"), question.answer) else question.sentence
                                        TtsButton(text = ttsText)
                                        IconButton(onClick = {
                                            if (!isTranslating && translatedText == null) { isTranslating = true; showTranslation = true; scope.launch { translatedText = translateWord(question.sentence.replace(Regex("_{2,}"), question.answer)); isTranslating = false } }
                                            else showTranslation = !showTranslation
                                        }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF2196F3))) {
                                            Icon(Icons.Default.RemoveRedEye, "Ver traducción", modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (beforeBlank.isNotEmpty()) TranslateableText(text = beforeBlank.trimEnd(), fontSize = 22, modifier = Modifier)

                                    Spacer(Modifier.width(6.dp))

                                    AnimatedContent(
                                        targetState = uiState.showingAnswer,
                                        transitionSpec = {
                                            if (targetState) (slideInVertically(tween(400)) { it } + fadeIn(tween(400))).togetherWith(fadeOut(tween(200)))
                                            else (fadeIn(tween(200))).togetherWith(fadeOut(tween(200)))
                                                .using(SizeTransform(clip = false))
                                        },
                                        label = "answer"
                                    ) { showing ->
                                        if (showing) {
                                            Box(Modifier.clip(RoundedCornerShape(8.dp)).border(2.dp, CorrectGreen, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                                Text(question.answer, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold, color = CorrectGreen)
                                            }
                                        } else {
                                            Box(Modifier.clip(RoundedCornerShape(8.dp)).border(2.dp, Color(0xFFBDBDBD), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                                Text("______", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = Color(0xFFBDBDBD))
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    if (afterBlank.isNotEmpty()) TranslateableText(text = afterBlank.trimStart(), fontSize = 22, modifier = Modifier)
                                }

                                Spacer(Modifier.height(20.dp))

                                if (!uiState.answered || uiState.isCorrect == null) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        items(options) { opt ->
                                            FilterChip(selected = false, onClick = { viewModel.selectOption(opt) },
                                                label = { Text(opt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) },
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFF5F5F5), labelColor = MaterialTheme.colorScheme.onSurface))
                                        }
                                    }
                                } else if (uiState.isCorrect == true) {
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        items(options) { opt ->
                                            val isCorrectOpt = opt.trim().lowercase() == question.answer.trim().lowercase()
                                            val wasSelected = uiState.userInput == opt
                                            FilterChip(selected = false, onClick = {}, label = { Text(opt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }, enabled = false, modifier = Modifier.padding(vertical = 4.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = when { isCorrectOpt -> Color(0xFFC8E6C9); wasSelected && !isCorrectOpt -> Color(0xFFFFCDD2); else -> Color(0xFFF5F5F5) },
                                                    selectedContainerColor = Color.Transparent,
                                                    labelColor = when { isCorrectOpt -> Color(0xFF2E7D32); wasSelected && !isCorrectOpt -> Color(0xFFC62828); else -> Color(0xFFBDBDBD) }))
                                        }
                                    }
                                } else if (uiState.isCorrect == false) {
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        items(options) { opt ->
                                            val wasSelected = uiState.userInput == opt
                                            FilterChip(selected = false, onClick = {}, label = { Text(opt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }, enabled = false, modifier = Modifier.padding(vertical = 4.dp),
                                                colors = FilterChipDefaults.filterChipColors(containerColor = if (wasSelected) Color(0xFFFFCDD2) else Color(0xFFF5F5F5), selectedContainerColor = Color.Transparent, labelColor = if (wasSelected) Color(0xFFC62828) else Color(0xFFBDBDBD)))
                                        }
                                    }
                                }

                                // Fixed-height feedback area
                                Box(Modifier.fillMaxWidth().height(56.dp)) {
                                    val fb = uiState.feedback
                                    if (fb != null) {
                                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (uiState.isCorrect == true) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
                                            Text(fb, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (uiState.isCorrect == true) CorrectGreen else IncorrectRed, modifier = Modifier.padding(12.dp))
                                        }
                                    }
                                }

                                if (uiState.playingFullAudio) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("🔊 Reproduciendo...", style = MaterialTheme.typography.bodySmall, color = Primary)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                val isLast = uiState.currentIndex + 1 >= uiState.questions.size
                                Button(
                                    onClick = { viewModel.nextQuestion() },
                                    enabled = uiState.readyForNext,
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Color(0xFFE0E0E0), disabledContentColor = Color(0xFF9E9E9E)),
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                                ) { Text(if (isLast) "Finalizar" else "Siguiente →", color = if (uiState.readyForNext) Color.White else Color(0xFF9E9E9E)) }
                            }
                        }

                        AnimatedContent(targetState = showTranslation, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "trans") { visible ->
                            if (visible) {
                                Spacer(Modifier.height(4.dp))
                                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        if (isTranslating) { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF2196F3)); Spacer(Modifier.width(8.dp)); Text("Traduciendo...", style = MaterialTheme.typography.bodySmall) } }
                                        else if (translatedText != null) { Text("Traducción:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0)); Spacer(Modifier.height(2.dp)); Text(translatedText!!, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
