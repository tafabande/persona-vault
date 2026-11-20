package com.pims.vault.security

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.core.security.PinSecurityManager
import com.pims.vault.core.security.PinVerification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@RunWith(AndroidJUnit4::class)
class PinSecurityDeviceTest {
    @Test
    fun credentialRequiresConfigurationAndPersistsAcrossManagers() = withIsolatedCredential { create, _ ->
        val original = create()
        assertFalse(original.hasPin())
        assertEquals(PinVerification.Unconfigured, original.verifyPin("0123"))
        assertFalse(original.setPin("abcd"))
        assertTrue(original.setPin("0123"))

        val reopened = create()
        assertTrue(reopened.hasPin())
        assertFalse(reopened.setPin("8765"))
        assertEquals(PinVerification.Incorrect, reopened.verifyPin("8765"))
        assertEquals(PinVerification.Verified, reopened.verifyPin("0123"))
        assertEquals(PinVerification.Verified, reopened.changePin("0123", "8765"))
        assertEquals(PinVerification.Incorrect, create().verifyPin("0123"))
        assertEquals(PinVerification.Verified, create().disablePin("8765"))
        assertEquals(PinVerification.Unconfigured, create().verifyPin("8765"))
    }

    @Test
    fun failedAttemptsStayThrottledAfterManagerRecreation() = withIsolatedCredential { create, _ ->
        assertTrue(create().setPin("0123"))
        val verifier = create()
        repeat(4) { assertEquals(PinVerification.Incorrect, verifier.verifyPin("8765")) }
        val cooldown = verifier.verifyPin("8765")
        assertTrue(cooldown is PinVerification.Cooldown)
        assertTrue(create().verifyPin("0123") is PinVerification.Cooldown)
    }

    @Test
    fun legacyCredentialMigratesAfterVerificationAndDamagedCredentialFailsClosed() = withIsolatedCredential { create, prefs ->
        val salt = ByteArray(16) { it.toByte() }
        val spec = PBEKeySpec("6295".toCharArray(), salt, 10_000, 256)
        val verifier = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        try {
            assertTrue(prefs.edit()
                .putString("pin_salt_v1", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("pin_hash_v1", Base64.encodeToString(verifier, Base64.NO_WRAP)).commit())
            assertEquals(PinVerification.Incorrect, create().verifyPin("0000"))
            assertTrue(prefs.contains("pin_hash_v1"))
            assertEquals(PinVerification.Verified, create().verifyPin("6295"))
            assertFalse(prefs.contains("pin_hash_v1"))
            assertTrue(prefs.contains("pin_verifier_v2"))
            assertEquals(PinVerification.Verified, create().verifyPin("6295"))

            assertTrue(prefs.edit().remove("pin_salt_v2").commit())
            assertTrue(create().hasPin())
            assertEquals(PinVerification.Unavailable, create().verifyPin("6295"))
        } finally {
            salt.fill(0)
            verifier.fill(0)
        }
    }

    private fun withIsolatedCredential(
        test: suspend (() -> PinSecurityManager, SharedPreferences) -> Unit
    ) = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = "app-pin-test-${UUID.randomUUID()}"
        val prefs = app.getSharedPreferences(fixture, Context.MODE_PRIVATE)
        val context = object : ContextWrapper(app) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
        }
        try {
            test({ PinSecurityManager(context, fixture) }, prefs)
        } finally {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(fixture)
            app.deleteSharedPreferences(fixture)
        }
    }
}
