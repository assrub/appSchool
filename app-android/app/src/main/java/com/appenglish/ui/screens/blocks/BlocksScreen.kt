package com.appenglish.ui.screens.blocks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.appenglish.domain.model.ExerciseBlock
import com.appenglish.domain.model.UnitTheory
import com.appenglish.ui.components.TheoryHtmlView
import com.appenglish.ui.theme.CorrectBackground
import com.appenglish.ui.theme.CorrectGreen
import com.appenglish.ui.theme.ErrorRed
import com.appenglish.ui.theme.IncorrectBackground
import com.appenglish.ui.theme.Primary
import com.appenglish.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksScreen(
    onBackClick: () -> Unit,
    onBlockClick: (String, String) -> Unit,
    onTopicTheoryClick: (String) -> Unit,
    onUnitTheoryClick: (String, String) -> Unit,
    viewModel: BlocksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasUnitTheory = true
    val tabs = listOf("BLOQUES", "TEORÍA")
    val selectedTab = if (hasUnitTheory) uiState.selectedTab else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.unitName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                selectedTab == 0 || !hasUnitTheory -> {
                    var clickBlockIdx by remember { mutableStateOf<Int?>(null) }

                    BlocksListContent(
                        blocks = uiState.blocks,
                        blockProgress = uiState.blockProgress,
                        topicId = uiState.topicId,
                        unitId = uiState.unitId,
                        onBlockClick = { blockIdx ->
                            clickBlockIdx = blockIdx
                        }
                    )

                    clickBlockIdx?.let { blockIdx ->
                        val bp = uiState.blockProgress[blockIdx]
                        val isComplete = bp?.completed == true
                        val wrongCount = if (isComplete && bp != null) (bp.totalItems - bp.score).coerceAtLeast(0) else 0

                        if (isComplete && wrongCount > 0) {
                            AlertDialog(
                                onDismissRequest = { clickBlockIdx = null },
                                title = { Text("Bloque con errores") },
                                text = { Text("¿Querés rehacer solo los $wrongCount errores o todo el bloque?") },
                                confirmButton = {
                                    TextButton(onClick = { clickBlockIdx = null; onBlockClick(uiState.topicId, uiState.unitId) }) {
                                        Text("Solo errores", color = WarningOrange)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        viewModel.redoBlock(blockIdx)
                                        clickBlockIdx = null
                                        onBlockClick(uiState.topicId, uiState.unitId)
                                    }) {
                                        Text("Rehacer todo", color = Primary)
                                    }
                                }
                            )
                        } else if (isComplete) {
                            AlertDialog(
                                onDismissRequest = { clickBlockIdx = null },
                                title = { Text("Bloque completado") },
                                text = { Text("¿Querés rehacer todo el bloque?") },
                                confirmButton = {
                                    TextButton(onClick = { clickBlockIdx = null; onBlockClick(uiState.topicId, uiState.unitId) }) {
                                        Text("Repetir", color = Primary)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        viewModel.redoBlock(blockIdx)
                                        clickBlockIdx = null
                                        onBlockClick(uiState.topicId, uiState.unitId)
                                    }) {
                                        Text("Rehacer desde 0", color = WarningOrange)
                                    }
                                }
                            )
                        } else {
                            onBlockClick(uiState.topicId, uiState.unitId)
                            clickBlockIdx = null
                        }
                    }
                }
                selectedTab == 1 && hasUnitTheory -> {
                    TheoryTabContent(theory = uiState.unitTheory!!)
                }
            }
        }
    }
}

@Composable
private fun BlocksListContent(
    blocks: List<ExerciseBlock>,
    blockProgress: Map<Int, BlockProgressData>,
    topicId: String,
    unitId: String,
    onBlockClick: (Int) -> Unit
) {
    if (blocks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay bloques", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            itemsIndexed(blocks) { idx, block ->
                val rawProgress = blockProgress[idx]
                val isComplete = rawProgress?.completed == true
                val hasStarted = rawProgress != null && (rawProgress.score > 0 || rawProgress.completed)
                
                // If completed, show 100%. Otherwise show accuracy (score/total)
                val percent = when {
                    isComplete -> 1f
                    rawProgress != null && rawProgress.totalItems > 0 -> rawProgress.score.toFloat() / rawProgress.totalItems
                    else -> 0f
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = percent,
                    animationSpec = tween(600),
                    label = "blockProgress"
                )

                val barColor = when {
                    isComplete -> CorrectGreen
                    animatedProgress >= 0.7f -> WarningOrange
                    else -> Primary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBlockClick(idx) },
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isComplete) CorrectBackground else Color.White
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(block.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${block.items.size} ejercicios",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (isComplete && (rawProgress?.totalItems ?: 0 - (rawProgress?.score ?: 0)) > 0) WarningOrange else barColor,
                            trackColor = barColor.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isComplete) {
                                val wrongCount = ((rawProgress?.totalItems ?: 0) - (rawProgress?.score ?: 0)).coerceAtLeast(0)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅ ", style = MaterialTheme.typography.labelSmall)
                                    Text("${rawProgress?.score ?: 0}", style = MaterialTheme.typography.labelSmall, color = CorrectGreen, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Text("❌ ", style = MaterialTheme.typography.labelSmall)
                                    Text("${wrongCount}", style = MaterialTheme.typography.labelSmall, color = ErrorRed, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "${((rawProgress?.score ?: 0).toFloat() / (rawProgress?.totalItems ?: 1).coerceAtLeast(1) * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CorrectGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (hasStarted) {
                                Text(
                                    "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = barColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "✅ ${rawProgress?.score ?: 0}  ❌ ${((rawProgress?.totalItems ?: 0) - (rawProgress?.score ?: 0)).coerceAtLeast(0)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "0%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "✅ 0  ❌ 0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun TheoryTabContent(
    theory: UnitTheory
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Teoría de la unidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (theory.blocks.isNotEmpty()) {
                    theory.blocks.forEach { block ->
                        if (!block.title.isNullOrBlank()) {
                            Text(
                                block.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        if (!block.html.isNullOrBlank()) {
                            TheoryHtmlView(html = block.html)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else if (theory.text.isNotBlank()) {
                    Text(theory.text, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("No hay teoría para esta unidad", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }

        if (theory.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WarningOrange)
                    Spacer(Modifier.height(8.dp))
                    theory.tips.forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tip.emoji, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(tip.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}