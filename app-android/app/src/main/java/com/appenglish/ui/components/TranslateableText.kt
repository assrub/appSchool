package com.appenglish.ui.components

import android.content.Context
import android.media.MediaPlayer
import com.appenglish.R
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.appenglish.ui.theme.CorrectBackground
import com.appenglish.ui.theme.InfoBlue
import com.appenglish.ui.theme.Primary
import com.appenglish.util.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

fun isUnderscoreWord(word: String): Boolean {
    val cleaned = word.trimEnd('.', ',', '!', '?', ';', ':')
    return cleaned.all { it == '_' }
}

object SoundHelper {
    fun playCorrect(context: Context) {
        try {
            val mp = MediaPlayer.create(context, R.raw.correct)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        } catch (_: Exception) {}
    }

    fun playIncorrect(context: Context) {
        try {
            val mp = MediaPlayer.create(context, R.raw.fail)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        } catch (_: Exception) {}
    }
}

@Composable
fun TranslateableText(
    text: String,
    onDictionarySave: ((String, String) -> Unit)? = null,
    fontSize: Int = 18,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var translation by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var popupVisible by remember { mutableStateOf(false) }

    val rawWords = text.split(" ").filter { it.isNotBlank() }

    val annotatedString = buildAnnotatedString {
        rawWords.forEachIndexed { index, word ->
            val cleanWord = word.trimEnd('.', ',', '!', '?', ';', ':')

            if (isUnderscoreWord(word)) {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)) {
                    append(cleanWord)
                }
            } else {
                val isSelected = cleanWord == selectedWord && popupVisible
                pushStringAnnotation("word", cleanWord)
                withStyle(
                    SpanStyle(
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = fontSize.sp,
                        background = if (isSelected) Primary else Color.Transparent,
                        textDecoration = if (isSelected) null else TextDecoration.Underline
                    )
                ) {
                    append(cleanWord)
                }
                pop()
            }
            if (index < rawWords.size - 1) {
                append("  ")
            }
        }
    }

    val tts = remember {
        TextToSpeech(context) { status -> }
    }

    Box(modifier = modifier) {
        SelectionContainer {
            androidx.compose.foundation.text.ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp, lineHeight = (fontSize + 10).sp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations("word", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            val word = annotation.item
                            if (isUnderscoreWord(word)) return@let
                            selectedWord = word
                            popupVisible = true
                            isTranslating = true

                            tts.language = Locale.US
                            tts.setSpeechRate(0.85f)
                            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_word")

                            scope.launch {
                                val result = translateWord(word)
                                translation = result
                                isTranslating = false
                            }
                        }
                }
            )
        }

        if (popupVisible && selectedWord != null) {
            Popup(
                alignment = Alignment.BottomCenter,
                onDismissRequest = { popupVisible = false; selectedWord = null; translation = null },
                properties = PopupProperties(focusable = false)
            ) {
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedWord!!,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            if (isTranslating) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(12.dp).width(12.dp),
                                        strokeWidth = 2.dp,
                                        color = Primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            } else if (translation != null) {
                                Text(
                                    translation!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                tts.language = Locale.US
                                tts.setSpeechRate(0.85f)
                                tts.speak(selectedWord, TextToSpeech.QUEUE_FLUSH, null, "tts_word2")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { popupVisible = false; selectedWord = null; translation = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

suspend fun translateWord(word: String): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("text", word)
            put("sourceLang", "en")
            put("targetLang", "es")
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${ApiConfig.BASE_URL}translate")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseJson = JSONObject(response.body?.string() ?: "")
            responseJson.optString("translation", word)
        } else {
            word
        }
    } catch (_: Exception) {
        word
    }
}