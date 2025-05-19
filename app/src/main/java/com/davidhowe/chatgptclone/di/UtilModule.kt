package com.davidhowe.chatgptclone.di

import android.content.Context
import android.content.SharedPreferences
import com.davidhowe.chatgptclone.AIConfigModel
import com.davidhowe.chatgptclone.AIConfigTemp
import com.davidhowe.chatgptclone.data.preferences.GptClonePreferences
import com.davidhowe.chatgptclone.util.AudioRecorderUtil
import com.davidhowe.chatgptclone.util.DeviceUtil
import com.davidhowe.chatgptclone.util.ResourceUtil
import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.HarmBlockThreshold
import com.google.firebase.vertexai.type.HarmCategory
import com.google.firebase.vertexai.type.SafetySetting
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object UtilModule {

    @Singleton
    @Provides
    fun provideResourceUtil(@ApplicationContext context: Context): ResourceUtil {
        return ResourceUtil(context)
    }

    @Singleton
    @Provides
    fun provideDeviceUtil(@ApplicationContext context: Context): DeviceUtil {
        return DeviceUtil(context)
    }

    @Singleton
    @Provides
    fun provideAudioRecorderUtil(@ApplicationContext context: Context): AudioRecorderUtil {
        return AudioRecorderUtil(context)
    }

    @Singleton
    @Provides
    fun provideGenerativeModel(): GenerativeModel {
        return Firebase.vertexAI.generativeModel(
            modelName = AIConfigModel,
            generationConfig = generationConfig {
                temperature = AIConfigTemp
            },
            safetySettings = listOf(
                SafetySetting(
                    harmCategory = HarmCategory.DANGEROUS_CONTENT,
                    threshold = HarmBlockThreshold.ONLY_HIGH
                )
            )
        )
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext appContext: Context): SharedPreferences {
        return appContext.getSharedPreferences("gpt_clone_prefs", Context.MODE_PRIVATE)
    }

    @Singleton
    @Provides
    fun provideGptClonePreferences(sharedPrefs: SharedPreferences): GptClonePreferences {
        return GptClonePreferences(sharedPrefs)
    }
}
