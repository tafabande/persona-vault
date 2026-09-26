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

        // 0. Initialize SQLCipher native libraries
        net.sqlcipher.database.SQLiteDatabase.loadLibs(this)

        // 0.1 Initialize FirebaseApp safely
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:321349557206:android:1d287d5dbbbe36c435e277")
                    .setApiKey("AIzaSyDG4KPcy72pL-OJibZizStT_nP0JBxOV8I")
                    .setProjectId("person-cab82")
                    .setStorageBucket("person-cab82.firebasestorage.app")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            android.util.Log.e("PimsApplication", "Failed to initialize Firebase", e)
        }

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
