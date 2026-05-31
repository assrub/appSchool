package com.appenglish.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appenglish.ui.screens.subjects.SubjectsScreen
import com.appenglish.ui.screens.topic.TopicScreen
import com.appenglish.ui.screens.exercise.UnitExerciseScreen
import com.appenglish.ui.screens.test.FinalTestScreen
import com.appenglish.ui.screens.dictionary.DictionaryScreen
import com.appenglish.ui.screens.progress.ProgressScreen

object Routes {
    const val SUBJECTS = "subjects"
    const val TOPIC = "topic/{topicId}"
    const val UNIT = "unit/{topicId}/{unitId}"
    const val TEST = "test/{topicId}"
    const val DICTIONARY = "dictionary"
    const val PROGRESS = "progress"

    fun topic(topicId: String) = "topic/$topicId"
    fun unit(topicId: String, unitId: String) = "unit/$topicId/$unitId"
    fun test(topicId: String) = "test/$topicId"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SUBJECTS
    ) {
        composable(Routes.SUBJECTS) {
            SubjectsScreen(
                onTopicClick = { subjectId, topicId ->
                    navController.navigate(Routes.topic(topicId))
                },
                onDictionaryClick = {
                    navController.navigate(Routes.DICTIONARY)
                },
                onProgressClick = {
                    navController.navigate(Routes.PROGRESS)
                }
            )
        }

        composable(
            route = Routes.TOPIC,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) {
            TopicScreen(
                onBackClick = { navController.popBackStack() },
                onUnitClick = { topicId, unitId ->
                    navController.navigate(Routes.unit(topicId, unitId))
                },
                onTestClick = { topicId ->
                    navController.navigate(Routes.test(topicId))
                }
            )
        }

        composable(
            route = Routes.UNIT,
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
            FinalTestScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.DICTIONARY) {
            DictionaryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.PROGRESS) {
            ProgressScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
