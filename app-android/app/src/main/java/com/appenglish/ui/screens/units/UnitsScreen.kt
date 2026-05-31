package com.appenglish.ui.screens.units

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appenglish.domain.model.Unit as DomainUnit
import com.appenglish.ui.theme.CorrectGreen
import com.appenglish.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsScreen(
    onBackClick: () -> Unit,
    onUnitClick: (String, String) -> Unit,
    onTestClick: (String) -> Unit,
    viewModel: UnitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadUnits()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            val allComplete = uiState.units.all { it.percent >= 100.0 }
            val icons = listOf("✏️", "❌", "❓", "✅")

            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(uiState.units.size) { idx ->
                    val u = uiState.units[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .then(if (!u.isLocked) Modifier.clickable { onUnitClick(u.id, u.id) } else Modifier),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = if (u.percent >= 100.0) Color(0xFFE8F5E9) else Color.White)
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(icons.getOrElse(idx) { "📝" }, fontSize = 28.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(u.exerciseType, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (u.percent / 100.0).toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = if (u.percent >= 100.0) CorrectGreen else Primary,
                                    trackColor = Color(0xFFE0E0E0)
                                )
                                Text("${u.completedItems}/${u.totalItems} items", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(
                                if (u.isLocked) "🔒" else if (u.percent >= 100.0) "✅" else "→",
                                fontSize = 22.sp,
                                color = if (u.isLocked) Color.Gray else if (u.percent >= 100.0) CorrectGreen else Primary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .then(if (allComplete) Modifier.clickable { onTestClick(uiState.units.firstOrNull()?.id ?: "") } else Modifier),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = if (allComplete) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🧪", fontSize = 28.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Test Final", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (allComplete) Primary else Color.Gray)
                                Text(if (allComplete) "¡Listo!" else "Completá todas las unidades", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(if (allComplete) "→" else "🔒", fontSize = 22.sp, color = if (allComplete) Primary else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
