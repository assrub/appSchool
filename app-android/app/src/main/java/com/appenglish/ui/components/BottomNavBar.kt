package com.appenglish.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appenglish.ui.theme.Primary

enum class BottomNavTab(val label: String) {
    HOME("Inicio"),
    DICTIONARY("Diccionario"),
    PROGRESS("Progreso"),
    SETTINGS("Ajustes")
}

@Composable
fun BottomNavBar(currentTab: BottomNavTab, onTabClick: (BottomNavTab) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = currentTab == BottomNavTab.HOME,
            onClick = { onTabClick(BottomNavTab.HOME) },
            icon = { Icon(Icons.Default.Home, "Inicio") },
            label = { Text("Inicio") },
            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Primary, selectedTextColor = Primary)
        )
        NavigationBarItem(
            selected = currentTab == BottomNavTab.DICTIONARY,
            onClick = { onTabClick(BottomNavTab.DICTIONARY) },
            icon = { Icon(Icons.Default.MenuBook, "Diccionario") },
            label = { Text("Diccionario") },
            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Primary, selectedTextColor = Primary)
        )
        NavigationBarItem(
            selected = currentTab == BottomNavTab.PROGRESS,
            onClick = { onTabClick(BottomNavTab.PROGRESS) },
            icon = { Icon(Icons.Default.BarChart, "Progreso") },
            label = { Text("Progreso") },
            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Primary, selectedTextColor = Primary)
        )
        NavigationBarItem(
            selected = currentTab == BottomNavTab.SETTINGS,
            onClick = { onTabClick(BottomNavTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, "Ajustes") },
            label = { Text("Ajustes") },
            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Primary, selectedTextColor = Primary)
        )
    }
}
