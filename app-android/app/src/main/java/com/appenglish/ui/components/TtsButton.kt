package com.appenglish.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appenglish.util.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

fun cleanForTts(text: String): String {
    return text
        .replace("______", "...")
        .replace("________________", "...")
        .replace("___", "...")
        .replace("__", "...")
        .replace(Regex("_+"), "...")
        .replace(Regex("\\.{3,}"), "...")
}

@Composable
fun TtsButton(
    text: String,
    modifier: Modifier = Modifier,
    language: String = "en"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
            }
        }
    }

    IconButton(
        onClick = {
            if (isReady) {
                isPlaying = true
                scope.launch {
                    try {
                        val cleanText = cleanForTts(text)
                        val result = tts.setLanguage(if (language == "en") Locale.US else Locale.getDefault())
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            playViaVps(context, text)
                        } else {
                            tts.setSpeechRate(0.85f)
                            speakParts(tts, text) { playing ->
                                if (!playing) isPlaying = false
                            }
                        }
                    } catch (_: Exception) {
                        scope.launch {
                            playViaVps(context, text)
                            isPlaying = false
                        }
                    }
                }
            }
        },
        modifier = modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF4CAF50))
    ) {
        if (isPlaying) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF4CAF50),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "Escuchar pronunciación",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun speakParts(tts: TextToSpeech, text: String, onDone: (Boolean) -> Unit) {
    val parts = text.split(Regex("_{2,}"))
    val cleanParts = parts.map { it.trim() }.filter { it.isNotEmpty() }

    if (cleanParts.isEmpty()) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_single")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone(false) }
            override fun onError(utteranceId: String?) { onDone(false) }
        })
        return
    }

    val playQueue = mutableListOf<String>()
    for (i in cleanParts.indices) {
        playQueue.add(cleanParts[i])
        if (i < cleanParts.size - 1) {
            playQueue.add("__PAUSE__")
        }
    }

    fun playNext(index: Int) {
        if (index >= playQueue.size) {
            onDone(false)
            return
        }
        val item = playQueue[index]
        if (item == "__PAUSE__") {
            val pause = tts.playSilentUtterance(400, TextToSpeech.QUEUE_FLUSH, "pause_$index")
            if (pause == TextToSpeech.SUCCESS) {
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        playNext(index + 1)
                    }
                    override fun onError(utteranceId: String?) {
                        playNext(index + 1)
                    }
                })
            } else {
                playNext(index + 1)
            }
        } else {
            tts.speak(item, TextToSpeech.QUEUE_FLUSH, null, "part_$index")
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    playNext(index + 1)
                }
                override fun onError(utteranceId: String?) {
                    playNext(index + 1)
                }
            })
        }
    }

    playNext(0)
}

suspend fun playViaVps(context: Context, text: String) {
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("text", text)
                put("voice", "en-US-JennyNeural")
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}tts")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext

            val audioBytes = response.body?.bytes() ?: return@withContext
            val tempFile = File(context.cacheDir, "tts_vps_${System.currentTimeMillis()}.mp3")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { release() }
                }
            }
        } catch (_: Exception) { }
    }
}
