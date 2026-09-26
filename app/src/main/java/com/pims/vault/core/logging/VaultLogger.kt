package com.pims.vault.core.logging

import android.util.Log

object VaultLogger {
    const val DEFAULT_TAG = "Vault"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            println("[$tag] DEBUG: $message")
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: Throwable) {
            println("[$tag] INFO: $message")
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String, tr: Throwable? = null) {
        try {
            if (tr != null) {
                Log.w(tag, message, tr)
            } else {
                Log.w(tag, message)
            }
        } catch (_: Throwable) {
            println("[$tag] WARN: $message")
            tr?.printStackTrace()
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, tr: Throwable? = null) {
        try {
            if (tr != null) {
                Log.e(tag, message, tr)
            } else {
                Log.e(tag, message)
            }
        } catch (_: Throwable) {
            System.err.println("[$tag] ERROR: $message")
            tr?.printStackTrace()
        }
    }

    fun setUserId(userId: String) {
        try {
            Log.d(DEFAULT_TAG, "User ID set: $userId")
        } catch (_: Throwable) {}
    }

    fun clearUserId() {
        try {
            Log.d(DEFAULT_TAG, "User ID cleared")
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: String) {
        try {
            Log.d(DEFAULT_TAG, "Custom key $key=$value")
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: Int) {
        try {
            Log.d(DEFAULT_TAG, "Custom key $key=$value")
        } catch (_: Throwable) {}
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            Log.d(DEFAULT_TAG, "Custom key $key=$value")
        } catch (_: Throwable) {}
    }
}
