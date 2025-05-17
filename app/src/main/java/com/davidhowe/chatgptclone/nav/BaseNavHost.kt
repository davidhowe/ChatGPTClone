package com.davidhowe.chatgptclone.nav

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.davidhowe.chatgptclone.ui.speechChat.SpeechChatScreen
import com.davidhowe.chatgptclone.ui.textChat.TextChatScreen

@Composable
fun BaseNavHost(
    baseNavController: NavController,
) {
    val navHostController = baseNavController as NavHostController
    NavHost(navController = navHostController, startDestination = NavDirections.textChat.route) {
        composable(NavDirections.textChat.route) {
            TextChatScreen(
                viewModel = hiltViewModel(), navHostController = navHostController
            )
        }
        composable(NavDirections.speechChat.route) {
            SpeechChatScreen(
                viewModel = hiltViewModel(), navHostController = navHostController
            )
        }
    }
}
