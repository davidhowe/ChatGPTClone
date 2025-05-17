package com.davidhowe.chatgptclone.nav

import androidx.navigation.NamedNavArgument
import com.davidhowe.chatgptclone.nav.AppRoutes.SPEECH_CHAT_ROUTE
import com.davidhowe.chatgptclone.nav.AppRoutes.TEXT_CHAT_ROUTE

object NavDirections {

    val textChat = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val route = TEXT_CHAT_ROUTE
    }

    val speechChat = object : NavigationCommand {
        override val arguments = emptyList<NamedNavArgument>()
        override val route = SPEECH_CHAT_ROUTE
    }
}

interface NavigationCommand {
    val arguments: List<NamedNavArgument>
    val route: String
}

object AppRoutes {
    const val TEXT_CHAT_ROUTE = "textChat"
    const val SPEECH_CHAT_ROUTE = "speechChat"
}