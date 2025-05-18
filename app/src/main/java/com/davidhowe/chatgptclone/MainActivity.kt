package com.davidhowe.chatgptclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.davidhowe.chatgptclone.nav.BaseNavHost
import com.davidhowe.chatgptclone.ui.theme.ChatGPTCloneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatGPTCloneTheme {
                val navController = rememberNavController()
                BaseNavHost(navController)
            }
        }
    }
}