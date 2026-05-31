package com.appenglish.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appenglish.ui.screens.login.LoginScreen
import com.appenglish.ui.screens.subjects.SubjectsScreen
import com.appenglish.ui.screens.topics.TopicsScreen
import com.appenglish.ui.screens.units.UnitsScreen
import com.appenglish.ui.screens.blocks.BlocksScreen
import com.appenglish.ui.screens.topic.TopicScreen
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
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
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
                onSubjectClick = { subjectId ->
                    navController.navigate(Routes.topicsList(subjectId))
                },
                onDictionaryClick = { navController.navigate(Routes.DICTIONARY) },
                onProgressClick = { navController.navigate(Routes.PROGRESS) }
            )
        }

        composable(
            route = Routes.TOPICS_LIST,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) {
            TopicsScreen(
                onBackClick = { navController.popBackStack() },
                onTopicClick = { topicId ->
                    navController.navigate(Routes.unitsList(topicId))
                }
            )
        }

        composable(
            route = Routes.UNITS_LIST,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) {
            UnitsScreen(
                onBackClick = { navController.popBackStack() },
                onUnitClick = { topicId, unitId ->
                    navController.navigate(Routes.blocks(unitId))
                },
                onTestClick = { topicId ->
                    navController.navigate(Routes.test(topicId))
                }
            )
        }

        composable(
            route = Routes.BLOCKS,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            BlocksScreen(
                onBackClick = { navController.popBackStack() },
        onBlockClick = { topicId, unitId ->
            navController.navigate(Routes.exercise(topicId, unitId))
        }
            )
        }

        composable(
            route = Routes.EXERCISE,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("unitId") { type = NavType.StringType }
            )
        ) {
            UnitExerciseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.TEST,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) {
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
