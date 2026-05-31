package com.appenglish.ui.screens.topic

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.appenglish.domain.model.UnitTheory
import com.appenglish.domain.model.Tip
import com.appenglish.ui.components.TtsButton
import com.appenglish.ui.theme.Primary

@Composable
fun UnitTheoryView(
    theory: UnitTheory,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📖", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Explicación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "OCULTAR ▲" else "VER ▼", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    val htmlContent = buildTheoryHtml(theory)
                    if (htmlContent.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
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
                                        "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>$THEORY_CSS</style></head><body>$htmlContent</body></html>",
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(360.dp)
                            )
                        }
                    }

                    // Fallback: render sections, tables, tips for old format
                    if (htmlContent.isBlank() && theory.sections.isNotEmpty()) {
                        theory.sections.forEach { section ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(section.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp))
                                    section.examples.forEach { ex ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("▸", color = Primary, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(ex, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp), color = Color(0xFF333333))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            TtsButton(text = ex)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (theory.headers.isNotEmpty() && theory.rows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tabla resumen:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(4.dp)) { theory.headers.forEach { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.weight(1f)) } }
                                theory.rows.forEach { row -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { row.forEach { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp), modifier = Modifier.weight(1f)) } } }
                            }
                        }
                    }

                    if (theory.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Trucos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                        theory.tips.forEach { tip ->
                            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(tip.emoji, fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Text(tip.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildTheoryHtml(theory: UnitTheory): String {
    val sb = StringBuilder()
    if (theory.blocks.isNotEmpty()) {
        theory.blocks.forEach { block ->
            if (!block.title.isNullOrBlank()) {
                sb.append("<h2>${block.title}</h2>")
            }
            sb.append(block.html ?: "")
        }
    } else if (theory.text.isNotBlank()) {
        sb.append(theory.text.replace("\n", "<br>"))
    }
    return sb.toString()
}

private val THEORY_CSS = """
    body { font-family: sans-serif; font-size: 17px; color: #333; padding: 8px; margin: 0; line-height: 1.5; }
    h1 { font-size: 1.5em; margin: 12px 0 8px; color: #4CAF50; }
    h2 { font-size: 1.25em; margin: 10px 0 6px; color: #4CAF50; }
    h3 { font-size: 1.1em; margin: 8px 0 4px; color: #4CAF50; }
    p { margin: 4px 0 8px; }
    table { border-collapse: collapse; width: 100%; margin: 8px 0; }
    th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; font-size: 0.85em; }
    th { background: #f5f5f5; font-weight: bold; }
    blockquote { border-left: 3px solid #4CAF50; margin: 8px 0; padding: 4px 12px; color: #666; }
    img { max-width: 100%; border-radius: 6px; margin: 8px 0; }
    ul, ol { padding-left: 20px; }
    a { color: #4CAF50; }
    mark { background: #FFF9C4; padding: 1px 4px; }
    .youtube-video { margin: 8px 0; }
    .youtube-video iframe { width: 100% !important; height: 180px !important; }
""".trimIndent()
