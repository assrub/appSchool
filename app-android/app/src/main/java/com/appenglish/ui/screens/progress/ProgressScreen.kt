package com.appenglish.ui.screens.progress

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appenglish.ui.theme.CorrectBackground
import com.appenglish.ui.theme.CorrectGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBackClick: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subjectProgress by viewModel.subjectProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Progreso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryCard(progressEntries = uiState.progressEntries)
                }

                if (subjectProgress.isEmpty()) {
                    item {
                        EmptyProgressState()
                    }
                } else {
                    items(subjectProgress) { subjectProgressItem ->
                        SubjectProgressCard(subjectProgressItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(progressEntries: List<com.appenglish.data.local.entity.ProgressEntity>) {
    val totalCompleted = progressEntries.filter { it.completed }.size
    val totalMastered = progressEntries.filter { it.status == "mastered" }.size
    val totalUnits = progressEntries.size
    val overallPercent = if (totalUnits > 0) (totalCompleted.toFloat() / totalUnits * 100).toInt() else 0
    val avgAccuracy = if (progressEntries.isNotEmpty()) progressEntries.map { it.accuracy }.average().toFloat() else 0f
    val avgMastery = if (progressEntries.isNotEmpty()) progressEntries.map { it.mastery }.average().toFloat() else 0f
    val totalTime = progressEntries.sumOf { it.timeSpentSeconds }

    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$overallPercent%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$totalCompleted de $totalUnits unidades completadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (totalUnits > 0) totalCompleted.toFloat() / totalUnits else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = CorrectGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            // New pedagogical metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(label = "Precisión", value = "${avgAccuracy.toInt()}%", color = Color(0xFF2196F3))
                MetricItem(label = "Dominio", value = "${avgMastery.toInt()}%", color = Color(0xFF9C27B0))
                MetricItem(label = "Dominados", value = "$totalMastered", color = CorrectGreen)
            }
            if (totalTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tiempo total: ${formatTime(totalTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        minutes > 0 -> "${minutes}min"
        else -> "${seconds}s"
    }
}

@Composable
private fun EmptyProgressState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📈", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                "Sin progreso registrado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "¡Comenzá a estudiar para ver tu progreso!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubjectProgressCard(subjectProgress: SubjectProgress) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(subjectProgress.subject.icon, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        subjectProgress.subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val completedTopics = subjectProgress.topics.count { tp ->
                        tp.units.isNotEmpty() && tp.units.all { it.progress?.completed == true }
                    }
                    Text(
                        "$completedTopics/${subjectProgress.topics.size} temas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider()
                    subjectProgress.topics.forEach { topicProgress ->
                        TopicProgressItem(topicProgress)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicProgressItem(topicProgress: TopicProgress) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(topicProgress.topic.icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    topicProgress.topic.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val completedUnits = topicProgress.units.count { it.progress?.completed == true }
                Text(
                    "$completedUnits/${topicProgress.units.size} unidades",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (topicProgress.units.isNotEmpty()) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 12.dp)) {
                topicProgress.units.forEach { unit ->
                    UnitProgressItem(unit)
                }
            }
        }
    }
}

@Composable
private fun UnitProgressItem(unit: UnitProgress) {
    val progress = unit.progress
    val isCompleted = progress?.completed == true
    val isMastered = progress?.status == "mastered"
    val avance = if (progress != null && progress.totalItems > 0) {
        (progress.itemsAttempted.toFloat() / progress.totalItems * 100).toInt()
    } else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                isMastered -> Icons.Default.CheckCircle
                isCompleted -> Icons.Default.CheckCircle
                else -> Icons.Default.Schedule
            },
            contentDescription = null,
            tint = when {
                isMastered -> Color(0xFF9C27B0) // Purple for mastered
                isCompleted -> CorrectGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                unit.unitTitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (avance > 0) FontWeight.Medium else FontWeight.Normal
            )
            if (progress != null && progress.itemsAttempted > 0) {
                LinearProgressIndicator(
                    progress = { progress.itemsAttempted.toFloat() / progress.totalItems.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = when {
                        isMastered -> Color(0xFF9C27B0)
                        isCompleted -> CorrectGreen
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Avance $avance%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Precisión ${progress.accuracy.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        "Dominio ${progress.mastery.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (progress != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${progress.score}/${progress.totalItems}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isMastered -> Color(0xFF9C27B0)
                        isCompleted -> CorrectGreen
                        else -> MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        isMastered -> "DOMINADO"
                        isCompleted -> "COMPLETADO"
                        progress.status == "in_progress" -> "EN PROGRESO"
                        else -> "NO INICIADO"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isMastered -> Color(0xFF9C27B0)
                        isCompleted -> CorrectGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}