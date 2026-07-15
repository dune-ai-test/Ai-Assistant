package com.midnight.assistant.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.midnight.assistant.ui.screens.ChatScreen
import com.midnight.assistant.ui.screens.HistoryScreen
import com.midnight.assistant.ui.screens.SettingsScreen
import com.midnight.assistant.viewmodel.ChatViewModel

private object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val viewModel: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.CHAT) {
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSessionSelected = { navController.popBackStack() }
            )
        }
    }
}
