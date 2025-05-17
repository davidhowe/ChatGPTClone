package com.davidhowe.chatgptclone.util

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DeviceUtil @Inject constructor(
    private val context: Context
) {
    fun getDeviceName(): String {
        return Build.MODEL
    }

    fun isConnectedInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null && cm.getNetworkCapabilities(cm.activeNetwork) != null
    }
}
