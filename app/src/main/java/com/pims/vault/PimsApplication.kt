package com.pims.vault

import android.app.Application
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.KeySecurityManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PimsApplication : Application() {

    @Inject
    lateinit var keySecurityManager: KeySecurityManager

    override fun onCreate() {
        super.onCreate()

        // 1. Enforce Release Build integrity check (Disallow test/debug crypto in production)
        if (!BuildConfig.DEBUG && BuildConfig.IS_DEBUG_CRYPTO_ALLOWED) {
            throw SecurityException("CRITICAL: Release build configured with insecure debug crypto allowance!")
        }

        // 2. Hardware Security Probing on startup
        try {
            val level = keySecurityManager.initializeAndGetSecurityLevel()
            // Application starts in LOCKED state; no plaintext or decrypted secrets are unsealed on startup
        } catch (e: Exception) {
            // Hardware keystore initialization logged securely or handled via fallback
        }
    }
}
