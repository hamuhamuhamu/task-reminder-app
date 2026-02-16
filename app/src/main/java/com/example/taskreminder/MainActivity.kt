package com.example.taskreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskreminder.ui.AppViewModel
import com.example.taskreminder.ui.HomeScreen
import com.example.taskreminder.ui.Routes
import com.example.taskreminder.ui.SettingsScreen
import com.example.taskreminder.ui.TaskDetailScreen

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppNav(viewModel = viewModel)
        }
    }
}

@Composable
private fun AppNav(viewModel: AppViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Routes.TASK_DETAIL}/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            TaskDetailScreen(taskId = taskId, viewModel = viewModel)
        }
    }
}
