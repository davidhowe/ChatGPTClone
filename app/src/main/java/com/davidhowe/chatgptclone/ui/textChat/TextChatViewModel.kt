package com.davidhowe.chatgptclone.ui.textChat

import androidx.lifecycle.ViewModel
import com.davidhowe.chatgptclone.util.ResourceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TextChatViewModel @Inject constructor(
    private val resourceUtil: ResourceUtil,
) : ViewModel() {


}