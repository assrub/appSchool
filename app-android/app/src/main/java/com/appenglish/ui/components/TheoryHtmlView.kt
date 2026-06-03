package com.appenglish.ui.components

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TheoryHtmlView(
    html: String,
    modifier: Modifier = Modifier
) {
    var webViewHeight by remember { mutableIntStateOf(300) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(AndroidColor.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(
                            "(function() { return document.body.scrollHeight; })()"
                        ) { height ->
                            height.toIntOrNull()?.let { h ->
                                webViewHeight = (h * 1.1).toInt() // Add 10% padding
                            }
                        }
                    }
                }
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
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (webViewHeight > 0) Modifier.fillMaxWidth()
                else Modifier.fillMaxWidth()
            )
    )
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
