package com.pims.vault.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object VaultLogger {
    const val DEFAULT_TAG = "Vault"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, message)
        runCatching {
            FirebaseCrashlytics.getInstance().log("[$tag] DEBUG: $message")
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
        runCatching {
            FirebaseCrashlytics.getInstance().log("[$tag] INFO: $message")
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.w(tag, message, tr)
            runCatching {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.log("[$tag] WARN: $message")
                crashlytics.recordException(tr)
            }
        } else {
            Log.w(tag, message)
            runCatching {
                FirebaseCrashlytics.getInstance().log("[$tag] WARN: $message")
            }
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e(tag, message, tr)
            runCatching {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.log("[$tag] ERROR: $message")
                crashlytics.recordException(tr)
            }
        } else {
            Log.e(tag, message)
            runCatching {
                FirebaseCrashlytics.getInstance().log("[$tag] ERROR: $message")
            }
        }
    }

    fun setUserId(userId: String) {
        runCatching {
            FirebaseCrashlytics.getInstance().setUserId(userId)
        }
    }

    fun clearUserId() {
        runCatching {
            FirebaseCrashlytics.getInstance().setUserId("")
        }
    }

    fun setCustomKey(key: String, value: String) {
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
    }

    fun setCustomKey(key: String, value: Int) {
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
    }
}
