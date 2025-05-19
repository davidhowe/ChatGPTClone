package com.davidhowe.chatgptclone.ui.textchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain
import com.davidhowe.chatgptclone.util.StringFunction
import com.davidhowe.chatgptclone.util.VoidFunction
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextChatNavDrawer(
    title: String,
    onNewChatClicked: VoidFunction,
    onSearchTextChanged: StringFunction,
    onChatClicked: (ChatSummaryDomain) -> Unit,
    chatList: List<ChatSummaryDomain>,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(Modifier.height(12.dp))
                    Row {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.8f),
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                onSearchTextChanged.invoke(it)
                            },
                            label = { Text("Search") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search Icon"
                                )
                            },
                            trailingIcon = { // Add trailingIcon
                                if (searchText.isNotEmpty()) { // Only show if text is not empty
                                    IconButton(onClick = {
                                        searchText = ""
                                        onSearchTextChanged.invoke("")
                                        Timber.d("Search Text Cleared")
                                    }) { // Clear text on click
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                        )
                        IconButton(
                            modifier = Modifier
                                .weight(0.2f),
                            onClick = {
                                onNewChatClicked.invoke()
                                scope.launch {
                                    drawerState.close()
                                }
                            }) { // Clear text on click
                            Icon(
                                Icons.Filled.Create,
                                contentDescription = "Create chat"
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    items(chatList.size) { index ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .animateItem()
                                .clickable {
                                    onChatClicked.invoke(chatList[index])
                                    scope.launch {
                                        drawerState.close()
                                    }
                                },
                            horizontalAlignment = Alignment.Start
                        ) {
                            val titleText = chatList[index].title
                            if (titleText.length > 20) titleText.substring(0, 20) + "..." else titleText
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = chatList[index].content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}