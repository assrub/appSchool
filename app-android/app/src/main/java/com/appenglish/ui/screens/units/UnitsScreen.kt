package com.appenglish.ui.screens.units

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.appenglish.domain.model.Unit as DomainUnit
import com.appenglish.ui.theme.CorrectBackground
import com.appenglish.ui.theme.CorrectGreen
import com.appenglish.ui.theme.Primary
import com.appenglish.ui.theme.SurfaceVariant
import com.appenglish.ui.theme.WarningOrange

private fun exerciseTypeLabel(type: String): String = when (type) {
    "fill-blank" -> "Completar"
    "multiple-choice" -> "Opción múltiple"
    "true-false" -> "Verdadero/Falso"
    "reorder" -> "Ordenar"
    "matching" -> "Emparejar"
    "listening" -> "Escuchar"
    else -> type
}

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
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasTheory = true
    val tabs = listOf("UNIDADES", "TEORÍA")
    val selectedTab = if (hasTheory) uiState.selectedTab else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.topicName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Volver",
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                selectedTab == 0 || !hasTheory -> {
                    var unitToRedo by remember { mutableStateOf<String?>(null) }

                    UnitsListContent(
                        units = uiState.units,
                        onUnitClick = onUnitClick,
                        onTestClick = { onTestClick(uiState.topicId) },
                        onRedoClick = { unitToRedo = it }
                    )

                    unitToRedo?.let { unitId ->
                        AlertDialog(
                            onDismissRequest = { unitToRedo = null },
                            title = { Text("Rehacer unidad") },
                            text = { Text("¿Querés reiniciar esta unidad? Se perderá el progreso actual.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.resetUnitProgress(unitId)
                                        unitToRedo = null
                                    }
                                ) {
                                    Text("Rehacer", color = WarningOrange)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { unitToRedo = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
                selectedTab == 1 && hasTheory -> {
                    TheoryTabContent(
                        theory = uiState.topicTheory!!
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitsListContent(
    units: List<DomainUnit>,
    onUnitClick: (String, String) -> Unit,
    onTestClick: () -> Unit,
    onRedoClick: (String) -> Unit = {}
) {
    if (units.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(16.dp))
                Text(
                    "No hay unidades disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val allComplete = units.all { it.percent >= 100.0 }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            itemsIndexed(units) { _, u ->
                val displayIcon = u.icon
                val isApproved = u.percent >= 70.0 && u.percent < 100.0
                val isCompleted = u.percent >= 100.0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!u.isLocked) Modifier.clickable { onUnitClick(u.id, u.id) }
                            else Modifier
                        ),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> CorrectBackground
                            isApproved -> Color(0xFFE8F5E9)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(displayIcon, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                u.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                exerciseTypeLabel(u.exerciseType),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (u.percent / 100.0).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = when {
                                    isCompleted -> CorrectGreen
                                    isApproved -> Color(0xFF4CAF50)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${u.percent.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isCompleted -> CorrectGreen
                                        isApproved -> Color(0xFF4CAF50)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${u.completedItems}/${u.totalItems} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = when {
                                u.isLocked -> Icons.Default.Lock
                                isCompleted -> Icons.Default.CheckCircle
                                isApproved -> Icons.Default.CheckCircle
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = when {
                                u.isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                                isCompleted -> CorrectGreen
                                isApproved -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        if (isApproved || isCompleted) {
                            Spacer(Modifier.width(4.dp))
                            TextButton(
                                onClick = { onRedoClick(u.id) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Rehacer", style = MaterialTheme.typography.labelSmall, color = WarningOrange)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (allComplete) Modifier.clickable { onTestClick() }
                            else Modifier
                        ),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allComplete) CorrectBackground else SurfaceVariant
                    )
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            tint = if (allComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(28.dp).width(28.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Test Final",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (allComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (allComplete) "¡Listo!" else "Completá todas las unidades",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (allComplete) Icons.Default.PlayArrow else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (allComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TheoryTabContent(theory: com.appenglish.domain.model.Theory) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (theory.text.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📖 Teoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        theory.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (theory.tips.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "💡 Tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                    Spacer(Modifier.height(8.dp))
                    theory.tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tip.emoji, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(tip.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (theory.text.isBlank() && theory.tips.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No hay teoría disponible para este tema",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}