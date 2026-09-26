package com.pims.vault.core.logging

import android.util.Log

object VaultLogger {
    const val DEFAULT_TAG = "Vault"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, message)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.w(tag, message, tr)
        } else {
            Log.w(tag, message)
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e(tag, message, tr)
        } else {
            Log.e(tag, message)
        }
    }

    fun setUserId(userId: String) {
        Log.d(DEFAULT_TAG, "User ID set: $userId")
    }

    fun clearUserId() {
        Log.d(DEFAULT_TAG, "User ID cleared")
    }

    fun setCustomKey(key: String, value: String) {
        Log.d(DEFAULT_TAG, "Custom key $key=$value")
    }

    fun setCustomKey(key: String, value: Int) {
        Log.d(DEFAULT_TAG, "Custom key $key=$value")
    }

    fun setCustomKey(key: String, value: Boolean) {
        Log.d(DEFAULT_TAG, "Custom key $key=$value")
    }
}
