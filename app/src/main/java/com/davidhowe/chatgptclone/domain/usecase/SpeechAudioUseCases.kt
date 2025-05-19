package com.davidhowe.chatgptclone.domain.usecase

import android.util.Base64
import com.davidhowe.chatgptclone.BuildConfig
import com.davidhowe.chatgptclone.data.datasource.ChatLocalDataSource
import com.davidhowe.chatgptclone.data.datasource.MessageLocalDataSource
import com.davidhowe.chatgptclone.util.AudioRecorderUtil
import com.google.firebase.vertexai.GenerativeModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechAudioUseCases @Inject constructor(
    // TODO: Refactor this into individual use cases as app expands
    private val generativeModel: GenerativeModel,
    private val chatLocalDataSource: ChatLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource,
) {

    suspend fun synthesizeSpeech(text: String): ByteArray? {
        val url =
            "https://texttospeech.googleapis.com/v1/text:synthesize?key=${BuildConfig.TEXT_TO_SPEECH_API_KEY}"

        val requestBodyJson = """
        {
          "input": {
            "text": "$text"
          },
          "voice": {
            "languageCode": "en-US",
            "name": "en-US-Wavenet-D"
          },
          "audioConfig": {
            "audioEncoding": "MP3"
          }
        }
    """.trimIndent()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: return null)
            val audioContent = json.getString("audioContent")
            return Base64.decode(audioContent, Base64.DEFAULT)
        }
        return null
    }

}