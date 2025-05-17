package com.davidhowe.chatgptclone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

@HiltAndroidApp
class ChatGPTCloneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            plant(DebugTree())
        } // todo release logs for analytics

    }
}
