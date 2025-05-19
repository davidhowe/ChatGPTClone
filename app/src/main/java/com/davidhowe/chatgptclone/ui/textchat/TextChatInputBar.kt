package com.davidhowe.chatgptclone.ui.textchat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.davidhowe.chatgptclone.R

@Composable
fun TextChatInputBar(
    modifier: Modifier,
    inputText: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onSpeechClick: () -> Unit
) {

    val focusRequester = remember { FocusRequester() }
    val lineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp()
    }
    val maxHeight = lineHeight * 10 + 24.dp // ~10 lines + padding

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        TextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .heightIn(min = 48.dp, max = maxHeight)
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(12.dp)
                ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            placeholder = {
                Text(
                    text = "Type your message...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Default, // Use Next to allow multi-line
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences
            ),
            /*keyboardActions = KeyboardActions(
                onNext = {
                    onSendClick.invoke()
                }
            ),*/
            maxLines = 10,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            visualTransformation = VisualTransformation.None,
            singleLine = false
        )

        IconButton(
            onClick = onSpeechClick,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                modifier = Modifier.size(54.dp),
                painter = painterResource(R.drawable.ic_speech_circle),
                contentDescription = "Speech",
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        IconButton(
            onClick = onSendClick,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                modifier = Modifier.size(54.dp),
                painter = painterResource(R.drawable.ic_send_circle),
                contentDescription = "Send",
                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.LightGray
            )
        }
    }
}