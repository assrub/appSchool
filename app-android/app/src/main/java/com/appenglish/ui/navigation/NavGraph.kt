package com.appenglish.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

object Routes {
    const val LOGIN = "login"
    const val SUBJECTS = "subjects"
    const val TOPICS_LIST = "topics/{subjectId}"
    const val UNITS_LIST = "units/{topicId}"
    const val BLOCKS = "blocks/{unitId}"
    const val EXERCISE = "exercise/{topicId}/{unitId}"
    const val TEST = "test/{topicId}"
    const val DICTIONARY = "dictionary"
    const val PROGRESS = "progress"

    fun topicsList(subjectId: String) = "topics/$subjectId"
    fun unitsList(topicId: String) = "units/$topicId"
    fun blocks(unitId: String) = "blocks/$unitId"
    fun exercise(topicId: String, unitId: String) = "exercise/$topicId/$unitId"
    fun test(topicId: String) = "test/$topicId"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBottomBar = currentRoute in listOf(Routes.LOGIN, null)

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                BottomNavBar(
                    currentTab = when {
                        currentRoute in listOf(Routes.SUBJECTS, Routes.TOPICS_LIST, Routes.UNITS_LIST, Routes.BLOCKS, Routes.EXERCISE, Routes.TEST) -> BottomNavTab.HOME
                        currentRoute == Routes.DICTIONARY -> BottomNavTab.DICTIONARY
                        currentRoute == Routes.PROGRESS -> BottomNavTab.PROGRESS
                        else -> BottomNavTab.HOME
                    },
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
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(padding)
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
                    onSubjectClick = { subjectId -> navController.navigate(Routes.topicsList(subjectId)) },
                    onDictionaryClick = { navController.navigate(Routes.DICTIONARY) },
                    onProgressClick = { navController.navigate(Routes.PROGRESS) },
                    onLogout = {
                        AuthInterceptor.clearSession(navController.context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
                    onUnitClick = { topicId, unitId -> navController.navigate(Routes.blocks(unitId)) },
                    onTestClick = { topicId -> navController.navigate(Routes.test(topicId)) }
                )
            }

            composable(Routes.BLOCKS, arguments = listOf(navArgument("unitId") { type = NavType.StringType })) {
                BlocksScreen(
                    onBackClick = { navController.popBackStack() },
                    onBlockClick = { topicId, unitId -> navController.navigate(Routes.exercise(topicId, unitId)) }
                )
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
        }
    }
}
