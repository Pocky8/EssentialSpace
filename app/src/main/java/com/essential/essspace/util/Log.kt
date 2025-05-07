package com.essential.essspace.util

import android.util.Log

object DebugLog {
    private const val TAG = "EssSpaceDebug"

    fun log(message: String) {
        Log.d(TAG, message)
    }

    fun error(message: String, exception: Throwable? = null) {
        Log.e(TAG, message, exception)
    }
}