package com.appenglish.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appenglish.data.remote.api.AuthInterceptor
import com.appenglish.ui.components.BottomNavBar
import com.appenglish.ui.components.BottomNavTab
import com.appenglish.ui.screens.login.LoginScreen
import com.appenglish.ui.screens.subjects.SubjectsScreen
import com.appenglish.ui.screens.topics.TopicsScreen
import com.appenglish.ui.screens.units.UnitsScreen
import com.appenglish.ui.screens.blocks.BlocksScreen
import com.appenglish.ui.screens.exercise.UnitExerciseScreen
import com.appenglish.ui.screens.test.FinalTestScreen
import com.appenglish.ui.screens.dictionary.DictionaryScreen
import com.appenglish.ui.screens.progress.ProgressScreen
import com.appenglish.ui.screens.settings.SettingsScreen
import com.appenglish.ui.screens.theory.TheoryScreen

object Routes {
    const val LOGIN = "login"
    const val SUBJECTS = "subjects"
    const val TOPICS_LIST = "topics/{subjectId}"
    const val UNITS_LIST = "units/{topicId}"
    const val BLOCKS = "blocks/{unitId}"
    const val EXERCISE = "exercise/{topicId}/{unitId}"
    const val TEST = "test/{topicId}"
    const val THEORY = "theory/{topicId}"
    const val UNIT_THEORY = "unit-theory/{topicId}/{unitId}"
    const val DICTIONARY = "dictionary"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    fun topicsList(subjectId: String) = "topics/$subjectId"
    fun unitsList(topicId: String) = "units/$topicId"
    fun blocks(unitId: String) = "blocks/$unitId"
    fun exercise(topicId: String, unitId: String) = "exercise/$topicId/$unitId"
    fun test(topicId: String) = "test/$topicId"
    fun theory(topicId: String) = "theory/$topicId"
    fun unitTheory(topicId: String, unitId: String) = "unit-theory/$topicId/$unitId"
}

private fun getCurrentTab(route: String?): BottomNavTab {
    return when {
        route == null -> BottomNavTab.HOME
        route == Routes.SUBJECTS || route.startsWith("topics/") || route.startsWith("units/") || route.startsWith("blocks/") || route.startsWith("exercise/") || route.startsWith("test/") || route.startsWith("theory/") || route.startsWith("unit-theory/") -> BottomNavTab.HOME
        route == Routes.DICTIONARY -> BottomNavTab.DICTIONARY
        route == Routes.PROGRESS -> BottomNavTab.PROGRESS
        route == Routes.SETTINGS -> BottomNavTab.SETTINGS
        else -> BottomNavTab.HOME
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // Check for existing session on first composition
    val hasSession = remember {
        AuthInterceptor.loadSession(context)
    }

    val hideBottomBar = currentRoute in listOf(Routes.LOGIN, Routes.EXERCISE, Routes.TEST, null)

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                BottomNavBar(
                    currentTab = getCurrentTab(currentRoute),
                    onTabClick = { tab ->
                        when (tab) {
                            BottomNavTab.HOME -> {
                                navController.navigate(Routes.SUBJECTS) {
                                    popUpTo(Routes.SUBJECTS) { inclusive = true }
                                }
                            }
                            BottomNavTab.DICTIONARY -> {
                                navController.navigate(Routes.DICTIONARY) {
                                    popUpTo(Routes.SUBJECTS)
                                }
                            }
                            BottomNavTab.PROGRESS -> {
                                navController.navigate(Routes.PROGRESS) {
                                    popUpTo(Routes.SUBJECTS)
                                }
                            }
                            BottomNavTab.SETTINGS -> {
                                navController.navigate(Routes.SETTINGS) {
                                    popUpTo(Routes.SUBJECTS)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (hasSession) Routes.SUBJECTS else Routes.LOGIN,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.SUBJECTS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SUBJECTS) {
                SubjectsScreen(
                    onSubjectClick = { subjectId -> navController.navigate(Routes.topicsList(subjectId)) }
                )
            }

            composable(Routes.TOPICS_LIST, arguments = listOf(navArgument("subjectId") { type = NavType.StringType })) {
                TopicsScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicClick = { topicId -> navController.navigate(Routes.unitsList(topicId)) }
                )
            }

            composable(Routes.UNITS_LIST, arguments = listOf(navArgument("topicId") { type = NavType.StringType })) {
                UnitsScreen(
                    onBackClick = { navController.popBackStack() },
                    onUnitClick = { _, unitId -> navController.navigate(Routes.blocks(unitId)) },
                    onTestClick = { topicId -> navController.navigate(Routes.test(topicId)) }
                )
            }

            composable(Routes.BLOCKS, arguments = listOf(navArgument("unitId") { type = NavType.StringType })) {
                BlocksScreen(
                    onBackClick = { navController.popBackStack() },
                    onBlockClick = { topicId, unitId -> navController.navigate(Routes.exercise(topicId, unitId)) },
                    onTopicTheoryClick = { topicId -> navController.navigate(Routes.theory(topicId)) },
                    onUnitTheoryClick = { topicId, unitId -> navController.navigate(Routes.unitTheory(topicId, unitId)) }
                )
            }

            composable(Routes.THEORY, arguments = listOf(navArgument("topicId") { type = NavType.StringType })) {
                TheoryScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.UNIT_THEORY, arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("unitId") { type = NavType.StringType }
            )) {
                TheoryScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.EXERCISE, arguments = listOf(navArgument("topicId") { type = NavType.StringType }, navArgument("unitId") { type = NavType.StringType })) {
                UnitExerciseScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.TEST, arguments = listOf(navArgument("topicId") { type = NavType.StringType })) {
                FinalTestScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.DICTIONARY) {
                DictionaryScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.PROGRESS) {
                ProgressScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onLogout = {
                        AuthInterceptor.clearSession(navController.context)
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }
    }
}
