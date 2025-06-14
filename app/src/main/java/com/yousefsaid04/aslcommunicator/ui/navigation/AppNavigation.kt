package com.yousefsaid04.aslcommunicator.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yousefsaid04.aslcommunicator.ui.features.sign_to_text.SignToTextScreen
import com.yousefsaid04.aslcommunicator.ui.features.speech_to_text.SpeechToTextScreen

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object SignToText : Screen("sign_to_text", "Sign to Text", { Icon(Icons.Rounded.SignLanguage, contentDescription = null) })
    object SpeechToText : Screen("speech_to_text", "Speech to Text", { Icon(Icons.Rounded.Hearing, contentDescription = null) })
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(Screen.SignToText, Screen.SpeechToText)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.SignToText.route, Modifier.padding(innerPadding)) {
            composable(Screen.SignToText.route) { SignToTextScreen() }
            composable(Screen.SpeechToText.route) { SpeechToTextScreen() }
        }
    }
}