package com.pims.vault.avatar

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import com.pims.vault.core.sync.PhotoBackupCoordinator
import com.pims.vault.presentation.avatar.*
import java.io.File
import javax.inject.Provider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.clearInvocations
import org.mockito.kotlin.*

class AvatarPhotoPersistenceTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun unknownCosmeticsPreserveTheAvatarIdentityAndOwnedPhotoAfterRecreation() {
        val fixture = Fixture()
        val photo = fixture.photo("avatars", "owned.jpg")
        fixture.preferences.edit().putString(CONFIG, JSONObject().apply {
            put("id", "stable-avatar")
            put("seed", "stable-seed")
            put("avatarSource", "CUSTOM_IMAGE")
            put("customAvatarPath", photo.absolutePath)
            listOf("style", "eyeType", "mouthType", "facialFeature", "accessory", "backgroundShape")
                .forEach { put(it, "FUTURE_COSMETIC") }
            put("futurePalette", "retained")
        }.toString()).commit()

        val manager = fixture.manager()
        val config = manager.avatarConfig.value
        assertEquals("stable-avatar", config.id)
        assertEquals("stable-seed", config.seed)
        assertEquals(photo.absolutePath, config.customAvatarPath)
        assertEquals(AvatarSource.CUSTOM_IMAGE, config.avatarSource)
        assertEquals(AvatarStyle.SOFT, config.style)
        manager.updateCustomPhotoAlignment(0.4f, -0.3f)
        val stored = JSONObject(fixture.preferences.getString(CONFIG, null)!!)
        assertEquals("retained", stored.getString("futurePalette"))
        assertEquals("FUTURE_COSMETIC", stored.getString("eyeType"))
        val upload = argumentCaptor<String>()
        verify(fixture.coordinator).backupAvatarConfigAsync(upload.capture(), eq(com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID))
        assertEquals("retained", JSONObject(upload.firstValue).getString("futurePalette"))
        assertEquals("FUTURE_COSMETIC", JSONObject(upload.firstValue).getString("eyeType"))
        assertFalse(JSONObject(upload.firstValue).has("customAvatarPath"))
        assertEquals(photo.absolutePath, fixture.manager().customAvatarPath.value)
    }

    @Test
    fun cloudConfigKeepsLocalOwnedPathsAndFutureFieldsWithoutUploadingAgain() {
        val fixture = Fixture()
        val photo = fixture.photo("avatars", "local.jpg")
        val manager = fixture.manager()
        manager.restoreAvatarPhotoPath(photo.absolutePath, photo.lastModified())
        clearInvocations(fixture.coordinator)

        manager.saveConfigFromJsonString(JSONObject().apply {
            put("id", "restored-avatar")
            put("avatarSource", "CUSTOM_IMAGE")
            put("customAvatarPath", "/data/user/0/other-device/avatars/remote.jpg")
            put("eyeType", "FUTURE_EYES")
            put("futureDetail", "preserved")
        }.toString())

        assertEquals(photo.absolutePath, manager.customAvatarPath.value)
        assertEquals("restored-avatar", manager.avatarConfig.value.id)
        val stored = JSONObject(fixture.preferences.getString(CONFIG, null)!!)
        assertEquals("FUTURE_EYES", stored.getString("eyeType"))
        assertEquals("preserved", stored.getString("futureDetail"))
        val cloud = JSONObject(manager.getCurrentConfigJsonString())
        assertFalse(cloud.has("customAvatarPath"))
        assertEquals("FUTURE_EYES", cloud.getString("eyeType"))
        assertEquals("preserved", cloud.getString("futureDetail"))
        verifyNoInteractions(fixture.coordinator)
    }

    @Test
    fun aForeignCloudPathIsNeverPublishedAsAnOwnedAvatar() {
        val fixture = Fixture()
        val manager = fixture.manager()
        manager.saveConfigFromJsonString("""{"avatarSource":"CUSTOM_IMAGE","customAvatarPath":"/another/device/photo.jpg"}""")
        assertNull(manager.customAvatarPath.value)
        assertFalse(JSONObject(fixture.preferences.getString(CONFIG, null)!!).has("customAvatarPath"))
        verifyNoInteractions(fixture.coordinator)
    }

    @Test
    fun sparseRemoteConfigPreservesPreviouslyStoredFutureFields() {
        val fixture = Fixture()
        val manager = fixture.manager()
        manager.saveConfigFromJsonString("""{"id":"stable-avatar","eyeType":"FUTURE_EYES","futurePalette":{"accent":"violet"}}""")
        manager.saveConfigFromJsonString("""{"customAvatarAlignmentX":0.4}""")

        val stored = JSONObject(fixture.preferences.getString(CONFIG, null)!!)
        assertEquals("stable-avatar", stored.getString("id"))
        assertEquals("FUTURE_EYES", stored.getString("eyeType"))
        assertEquals("violet", stored.getJSONObject("futurePalette").getString("accent"))
        assertEquals(0.4f, manager.avatarConfig.value.customAvatarAlignmentX)
        verifyNoInteractions(fixture.coordinator)
    }

    @Test
    fun restoringPersonPhotosUsesStableIdsAndDoesNotCreateAnotherUpload() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val first = fixture.photo("contact_photos", "one.jpg")
        val second = fixture.photo("contact_photos", "two.jpg")
        val version = manager.personPhotosVersion.value
        assertTrue(manager.restorePersonPhotoPath("person_with_underscores", first.absolutePath, 100L))
        assertTrue(manager.restorePersonPhotoPath("other_person", second.absolutePath, 200L))
        assertEquals(first.absolutePath, manager.getPersonPhotoPath("person_with_underscores"))
        assertEquals(second.absolutePath, manager.getPersonPhotoPath("other_person"))
        assertEquals(version + 2, manager.personPhotosVersion.value)
        assertEquals(setOf("person_with_underscores", "other_person"), manager.localPersonPhotoSelections().map { it.personId }.toSet())
        verifyNoInteractions(fixture.coordinator)
        assertEquals(first.absolutePath, fixture.manager().getPersonPhotoPath("person_with_underscores"))
    }

    @Test
    fun newerLocalSelectionRejectsAnOldRestoreAndAnOldUpload() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val first = fixture.photo("contact_photos", "first.jpg")
        val second = fixture.photo("contact_photos", "second.jpg")
        manager.setPersonPhotoPath("person_a", first.absolutePath)
        val firstRevision = manager.photoSelectionRevision("contact_person_a")
        manager.setPersonPhotoPath("person_a", second.absolutePath)
        val secondRevision = manager.photoSelectionRevision("contact_person_a")

        assertTrue(secondRevision > firstRevision)
        assertFalse(manager.restorePersonPhotoPath("person_a", first.absolutePath, firstRevision))
        assertFalse(manager.isCurrentPhotoSelection("contact", "person_a", first.absolutePath, firstRevision))
        assertTrue(manager.isCurrentPhotoSelection("contact", "person_a", second.absolutePath, secondRevision))
        assertEquals(second.absolutePath, manager.getPersonPhotoPath("person_a"))
    }

    @Test
    fun localEditsAdvancePastARestoredFutureClockRevision() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val first = fixture.photo("contact_photos", "future.jpg")
        val second = fixture.photo("contact_photos", "new.jpg")
        val future = System.currentTimeMillis() + 86_400_000L
        assertTrue(manager.restorePersonPhotoPath("person", first.absolutePath, future))
        manager.setPersonPhotoPath("person", second.absolutePath)
        assertEquals(future + 1L, manager.photoSelectionRevision("contact_person"))
    }

    @Test
    fun explicitPersonSelectionRecoversAnOldUnboundedDeletionRevision() {
        val fixture = Fixture()
        fixture.preferences.edit().putLong("photo_selection_revision_contact_person", Long.MAX_VALUE)
            .putBoolean("photo_removed_person", true).commit()
        val manager = fixture.manager()
        val photo = fixture.photo("contact_photos", "new-selection.jpg")

        manager.setPersonPhotoPath("person", photo.absolutePath)

        val revision = manager.photoSelectionRevision("contact_person")
        assertTrue(revision > 0L && revision < Long.MAX_VALUE)
        assertTrue(manager.isCurrentPhotoSelection("contact", "person", photo.absolutePath, revision))
        assertEquals(photo.absolutePath, fixture.manager().getPersonPhotoPath("person"))
    }

    @Test
    fun explicitPrimarySelectionRecoversAnOldUnboundedDeletionRevision() {
        val fixture = Fixture()
        fixture.preferences.edit().putLong("photo_selection_revision_avatar_primary", Long.MAX_VALUE).commit()
        val manager = fixture.manager()
        val photo = fixture.photo("avatars", "new-selection.jpg")

        manager.saveConfig(manager.avatarConfig.value.copy(avatarSource = AvatarSource.CUSTOM_IMAGE, customAvatarPath = photo.absolutePath))

        val revision = manager.photoSelectionRevision("avatar_primary")
        assertTrue(revision > 0L && revision < Long.MAX_VALUE)
        assertTrue(manager.isCurrentPhotoSelection("avatar", "primary_owner", photo.absolutePath, revision))
    }

    @Test
    fun oldLocalDeletionMarkersMigrateOnceToFiniteRevisionsAndCanBeCleared() {
        val fixture = Fixture()
        val id = "contact_person"
        fixture.restorePreferences.edit().putStringSet("tombstoned_photo_ids", mutableSetOf(id))
            .putLong("deleted_at_$id", Long.MAX_VALUE).commit()
        val coordinator = PhotoBackupCoordinator(fixture.context, mock(), mock(), mock(), mock())

        assertTrue(coordinator.isPhotoDeleted(id))
        val migrated = fixture.restorePreferences.getLong("deleted_at_$id", 0L)
        assertTrue(migrated > 0L && migrated < Long.MAX_VALUE)
        assertFalse(coordinator.isPhotoDeleted(id, migrated + 1L))
        assertEquals(migrated, fixture.restorePreferences.getLong("deleted_at_$id", 0L))
        coordinator.clearPhotoDeleted(id)
        assertFalse(coordinator.isPhotoDeleted(id))
    }

    @Test
    fun backupSelectionsUseCurrentPointersAndExcludeRelationshipAliasesAndOrphanFiles() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val current = fixture.photo("contact_photos", "current.jpg")
        fixture.photo("contact_photos", "contact_another_1700000000000.jpg")
        manager.setPersonPhotoPath("person_with_underscores", current.absolutePath, "relationship_alias")
        val selections = manager.localPersonPhotoSelections()
        assertEquals(1, selections.size)
        assertEquals("person_with_underscores", selections.single().personId)
        assertEquals(current.absolutePath, selections.single().path)
    }

    @Test
    fun legacyFilenameRecoveryDoesNotMistakeAnIdPrefixForAnotherPerson() {
        val fixture = Fixture()
        val other = fixture.photo("contact_photos", "contact_a_b_1700000000000.jpg")
        val manager = fixture.manager()
        assertNull(manager.getPersonPhotoPath("a"))
        assertEquals(other.absolutePath, manager.getPersonPhotoPath("a_b"))
    }

    @Test
    fun localRemovalBlocksOldRestoresAndLegacyFileResurrection() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val photo = fixture.photo("contact_photos", "contact_person_1700000000000.jpg")
        manager.setPersonPhotoPath("person", photo.absolutePath, "relationship")
        manager.setPersonPhotoPath("person", null, "relationship")
        val removedAt = manager.photoSelectionRevision("contact_person")
        assertNull(manager.getPersonPhotoPath("person", "relationship"))
        assertFalse(manager.restorePersonPhotoPath("person", photo.absolutePath, removedAt - 1))
        assertTrue(manager.restorePersonPhotoPath("person", photo.absolutePath, removedAt + 1))
        assertEquals(photo.absolutePath, manager.getPersonPhotoPath("person", "relationship"))
    }

    @Test
    fun remoteRemovalPreservesNewerSelectionsAndClearsAliasesWhenCurrent() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val photo = fixture.photo("contact_photos", "selected.jpg")
        manager.setPersonPhotoPath("person", photo.absolutePath, "relationship")
        val revision = manager.photoSelectionRevision("contact_person")
        clearInvocations(fixture.coordinator)
        assertFalse(manager.applyRemotePhotoDeletion("contact_person", revision - 1))
        assertEquals(photo.absolutePath, manager.getPersonPhotoPath("person", "relationship"))
        assertTrue(manager.applyRemotePhotoDeletion("contact_person", revision + 1))
        assertNull(manager.getPersonPhotoPath("person", "relationship"))
        assertNull(manager.getPersonPhotoPath("relationship"))
        verifyNoInteractions(fixture.coordinator)
    }

    @Test
    fun restoredPersonConfigCannotOverwriteItsOwnedPhotoOrStableLink() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val photo = fixture.photo("contact_photos", "person.jpg")
        manager.restorePersonPhotoPath("person", photo.absolutePath, 100L)
        manager.saveAvatarConfigForRelationshipFromJson("person", """{"id":"appearance-id","seed":"saved-seed","style":"FUTURE_STYLE","customAvatarPath":"/foreign/phone.jpg","futureField":true}""")
        val restored = manager.getAvatarConfigForRelationship("person", "Renamed label", "Friend")
        assertEquals("appearance-id", restored.id)
        assertEquals("saved-seed", restored.seed)
        assertEquals(photo.absolutePath, restored.customAvatarPath)
        assertEquals(photo.absolutePath, manager.getPersonPhotoPath("person"))
        assertTrue(JSONObject(fixture.preferences.getString("rel_avatar_person", null)!!).getBoolean("futureField"))
        verifyNoInteractions(fixture.coordinator)
    }

    @Test
    fun personAvatarEditsAndCloudUploadsPreserveFutureFieldsAndOmitLocalPaths() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val photo = fixture.photo("contact_photos", "person.jpg")
        manager.restorePersonPhotoPath("person", photo.absolutePath, 100L)
        manager.saveAvatarConfigForRelationshipFromJson("person", """{"id":"appearance-id","style":"FUTURE_STYLE","futurePalette":{"accent":"violet"}}""")
        val current = manager.getAvatarConfigForRelationship("person", "Label", "Friend")

        manager.saveAvatarConfigForRelationship("person", current.copy(customAvatarAlignmentX = 0.3f))

        val stored = JSONObject(fixture.preferences.getString("rel_avatar_person", null)!!)
        assertEquals("FUTURE_STYLE", stored.getString("style"))
        assertEquals("violet", stored.getJSONObject("futurePalette").getString("accent"))
        assertEquals(photo.absolutePath, stored.getString("customAvatarPath"))
        val upload = argumentCaptor<String>()
        verify(fixture.coordinator).backupAvatarConfigAsync(upload.capture(), eq("person"))
        val cloud = JSONObject(upload.firstValue)
        assertEquals("FUTURE_STYLE", cloud.getString("style"))
        assertEquals("violet", cloud.getJSONObject("futurePalette").getString("accent"))
        assertFalse(cloud.has("customAvatarPath"))
        assertEquals(photo.absolutePath, fixture.manager().getPersonPhotoPath("person"))
    }

    @Test
    fun primaryPhotoChangesPreserveFutureFieldsWhileExplicitCosmeticEditsReplaceTheChosenField() {
        val fixture = Fixture()
        val manager = fixture.manager()
        manager.saveConfigFromJsonString("""{"style":"FUTURE_STYLE","eyeType":"FUTURE_EYES","futurePalette":"retained"}""")
        val photo = fixture.photo("avatars", "new.jpg")

        manager.saveConfig(manager.avatarConfig.value.copy(avatarSource = AvatarSource.CUSTOM_IMAGE,
            customAvatarPath = photo.absolutePath, style = AvatarStyle.CUTE))
        var cloud = JSONObject(manager.getCurrentConfigJsonString())
        assertEquals("CUTE", cloud.getString("style"))
        assertEquals("FUTURE_EYES", cloud.getString("eyeType"))
        assertEquals("retained", cloud.getString("futurePalette"))
        assertFalse(cloud.has("customAvatarPath"))

        manager.removeCustomPhoto()
        cloud = JSONObject(manager.getCurrentConfigJsonString())
        assertEquals("FUTURE_EYES", cloud.getString("eyeType"))
        assertEquals("retained", cloud.getString("futurePalette"))
        assertFalse(cloud.has("customAvatarPath"))
        assertNull(manager.customAvatarPath.value)
    }

    @Test
    fun failedPreferenceCommitKeepsThePreviousPhotoAndRevision() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val first = fixture.photo("contact_photos", "first.jpg")
        val second = fixture.photo("contact_photos", "second.jpg")
        manager.setPersonPhotoPath("person", first.absolutePath)
        val revision = manager.photoSelectionRevision("contact_person")
        fixture.preferences.failCommit = true
        assertThrows(IllegalStateException::class.java) { manager.setPersonPhotoPath("person", second.absolutePath) }
        assertEquals(first.absolutePath, manager.getPersonPhotoPath("person"))
        assertEquals(revision, manager.photoSelectionRevision("contact_person"))
        assertTrue(first.exists())
    }

    @Test
    fun anUnreadableProviderLeavesTheSavedPhotoUntouchedAndCleansItsStagingFile(): Unit = runBlocking {
        val fixture = Fixture()
        val manager = fixture.manager()
        val photo = fixture.photo("contact_photos", "kept.jpg")
        manager.setPersonPhotoPath("person", photo.absolutePath)
        val uri: Uri = mock()
        whenever(fixture.resolver.openInputStream(uri)).thenReturn(null)
        try {
            manager.importPersonPhoto("person", uri)
            fail("An unreadable provider must report failure")
        } catch (_: IllegalStateException) { }
        assertEquals(photo.absolutePath, manager.getPersonPhotoPath("person"))
        assertTrue(File(fixture.cache, "photo_imports").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun failedImageEncodingLeavesThePreviousPhotoFileAndPointerIntact() {
        val fixture = Fixture()
        val manager = fixture.manager()
        val photo = fixture.photo("avatars", "kept.jpg")
        manager.restoreAvatarPhotoPath(photo.absolutePath, photo.lastModified())
        val bitmap: Bitmap = mock()
        whenever(bitmap.width).thenReturn(10)
        whenever(bitmap.height).thenReturn(10)
        assertNull(manager.saveCustomPhoto(bitmap))
        assertEquals(photo.absolutePath, manager.customAvatarPath.value)
        assertTrue(photo.exists())
        assertEquals(listOf("kept.jpg"), photo.parentFile!!.listFiles().orEmpty().map { it.name })
    }

    private inner class Fixture {
        val files = temporary.newFolder()
        val cache = temporary.newFolder()
        val preferences = MemoryPreferences()
        val restorePreferences = MemoryPreferences()
        val context: Context = mock()
        val resolver: ContentResolver = mock()
        val coordinator: PhotoBackupCoordinator = mock()
        init {
            whenever(context.getSharedPreferences("persona_avatar_prefs", Context.MODE_PRIVATE)).thenReturn(preferences)
            whenever(context.getSharedPreferences("pims_photo_restore_prefs", Context.MODE_PRIVATE)).thenReturn(restorePreferences)
            whenever(context.filesDir).thenReturn(files)
            whenever(context.cacheDir).thenReturn(cache)
            whenever(context.contentResolver).thenReturn(resolver)
            whenever(context.applicationContext).thenReturn(context)
        }
        fun manager() = PersonaAvatarManager(context, Provider { coordinator })
        fun photo(directory: String, name: String) = File(files, directory).apply { mkdirs() }
            .resolve(name).apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()
        var failCommit = false
        override fun getAll(): MutableMap<String, *> = HashMap(values)
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun contains(key: String?) = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = Editor()
        private inner class Editor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var cleared = false
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
            override fun clear(): SharedPreferences.Editor { cleared = true; return this }
            override fun commit(): Boolean {
                if (failCommit) return false
                if (cleared) values.clear()
                changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                return true
            }
            override fun apply() { commit() }
        }
    }

    private companion object {
        const val CONFIG = "user_persona_avatar_config"
    }
}
