package com.pims.vault.design

import android.content.Context
import android.content.SharedPreferences
import com.pims.vault.BuildConfig
import com.pims.vault.core.auth.AuthProviderKind
import com.pims.vault.core.auth.AuthUser
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.crypto.SecretBytes
import com.pims.vault.core.crypto.ZeroizingByteArrayOutputStream
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.core.session.RememberedAccountManager
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import com.pims.vault.presentation.ui.theme.PersonaMood
import com.pims.vault.presentation.ui.theme.PersonaMoodManager
import com.pims.vault.presentation.ui.theme.PersonaMotionManager
import com.pims.vault.presentation.ui.theme.ThemeManager
import com.pims.vault.presentation.ui.theme.ThemeMode
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Appearance setters exercise their normal entry points against separate persistent namespaces. */
class AppearancePersistenceIsolationTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun changingAppearancePreservesTheRememberedAccountAndVaultBinding() {
        val fixture = Fixture()
        val remembered = RememberedAccountManager(fixture.context)
        remembered.rememberAccount(
            AuthUser(ACCOUNT_UID, "fixture@example.invalid", "Fixture Owner", null, true,
                setOf(AuthProviderKind.GOOGLE)),
            AuthProviderKind.GOOGLE.name
        )
        remembered.bindVaultToAccount(ACCOUNT_UID)
        val account = AccountModeManager(fixture.context)
        account.upgradeToCloudAccount("fixture@example.invalid")
        account.completeWalkthrough()
        account.completeInitialProfile()
        val before = fixture.protectedState()
        val editCounts = fixture.protectedEditCounts()

        fixture.cycleAppearance()

        assertEquals(before, fixture.protectedState())
        assertEquals(editCounts, fixture.protectedEditCounts())
        val reopenedAccount = AccountModeManager(fixture.context)
        val reopenedRemembered = RememberedAccountManager(fixture.context)
        assertEquals(AccountMode.CLOUD_SYNCED, reopenedAccount.accountMode.value)
        assertTrue(reopenedAccount.hasCompletedWalkthrough.value)
        assertTrue(reopenedAccount.hasCompletedInitialProfile.value)
        assertEquals(ACCOUNT_UID, reopenedRemembered.rememberedAccount.value?.uid)
        assertEquals(ACCOUNT_UID, reopenedRemembered.boundVaultUid.value)
    }

    @Test
    fun changingAppearancePreservesPersonPhotoLinksAndWallpaperFiles() {
        val fixture = Fixture()
        val avatar = File(fixture.files, "avatars").apply { mkdirs() }
            .resolve("avatar_custom_fixture.jpg").apply { writeBytes(byteArrayOf(8, 6, 4, 2)) }
        val personPhoto = File(fixture.files, "contact_photos").apply { mkdirs() }
            .resolve("contact_${PERSON_ID}_fixture.jpg").apply { writeBytes(byteArrayOf(1, 3, 5, 7)) }
        val wallpaper = File(fixture.files, "wallpapers").apply { mkdirs() }
            .resolve("custom_fixture.jpg").apply { writeBytes(byteArrayOf(9, 8, 7, 6)) }
        val wallpaperMetadata = File(fixture.files, "wallpapers_meta.json").apply {
            writeText("""[{"id":"wallpaper-fixture","title":"Renamable label","type":"LOCAL_IMAGE","localFilePath":"${wallpaper.invariantSeparatorsPath}","isBuiltIn":false}]""")
        }
        val activeWallpaper = File(fixture.files, "wallpapers_active.txt").apply { writeText("2") }
        fixture.prefs("persona_avatar_prefs").edit()
            .putString("person_photo_$PERSON_ID", personPhoto.absolutePath)
            .putString("rel_avatar_$PERSON_ID", """{"id":"cosmetic-avatar-id","seed":"renamable-label","avatarSource":"CUSTOM_IMAGE"}""")
            .putString("user_persona_avatar_config", """{"id":"user_primary","avatarSource":"CUSTOM_IMAGE","customAvatarPath":"${avatar.invariantSeparatorsPath}"}""")
            .apply()
        fixture.prefs("persona_wallpaper_surfaces").edit()
            .putString("active_persona_wp_id", "wallpaper-fixture")
            .putString("active_widget_wp_id", "wallpaper-fixture")
            .putBoolean("wallpaper_autoplay_enabled", true)
            .apply()
        val filesBefore = listOf(avatar, personPhoto, wallpaper, wallpaperMetadata, activeWallpaper)
            .associateWith { it.readBytes() }
        val before = fixture.protectedState()
        val editCounts = fixture.protectedEditCounts()

        fixture.cycleAppearance()

        assertEquals(before, fixture.protectedState())
        assertEquals(editCounts, fixture.protectedEditCounts())
        filesBefore.forEach { (file, bytes) -> assertArrayEquals(file.name, bytes, file.readBytes()) }
        assertEquals(personPhoto.absolutePath,
            fixture.prefs("persona_avatar_prefs").getString("person_photo_$PERSON_ID", null))
        assertEquals("wallpaper-fixture",
            fixture.prefs("persona_wallpaper_surfaces").getString("active_persona_wp_id", null))
    }

    @Test
    fun appearanceChangesLeaveCurrentAndHistoricalEncryptedMediaReadableAfterRecreation(): Unit = runBlocking {
        val fixture = Fixture()
        val deviceWrappingKey = ByteArray(32) { 0x31 }
        val historicalKey = ByteArray(32) { 0x42 }
        val content = ByteArray(80_000) { (it % 251).toByte() }
        val security: KeySecurityManager = mock()
        whenever(security.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES))
            .thenAnswer { SecretBytes(deviceWrappingKey) }
        val crypto = HardenedCryptoEngine()
        val portable = PortableFileKeyManager(fixture.context, security, crypto)
        val originalKey = portable.copyKeyBytes()
        var reopened: PortableFileKeyManager? = null
        try {
            portable.retainHistoricalKey(historicalKey)
            val currentFiles = EncryptedFileStorageImpl(fixture.context, crypto, portable::copyKeyBytes)
            val oldFiles = EncryptedFileStorageImpl(fixture.context, crypto, { historicalKey.copyOf() })
            val current = currentFiles.storeEncryptedFile("current-appearance-fixture", 1,
                "image/png", content.inputStream())
            val old = oldFiles.storeEncryptedFile("historical-appearance-fixture", 1,
                "image/png", content.inputStream())
            val ciphertextBefore = listOf(current, old).associateWith {
                File(fixture.files, it.relativePath).readBytes()
            }
            val before = fixture.protectedState()
            val editCounts = fixture.protectedEditCounts()

            fixture.cycleAppearance()
            portable.lock()
            val restored = PortableFileKeyManager(fixture.context, security, crypto)
            reopened = restored
            val restoredKey = restored.copyKeyBytes()
            try { assertArrayEquals(originalKey, restoredKey) } finally { restoredKey.fill(0) }
            val reader = EncryptedFileStorageImpl(fixture.context, crypto, restored::copyKeyBytes,
                historicalKeyProvider = restored::historicalKeyBytes)
            for (metadata in listOf(current, old)) {
                ZeroizingByteArrayOutputStream().use { output ->
                    reader.readDecryptedFile(metadata.relativePath, metadata.encryptionIvHex,
                        metadata.sha256Hex, output)
                    val plaintext = output.takeBytesAndWipe()
                    try { assertArrayEquals(content, plaintext) } finally { plaintext.fill(0) }
                }
                assertArrayEquals(ciphertextBefore.getValue(metadata),
                    File(fixture.files, metadata.relativePath).readBytes())
            }
            assertEquals(before, fixture.protectedState())
            assertEquals(editCounts, fixture.protectedEditCounts())
        } finally {
            portable.lock()
            reopened?.lock()
            originalKey.fill(0)
            deviceWrappingKey.fill(0)
            historicalKey.fill(0)
            content.fill(0)
        }
    }

    @Test
    fun durableIdentityAndCryptoIdentifiersSurviveAppearanceRefactors() {
        // These strings identify persisted data and wrapped keys across installed app versions.
        assertEquals("personal.info", BuildConfig.APPLICATION_ID)
        assertEquals(
            listOf("PIMS/database/v1", "PIMS/files/v1", "PIMS/vault/v1", "PIMS/vault/v2",
                "PIMS/audit/v1", "PIMS/sharing/master/v1"),
            listOf(HkdfKeyDerivation.CONTEXT_DATABASE, HkdfKeyDerivation.CONTEXT_FILES,
                HkdfKeyDerivation.CONTEXT_VAULT, HkdfKeyDerivation.CONTEXT_VAULT_V2,
                HkdfKeyDerivation.CONTEXT_AUDIT, HkdfKeyDerivation.CONTEXT_SHARING_MASTER)
        )
        val fixture = Fixture()
        val wrappingBytes = ByteArray(32) { 0x35 }
        val databaseKey = ByteArray(32) { 0x67 }
        val master = SecretKeySpec(wrappingBytes, "AES")
        val store: KeyStore = mock()
        whenever(store.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        whenever(store.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), isNull())).thenReturn(master)
        val encrypted = HardenedCryptoEngine().encrypt(databaseKey, wrappingBytes)
        val prefs = fixture.prefs("pims_keystore_wrapped_subkeys")
        prefs.edit()
            .putString("PIMS/database/v1_iv", Base64.getEncoder().encodeToString(encrypted.iv))
            .putString("PIMS/database/v1_cipher",
                Base64.getEncoder().encodeToString(encrypted.combinedCiphertextWithTag))
            .commit()
        try {
            val initial = KeySecurityManager(fixture.context, keyStoreInstance = store, sharedPreferences = prefs)
            initial.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_DATABASE).use {
                assertArrayEquals(databaseKey, it.bytes)
            }
            val before = fixture.protectedState()
            val editCounts = fixture.protectedEditCounts()

            fixture.cycleAppearance()

            val recreated = KeySecurityManager(fixture.context, keyStoreInstance = store, sharedPreferences = prefs)
            recreated.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_DATABASE).use {
                assertArrayEquals(databaseKey, it.bytes)
            }
            assertEquals(before, fixture.protectedState())
            assertEquals(editCounts, fixture.protectedEditCounts())
            verify(store, times(2)).getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), isNull())
        } finally {
            wrappingBytes.fill(0)
            databaseKey.fill(0)
        }
    }

    private inner class Fixture {
        val context: Context = mock()
        val files = temporary.newFolder("files")
        private val preferences = mutableMapOf<String, MemoryPreferences>()

        init {
            whenever(context.filesDir).thenReturn(files)
            whenever(context.cacheDir).thenReturn(temporary.newFolder("cache"))
            whenever(context.applicationContext).thenReturn(context)
            whenever(context.getSharedPreferences(any(), eq(Context.MODE_PRIVATE)))
                .thenAnswer { invocation -> prefs(invocation.getArgument(0)) }
            prefs("persona_motion_prefs").edit()
                .putBoolean("persona_reduced_motion_enabled", false).apply()
            prefs("pims_keystore_wrapped_subkeys").edit()
                .putString("PIMS/database/v1_iv", "wrapped-database-nonce-fixture")
                .putString("PIMS/database/v1_cipher", "wrapped-database-ciphertext-fixture")
                .apply()
            prefs("pims_pin_security_prefs").edit()
                .putString("pin_hash_v1", "pin-hash-fixture")
                .putString("pin_salt_v1", "pin-salt-fixture")
                .putBoolean("biometric_enabled", true).apply()
            PROTECTED_NAMES.forEach { prefs(it) }
        }

        fun prefs(name: String): MemoryPreferences = preferences.getOrPut(name) { MemoryPreferences() }

        fun protectedState(): Map<String, Map<String, *>> = PROTECTED_NAMES.associateWith { prefs(it).getAll() }

        fun protectedEditCounts(): Map<String, Int> = PROTECTED_NAMES.associateWith { prefs(it).edits }

        fun cycleAppearance() {
            val theme = ThemeManager(context)
            val mood = PersonaMoodManager(context)
            val motion = PersonaMotionManager(context)
            val originalTheme = theme.themeMode.value
            val originalMood = mood.currentMood.value
            val originalMotion = motion.isReducedMotionPreferred.value
            val originalSakura = theme.sakuraEffectsEnabled.value
            try {
                ThemeMode.entries.forEach { theme.setThemeMode(it) }
                PersonaMood.entries.forEach { mood.setMood(it) }
                motion.setReducedMotion(true)
                motion.setReducedMotion(false)
                theme.setSakuraEffectsEnabled(true)
                assertTrue(ThemeManager(context).sakuraEffectsEnabled.value)
                assertTrue(prefs("pims_theme_prefs").getBoolean("sakura_effects_enabled", false))
                theme.setSakuraEffectsEnabled(false)
                assertFalse(ThemeManager(context).sakuraEffectsEnabled.value)
                assertFalse(prefs("pims_theme_prefs").getBoolean("sakura_effects_enabled", true))
            } finally {
                theme.setThemeMode(originalTheme)
                mood.setMood(originalMood)
                motion.setReducedMotion(originalMotion)
                theme.setSakuraEffectsEnabled(originalSakura)
            }
        }
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()
        var edits = 0
            private set

        override fun getAll(): MutableMap<String, *> = HashMap(values)
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun edit(): SharedPreferences.Editor {
            edits++
            return Editor()
        }

        private inner class Editor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var clear = false

            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putString(key: String?, value: String?) = put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toMutableSet())
            override fun putInt(key: String?, value: Int) = put(key, value)
            override fun putLong(key: String?, value: Long) = put(key, value)
            override fun putFloat(key: String?, value: Float) = put(key, value)
            override fun putBoolean(key: String?, value: Boolean) = put(key, value)
            override fun remove(key: String?) = put(key, null)
            override fun clear(): SharedPreferences.Editor { clear = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) values.clear()
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT_UID = "firebase-account-fixture"
        private const val PERSON_ID = "0bd79750-1a99-421b-8e64-3f8237d64e70"
        private val PROTECTED_NAMES = listOf(
            "pims_account_mode_prefs", "pims_remembered_account_prefs", "pims_pin_security_prefs",
            "pims_keystore_wrapped_subkeys", "pims_portable_filekey_prefs", "persona_avatar_prefs",
            "persona_wallpaper_surfaces", "pims_photo_restore_prefs"
        )
    }
}
