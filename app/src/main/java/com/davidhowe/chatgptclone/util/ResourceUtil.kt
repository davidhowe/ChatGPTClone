package com.davidhowe.chatgptclone.util

import android.content.Context
import android.graphics.drawable.Drawable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceUtil @Inject constructor(
    private val context: Context,
) {
    fun getString(id: Int): String {
        return context.resources.getString(id)
    }

    fun getString(id: Int, intPlaceHolder: Int): String {
        return String.format(context.resources.getString(id), intPlaceHolder)
    }

    fun getString(id: Int, vararg args: Any?): String {
        return String.format(context.resources.getString(id), *args)
    }

    fun getStringArray(id: Int): List<String> {
        return context.resources.getStringArray(id).toList()
    }

    fun getColorInt(id: Int): Int {
        return context.resources.getColor(id)
    }

    fun getDrawable(id: Int): Drawable {
        return context.resources.getDrawable(id)
    }
}
