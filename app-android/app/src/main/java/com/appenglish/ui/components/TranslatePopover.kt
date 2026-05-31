package com.appenglish.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenglish.ui.theme.Primary
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranslatePopover(
    text: String,
    onTranslateRequest: suspend (String) -> String,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    var showPopover by remember { mutableStateOf(false) }
    var translation by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        content(
            Modifier.combinedClickable(
                onClick = { },
                onLongClick = {
                    showPopover = true
                    scope.launch {
                        isTranslating = true
                        translation = onTranslateRequest(text)
                        isTranslating = false
                    }
                }
            )
        )

        if (showPopover && translation != null) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(translation!!, style = MaterialTheme.typography.bodyLarge, color = Primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tocar para cerrar",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.combinedClickable(
                            onClick = { showPopover = false }
                        )
                    )
                }
            }
        }

        if (showPopover && isTranslating) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "Traduciendo...",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
