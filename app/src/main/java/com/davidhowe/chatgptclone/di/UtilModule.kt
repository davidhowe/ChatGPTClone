package com.davidhowe.chatgptclone.di

import android.content.Context
import com.davidhowe.chatgptclone.util.DeviceUtil
import com.davidhowe.chatgptclone.util.ResourceUtil
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
}
