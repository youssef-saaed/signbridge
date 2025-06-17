package com.yousefsaid04.aslcommunicator.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yousefsaid04.aslcommunicator.ui.features.sign_to_text.SignToTextScreen
import com.yousefsaid04.aslcommunicator.ui.features.speech_to_text.SpeechToTextScreen
import com.yousefsaid04.aslcommunicator.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class AutoPilotState { PASSIVE, ACTIVE, DISABLED }

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object SignToText : Screen("sign_to_text", "Sign Bridge", { Icon(Icons.Rounded.SignLanguage, contentDescription = null) })
    data object SpeechToText : Screen("speech_to_text", "Speech To Text", { Icon(Icons.Rounded.Hearing, contentDescription = null) })
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(Screen.SignToText, Screen.SpeechToText)
    val appViewModel: AppViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var autoPilotState by remember { mutableStateOf(AutoPilotState.PASSIVE) }

    val recognizedText by appViewModel.recognizedText.collectAsState()
    val isListening by appViewModel.isListening.collectAsState()

    // --- AUTO-PILOT LOGIC ---

    // Effect to navigate TO the speech screen based on recognized text
    LaunchedEffect(recognizedText, autoPilotState, currentDestination) {
        if (autoPilotState == AutoPilotState.PASSIVE && currentDestination?.route == Screen.SignToText.route) {
            val wordCount = recognizedText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            if (wordCount >= 2) {
                delay(500)
                autoPilotState = AutoPilotState.ACTIVE
                navController.navigate(Screen.SpeechToText.route)
            }
        }
    }

    // Effect to navigate BACK from the speech screen based on silence
    LaunchedEffect(isListening, autoPilotState, currentDestination) {
        if (autoPilotState == AutoPilotState.ACTIVE && currentDestination?.route == Screen.SpeechToText.route && !isListening) {
            delay(3000)
            if (autoPilotState == AutoPilotState.ACTIVE && navController.currentDestination?.route == Screen.SpeechToText.route) {
                autoPilotState = AutoPilotState.PASSIVE
                navController.navigate(Screen.SignToText.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                }
                appViewModel.startListening()
            }
        }
    }

    // THE FINAL FIX: Keep-Alive timer to prevent the listener from going stale
    LaunchedEffect(autoPilotState, currentDestination) {
        // This timer ONLY runs when auto-pilot is PASSIVE on the Sign screen
        if (autoPilotState == AutoPilotState.PASSIVE && currentDestination?.route == Screen.SignToText.route) {
            while (isActive) { // This loop will automatically be cancelled when the Effect leaves the screen
                delay(1_000L) // Wait for 20 seconds
                // Nudge the listener to keep it fresh
                appViewModel.startListening()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon() },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            when (screen) {
                                Screen.SpeechToText -> autoPilotState = AutoPilotState.DISABLED
                                Screen.SignToText -> autoPilotState = AutoPilotState.PASSIVE
                            }
                            appViewModel.startListening() // Always restart listener on manual nav
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            composable(Screen.SpeechToText.route) { SpeechToTextScreen(appViewModel = appViewModel) }
        }
    }
}