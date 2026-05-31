package com.appenglish.ui.screens.theory

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.appenglish.data.remote.dto.TheoryBlockDto
import com.appenglish.data.remote.dto.TipDto
import com.appenglish.ui.theme.Primary
import com.appenglish.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryScreen(
    onBackClick: () -> Unit,
    viewModel: TheoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.topicName, fontWeight = FontWeight.Bold) },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.theoryBlocks.isEmpty() && uiState.theoryText.isBlank()) {
                    item {
                        EmptyTheoryState()
                    }
                } else {
                    itemsIndexed(uiState.theoryBlocks) { idx, block ->
                        TheoryBlockCard(
                            index = idx + 1,
                            title = block.title,
                            html = block.html
                        )
                    }

                    if (uiState.theoryText.isNotBlank()) {
                        item {
                            LegacyTheoryCard(text = uiState.theoryText)
                        }
                    }
                }

                if (uiState.tips.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("💡 Trucos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    itemsIndexed(uiState.tips) { _, tip ->
                        TipCard(emoji = tip.emoji, text = tip.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun TheoryBlockCard(index: Int, title: String?, html: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!title.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("📖", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            if (!html.isNullOrBlank()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            webViewClient = WebViewClient()
                            isVerticalScrollBarEnabled = false
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>$THEORY_CSS</style></head><body>$html</body></html>",
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }
        }
    }
}

@Composable
private fun LegacyTheoryCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📖 Teoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text.replace("\n", "\n\n"), style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp), color = Color(0xFF333333))
        }
    }
}

@Composable
private fun TipCard(emoji: String, text: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmptyTheoryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📚", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No hay teoría disponible", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
    }
}

private val THEORY_CSS = """
    body { font-family: 'Roboto', sans-serif; font-size: 15px; color: #333; padding: 0; margin: 0; line-height: 1.6; }
    h1 { font-size: 1.3em; margin: 12px 0 8px; color: #388E3C; border-bottom: 2px solid #C8E6C9; padding-bottom: 4px; }
    h2 { font-size: 1.15em; margin: 10px 0 6px; color: #4CAF50; font-weight: bold; }
    h3 { font-size: 1.05em; margin: 8px 0 4px; color: #4CAF50; }
    p { margin: 4px 0 8px; }
    strong { color: #2E7D32; }
    table { border-collapse: collapse; width: 100%; margin: 8px 0; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    th { background: #4CAF50; color: white; padding: 8px 10px; text-align: left; font-size: 13px; font-weight: bold; }
    td { padding: 6px 10px; border-bottom: 1px solid #E8F5E9; font-size: 13px; }
    tr:nth-child(even) td { background: #F1F8E9; }
    tr:last-child td { border-bottom: none; }
    blockquote { border-left: 4px solid #4CAF50; margin: 8px 0; padding: 6px 12px; color: #555; background: #F1F8E9; border-radius: 0 6px 6px 0; font-style: italic; }
    img { max-width: 100%; border-radius: 6px; margin: 8px 0; }
    ul, ol { padding-left: 20px; }
    li { margin: 4px 0; }
    mark { background: #FFF9C4; padding: 1px 4px; border-radius: 3px; color: #333; }
""".trimIndent()