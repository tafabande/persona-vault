package com.pims.vault.presentation.avatar

import android.content.Context
import android.graphics.Bitmap
import com.pims.vault.core.storage.decodePhotoPreview
import com.pims.vault.core.storage.importOwnedPhoto
import com.pims.vault.core.storage.inspectPhoto
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PersonaAvatarManager
 *
 * Local-first manager for the user's avatar identity and contact avatars.
 * Supports dual avatar sources:
 * 1. Procedural Vector Generated Avatar (with rich animations, moods, time awareness)
 * 2. Local-first Custom Uploaded Photo (with subtle physical response)
 *
 * Switching between sources preserves all underlying data without loss.
 */
@Singleton
class PersonaAvatarManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoBackupCoordinatorProvider: javax.inject.Provider<com.pims.vault.core.sync.PhotoBackupCoordinator>? = null
) {
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface AvatarBackupEntryPoint {
        fun photoBackupCoordinator(): com.pims.vault.core.sync.PhotoBackupCoordinator
    }

    private fun getPhotoBackupCoordinator(): com.pims.vault.core.sync.PhotoBackupCoordinator? {
        return photoBackupCoordinatorProvider?.get() ?: try {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                AvatarBackupEntryPoint::class.java
            ).photoBackupCoordinator()
        } catch (_: Throwable) {
            null
        }
    }

    private val prefs = context.getSharedPreferences("persona_avatar_prefs", Context.MODE_PRIVATE)

    private val _avatarConfig = MutableStateFlow(loadConfig())
    val avatarConfig: StateFlow<PersonaAvatarConfig> = _avatarConfig.asStateFlow()

    private val _avatarBehavior = MutableStateFlow(loadBehaviorMode())
    val avatarBehavior: StateFlow<AvatarBehaviorMode> = _avatarBehavior.asStateFlow()

    private val _avatarSource = MutableStateFlow(_avatarConfig.value.avatarSource)
    val avatarSource: StateFlow<AvatarSource> = _avatarSource.asStateFlow()

    private val _customAvatarPath = MutableStateFlow(_avatarConfig.value.customAvatarPath)
    val customAvatarPath: StateFlow<String?> = _customAvatarPath.asStateFlow()

    private val _personPhotosVersion = MutableStateFlow(System.currentTimeMillis())
    val personPhotosVersion: StateFlow<Long> = _personPhotosVersion.asStateFlow()

    private fun loadBehaviorMode(): AvatarBehaviorMode {
        val modeStr = prefs.getString(KEY_BEHAVIOR, null) ?: return AvatarBehaviorMode.ALIVE
        return try {
            AvatarBehaviorMode.valueOf(modeStr)
        } catch (_: Exception) {
            AvatarBehaviorMode.ALIVE
        }
    }

    fun setBehaviorMode(mode: AvatarBehaviorMode) {
        prefs.edit().putString(KEY_BEHAVIOR, mode.name).apply()
        _avatarBehavior.value = mode
    }

    fun setAvatarSource(source: AvatarSource) {
        val current = _avatarConfig.value
        val updated = current.copy(avatarSource = source)
        saveConfig(updated)
        _avatarSource.value = source
    }

    fun clearCustomPhoto() = removeCustomPhoto()

    /**
     * Determines the current day period based strictly on device local time.
     * Works 100% offline, zero permissions, zero network.
     */
    fun getCurrentDayPeriod(): PersonaDayPeriod {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> PersonaDayPeriod.MORNING
            in 12..17 -> PersonaDayPeriod.AFTERNOON
            in 18..21 -> PersonaDayPeriod.EVENING
            else -> PersonaDayPeriod.NIGHT
        }
    }

    private fun parseConfigFromJson(json: JSONObject): PersonaAvatarConfig {
        val genderStr = json.optString("gender", AvatarGender.UNSPECIFIED.name)
        val gender = try { AvatarGender.valueOf(genderStr) } catch (_: Exception) { AvatarGender.UNSPECIFIED }
        val sourceStr = json.optString("avatarSource", AvatarSource.GENERATED.name)
        val source = try { AvatarSource.valueOf(sourceStr) } catch (_: Exception) { AvatarSource.GENERATED }
        val customPath = json.optString("customAvatarPath", "").takeIf { it.isNotBlank() }
        val customAlignX = json.optDouble("customAvatarAlignmentX", 0.0).toFloat()
        val customAlignY = json.optDouble("customAvatarAlignmentY", 0.0).toFloat()

        val headShapeStr = json.optString("headShape", if (gender == AvatarGender.MALE) HeadShape.CHISELED_ANGULAR.name else HeadShape.SOFT_OVAL.name)
        val headShape = try { HeadShape.valueOf(headShapeStr) } catch (_: Exception) { if (gender == AvatarGender.MALE) HeadShape.CHISELED_ANGULAR else HeadShape.SOFT_OVAL }
        val eyebrowStr = json.optString("eyebrowType", EyebrowType.NEUTRAL_ARCH.name)
        val eyebrowType = try { EyebrowType.valueOf(eyebrowStr) } catch (_: Exception) { EyebrowType.NEUTRAL_ARCH }

        return PersonaAvatarConfig(
            id = json.optString("id", "user_primary"),
            style = enumOrDefault(json.optString("style"), AvatarStyle.SOFT),
            seed = json.optString("seed", "persona"),
            gender = gender,
            headShape = headShape,
            avatarSource = source,
            customAvatarPath = customPath,
            customAvatarAlignmentX = customAlignX,
            customAvatarAlignmentY = customAlignY,
            skinTone = try {
                SkinTone.valueOf(json.optString("skinTone", SkinTone.WARM_BEIGE.name))
            } catch (_: Exception) {
                SkinTone.WARM_BEIGE
            },
            hairStyle = try {
                HairStyle.valueOf(json.optString("hairStyle", HairStyle.FADE.name))
            } catch (_: Exception) {
                HairStyle.FADE
            },
            hairColor = try {
                HairColor.valueOf(json.optString("hairColor", HairColor.ESPRESSO_BLACK.name))
            } catch (_: Exception) {
                HairColor.ESPRESSO_BLACK
            },
            eyeType = enumOrDefault(json.optString("eyeType"), EyeType.GENTLE_DOT),
            eyebrowType = eyebrowType,
            mouthType = enumOrDefault(json.optString("mouthType"), MouthType.WARM_SMILE),
            facialFeature = enumOrDefault(json.optString("facialFeature"), FacialFeature.CUTE_BLUSH),
            accessory = enumOrDefault(json.optString("accessory"), Accessory.NONE),
            clothingStyle = try {
                ClothingStyle.valueOf(json.optString("clothingStyle", ClothingStyle.MINIMAL_CREW.name))
            } catch (_: Exception) {
                ClothingStyle.MINIMAL_CREW
            },
            clothingColor = try {
                ClothingColor.valueOf(json.optString("clothingColor", ClothingColor.TERRACOTTA.name))
            } catch (_: Exception) {
                ClothingColor.TERRACOTTA
            },
            backgroundShape = enumOrDefault(json.optString("backgroundShape"), BackgroundShape.ORGANIC_BLOB),
            expression = AvatarExpression.NORMAL
        )
    }

    private fun configToJson(config: PersonaAvatarConfig): JSONObject {
        return JSONObject().apply {
            put("id", config.id)
            put("style", config.style.name)
            put("seed", config.seed)
            put("gender", config.gender.name)
            put("headShape", config.headShape.name)
            put("avatarSource", config.avatarSource.name)
            config.customAvatarPath?.let { put("customAvatarPath", it) }
            put("customAvatarAlignmentX", config.customAvatarAlignmentX.toDouble())
            put("customAvatarAlignmentY", config.customAvatarAlignmentY.toDouble())
            put("skinTone", config.skinTone.name)
            put("hairStyle", config.hairStyle.name)
            put("hairColor", config.hairColor.name)
            put("eyeType", config.eyeType.name)
            put("eyebrowType", config.eyebrowType.name)
            put("mouthType", config.mouthType.name)
            put("facialFeature", config.facialFeature.name)
            put("accessory", config.accessory.name)
            put("clothingStyle", config.clothingStyle.name)
            put("clothingColor", config.clothingColor.name)
            put("backgroundShape", config.backgroundShape.name)
        }
    }

    private fun healStaleAvatarPath(config: PersonaAvatarConfig): PersonaAvatarConfig {
        if (config.avatarSource != AvatarSource.CUSTOM_IMAGE) {
            if (config.customAvatarPath.isNullOrBlank()) {
                val coordinator = getPhotoBackupCoordinator()
                if (coordinator != null && !coordinator.isPhotoDeleted("avatar_primary")) {
                    coordinator.downloadPhotoIfMissingAsync(
                        com.pims.vault.core.sync.ProfilePhotoSyncService.KIND_AVATAR,
                        "avatar_primary",
                        "primary_owner"
                    )
                }
            }
            return config
        }
        val path = config.customAvatarPath
        if (!path.isNullOrBlank()) {
            ownedPhotoFile(path, "avatars")?.let { return config.copy(customAvatarPath = it.absolutePath) }
        }
        // File not found locally: attempt download from cloud storage if not explicitly deleted
        val coordinator = getPhotoBackupCoordinator()
        if (coordinator != null && !coordinator.isPhotoDeleted("avatar_primary")) {
            coordinator.downloadPhotoIfMissingAsync(
                com.pims.vault.core.sync.ProfilePhotoSyncService.KIND_AVATAR,
                "avatar_primary",
                "primary_owner"
            )
        }
        return config.copy(customAvatarPath = null)
    }

    private fun loadConfig(): PersonaAvatarConfig {
        val jsonStr = prefs.getString(KEY_CONFIG, null) ?: return PersonaAvatarConfig.default()
        return try {
            healStaleAvatarPath(parseConfigFromJson(JSONObject(jsonStr)))
        } catch (_: Exception) {
            PersonaAvatarConfig.default()
        }
    }

    @Synchronized
    fun getCurrentConfigJsonString(): String {
        val stored = runCatching {
            JSONObject(prefs.getString(KEY_CONFIG, null) ?: configToJson(_avatarConfig.value).toString())
        }.getOrElse { configToJson(_avatarConfig.value) }
        return cloudConfig(stored)
    }

    private fun cloudConfig(config: JSONObject): String = JSONObject(config.toString()).apply {
        remove("customAvatarPath")
    }.toString()

    private fun mergeConfigChanges(stored: JSONObject, previous: PersonaAvatarConfig, updated: PersonaAvatarConfig) {
        val before = configToJson(previous)
        val after = configToJson(updated)
        after.keys().forEach { key ->
            // Keep unsupported future enum values until this client actually changes that setting.
            if (!stored.has(key) || before.opt(key) != after.opt(key)) stored.put(key, after.get(key))
        }
        if (updated.customAvatarPath == null) stored.remove("customAvatarPath")
        else stored.put("customAvatarPath", updated.customAvatarPath)
    }

    private fun persistAvatarConfig(config: PersonaAvatarConfig, revision: Long? = null, restoredJson: JSONObject? = null) {
        val previous = restoredJson ?: runCatching { JSONObject(prefs.getString(KEY_CONFIG, "{}") ?: "{}") }.getOrDefault(JSONObject())
        if (restoredJson == null) {
            mergeConfigChanges(previous, _avatarConfig.value, config)
        }
        if (config.customAvatarPath == null) previous.remove("customAvatarPath")
        else previous.put("customAvatarPath", config.customAvatarPath)
        val editor = prefs.edit().putString(KEY_CONFIG, previous.toString())
        if (revision != null) editor.putLong(REVISION_PREFIX + "avatar_primary", revision)
        check(editor.commit()) { "Avatar preferences could not be saved" }
        _avatarConfig.value = config.copy(expression = AvatarExpression.NORMAL)
        _avatarSource.value = config.avatarSource
        _customAvatarPath.value = config.customAvatarPath
    }

    @Synchronized
    fun saveConfig(config: PersonaAvatarConfig) {
        val healed = healStaleAvatarPath(config)
        val changedPhoto = healed.customAvatarPath != _customAvatarPath.value || healed.avatarSource != _avatarSource.value
        val revision = if (changedPhoto) nextSelectionRevision("avatar_primary") else null
        persistAvatarConfig(healed, revision)
        try {
            getPhotoBackupCoordinator()?.backupAvatarConfigAsync(getCurrentConfigJsonString())
            if (changedPhoto && healed.avatarSource == AvatarSource.CUSTOM_IMAGE && !healed.customAvatarPath.isNullOrBlank()) {
                getPhotoBackupCoordinator()?.clearPhotoDeleted("avatar_primary")
                getPhotoBackupCoordinator()?.backupAvatarAsync(healed.customAvatarPath)
            }
        } catch (_: Exception) { }
    }

    @Synchronized
    fun saveConfigFromJsonString(jsonString: String) {
        try {
            val local = JSONObject(prefs.getString(KEY_CONFIG, "{}") ?: "{}")
            val current = configToJson(_avatarConfig.value)
            current.keys().forEach { key -> if (!local.has(key)) local.put(key, current.get(key)) }
            val remote = JSONObject(jsonString)
            remote.keys().forEach { key -> if (key != "customAvatarPath") local.put(key, remote.get(key)) }
            val ownedPath = ownedPhotoFile(_customAvatarPath.value, "avatars")?.absolutePath
            if (ownedPath == null) local.remove("customAvatarPath") else {
                local.put("customAvatarPath", ownedPath)
                local.put("avatarSource", AvatarSource.CUSTOM_IMAGE.name)
            }
            persistAvatarConfig(parseConfigFromJson(local), restoredJson = local)
        } catch (_: Exception) { }
    }

    fun getAvatarConfigForRelationship(personId: String, name: String, role: String): PersonaAvatarConfig {
        val saved = prefs.getString("rel_avatar_$personId", null)
        val config = saved?.let { runCatching { parseConfigFromJson(JSONObject(it)) }.getOrNull() }
            ?: PersonaAvatarConfig.fromRelationship(name, role)
        val path = localPersonPhotoPath(personId)
        return if (path == null) config.copy(customAvatarPath = null) else
            config.copy(avatarSource = AvatarSource.CUSTOM_IMAGE, customAvatarPath = path)
    }

    @Synchronized
    fun saveAvatarConfigForRelationship(personId: String, config: PersonaAvatarConfig) {
        if (personId.isBlank()) return
        val localPath = localPersonPhotoPath(personId)
        val local = config.copy(customAvatarPath = localPath)
        val stored = runCatching { JSONObject(prefs.getString("rel_avatar_$personId", "{}") ?: "{}") }.getOrDefault(JSONObject())
        mergeConfigChanges(stored, parseConfigFromJson(stored), local)
        check(prefs.edit().putString("rel_avatar_$personId", stored.toString()).commit()) {
            "Person avatar preferences could not be saved"
        }
        _personPhotosVersion.value += 1
        try { getPhotoBackupCoordinator()?.backupAvatarConfigAsync(cloudConfig(stored), personId) }
        catch (_: Exception) { }
    }

    @Synchronized
    fun saveAvatarConfigForRelationshipFromJson(personId: String, jsonString: String) {
        if (personId.isBlank() || jsonString.isBlank()) return
        try {
            val local = JSONObject(prefs.getString("rel_avatar_$personId", "{}") ?: "{}")
            val remote = JSONObject(jsonString)
            remote.keys().forEach { key -> if (key != "customAvatarPath") local.put(key, remote.get(key)) }
            val path = localPersonPhotoPath(personId)
            if (path == null) local.remove("customAvatarPath") else local.put("customAvatarPath", path)
            // Parse validates supported fields while the merged JSON keeps future cosmetic attributes.
            parseConfigFromJson(local)
            check(prefs.edit().putString("rel_avatar_$personId", local.toString()).commit())
            _personPhotosVersion.value += 1
        } catch (_: Exception) { }
    }

    @Synchronized
    fun restoreAvatarPhotoPath(path: String, revision: Long): Boolean {
        val owned = ownedPhotoFile(path, "avatars") ?: return false
        val currentPath = _customAvatarPath.value
        val currentRevision = photoSelectionRevision("avatar_primary", currentPath)
        if (currentRevision > revision) return false
        if (currentRevision == revision) {
            if (currentPath == owned.absolutePath) return true
            if (hasReadablePhoto(currentPath, "avatars")) return false
        }
        val config = _avatarConfig.value.copy(avatarSource = AvatarSource.CUSTOM_IMAGE, customAvatarPath = owned.absolutePath)
        persistAvatarConfig(config, revision)
        return true
    }

    @Synchronized
    fun restorePersonPhotoPath(personId: String, path: String, revision: Long): Boolean {
        if (!validPersonPhotoId(personId)) return false
        val owned = ownedPhotoFile(path, "contact_photos") ?: return false
        val current = localPersonPhotoPath(personId)
        val currentRevision = photoSelectionRevision("contact_$personId", current)
        if (currentRevision > revision) return false
        if (currentRevision == revision) {
            if (current == owned.absolutePath) return true
            if (hasReadablePhoto(current, "contact_photos")) return false
        }
        val editor = prefs.edit().putString("person_photo_$personId", owned.absolutePath)
            .putLong(REVISION_PREFIX + "contact_$personId", revision).putBoolean(REMOVED_PREFIX + personId, false)
        prefs.all.filter { (key, value) -> key.startsWith(ALIAS_PREFIX) && value == personId }.keys.forEach { key ->
            val alias = key.removePrefix(ALIAS_PREFIX)
            editor.putString("person_photo_$alias", owned.absolutePath).putBoolean(REMOVED_PREFIX + alias, false)
        }
        check(editor.commit())
        _personPhotosVersion.value += 1
        return true
    }

    @Synchronized
    fun applyRemotePhotoDeletion(photoId: String, revision: Long): Boolean {
        val path = if (photoId == "avatar_primary") _customAvatarPath.value else
            photoId.takeIf { it.startsWith("contact_") }?.removePrefix("contact_")?.let { localPersonPhotoPath(it) }
        if (photoSelectionRevision(photoId, path) > revision) return false
        if (photoId == "avatar_primary") {
            persistAvatarConfig(_avatarConfig.value.copy(customAvatarPath = null, avatarSource = AvatarSource.GENERATED), revision)
        } else if (photoId.startsWith("contact_")) {
            val personId = photoId.removePrefix("contact_")
            if (!validPersonPhotoId(personId)) return false
            val aliases = prefs.all.filter { (key, value) -> key.startsWith(ALIAS_PREFIX) && value == personId }.keys
            val editor = prefs.edit().remove("person_photo_$personId").putBoolean(REMOVED_PREFIX + personId, true)
                .putLong(REVISION_PREFIX + photoId, revision)
            aliases.forEach { key ->
                val alias = key.removePrefix(ALIAS_PREFIX)
                editor.remove("person_photo_$alias").putBoolean(REMOVED_PREFIX + alias, true)
            }
            check(editor.commit())
            _personPhotosVersion.value += 1
        } else return false
        return true
    }

    @Synchronized
    fun photoSelectionRevision(photoId: String, path: String? = null): Long =
        prefs.getLong(REVISION_PREFIX + photoId, 0L).takeIf { it > 0 }
            ?: path?.let { File(it).lastModified() } ?: 0L

    private fun nextSelectionRevision(photoId: String): Long {
        val path = if (photoId == "avatar_primary") _customAvatarPath.value else
            photoId.removePrefix("contact_").let { localPersonPhotoPath(it) }
        val previous = photoSelectionRevision(photoId, path)
        val now = System.currentTimeMillis().coerceAtLeast(1L)
        // Older recovery code used MAX_VALUE for an undated deletion marker.
        return if (previous == Long.MAX_VALUE) now else maxOf(now, previous + 1L)
    }

    @Synchronized
    fun isCurrentPhotoSelection(kind: String, ownerKey: String, path: String, revision: Long): Boolean {
        val current = when (kind) {
            "avatar" -> _customAvatarPath.value
            "contact" -> localPersonPhotoPath(ownerKey)
            else -> return true
        }
        val id = if (kind == "avatar") "avatar_primary" else "contact_$ownerKey"
        return current == path && photoSelectionRevision(id, current) == revision
    }

    data class PersonPhotoSelection(val personId: String, val path: String, val revision: Long)

    @Synchronized
    fun localPersonPhotoSelections(): List<PersonPhotoSelection> = prefs.all.entries.mapNotNull { (key, value) ->
        if (!key.startsWith("person_photo_") || value !is String) return@mapNotNull null
        val id = key.removePrefix("person_photo_")
        if (prefs.contains(ALIAS_PREFIX + id) || prefs.getBoolean(REMOVED_PREFIX + id, false)) return@mapNotNull null
        val path = ownedPhotoFile(value, "contact_photos")?.absolutePath ?: return@mapNotNull null
        PersonPhotoSelection(id, path, photoSelectionRevision("contact_$id", path))
    }

    /**
     * Saves a local custom photo to internal storage.
     * Local-first cache; cloud backup is handled separately by
     * PhotoBackupCoordinator (encrypted B2 + Firestore metadata) so the
     * avatar survives reinstall. Returns the absolute local path.
     */
    fun saveCustomPhoto(bitmap: Bitmap): String? = try {
        val file = writeOwnedBitmap(bitmap, "avatars", "avatar_custom")
        saveConfig(_avatarConfig.value.copy(customAvatarPath = file.absolutePath, avatarSource = AvatarSource.CUSTOM_IMAGE))
        file.absolutePath
    } catch (_: Exception) { null }

    suspend fun importAvatarPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val file = importOwnedPhoto(context, uri, "avatars", "avatar_custom")
        try {
            currentCoroutineContext().ensureActive()
            synchronized(this@PersonaAvatarManager) {
                saveConfig(_avatarConfig.value.copy(customAvatarPath = file.absolutePath, avatarSource = AvatarSource.CUSTOM_IMAGE))
            }
            file.absolutePath
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
    }

    suspend fun importPersonPhoto(personId: String, uri: Uri, altId: String? = null): String =
        withContext(Dispatchers.IO) {
            val key = personId.ifBlank { altId.orEmpty() }
            require(validPersonPhotoId(key)) { "Invalid person identity" }
            val file = importOwnedPhoto(context, uri, "contact_photos", "contact_$key")
            try {
                currentCoroutineContext().ensureActive()
                setPersonPhotoPath(personId, file.absolutePath, altId)
                file.absolutePath
            } catch (failure: Exception) {
                file.delete()
                throw failure
            }
        }

    private fun writeOwnedBitmap(bitmap: Bitmap, directory: String, prefix: String): File {
        require(bitmap.width > 0 && bitmap.height > 0)
        val root = context.filesDir.canonicalFile
        val base = File(root, directory).canonicalFile
        require(base != root && base.toPath().startsWith(root.toPath()))
        check(base.isDirectory || base.mkdirs())
        val target = File(base, "${prefix}_${UUID.randomUUID()}.jpg")
        val staged = File.createTempFile(".pending-", ".jpg", base)
        try {
            FileOutputStream(staged).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "Photo encoding failed" }
                output.fd.sync()
            }
            require(staged.length() in 1..8L * 1024 * 1024) { "The saved photo exceeds the supported size" }
            Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            return target
        } finally { staged.delete() }
    }

    /**
     * Removes the custom photo and falls back seamlessly to the preserved generated avatar.
     */
    @Synchronized
    fun removeCustomPhoto() {
        val previous = ownedPhotoFile(_customAvatarPath.value, "avatars")
        val config = _avatarConfig.value.copy(customAvatarPath = null, avatarSource = AvatarSource.GENERATED)
        persistAvatarConfig(config, nextSelectionRevision("avatar_primary"))
        try {
            getPhotoBackupCoordinator()?.deleteAvatarPhotoAsync()
            getPhotoBackupCoordinator()?.backupAvatarConfigAsync(getCurrentConfigJsonString())
        } catch (_: Exception) { }
        previous?.delete()
    }

    fun updateCustomPhotoAlignment(alignX: Float, alignY: Float) {
        val updated = _avatarConfig.value.copy(
            customAvatarAlignmentX = alignX.coerceIn(-1f, 1f),
            customAvatarAlignmentY = alignY.coerceIn(-1f, 1f)
        )
        saveConfig(updated)
    }

    fun loadCustomPhotoBitmap(): Bitmap? {
        val file = ownedPhotoFile(_customAvatarPath.value, "avatars") ?: return null
        return decodePhotoPreview(file)
    }

    private fun hasReadablePhoto(path: String?, directory: String): Boolean =
        ownedPhotoFile(path, directory)?.let { runCatching { inspectPhoto(it) != null }.getOrDefault(false) } == true

    fun setTemporaryExpression(expression: AvatarExpression) {
        _avatarConfig.value = _avatarConfig.value.copy(expression = expression)
    }

    fun resetExpression() {
        _avatarConfig.value = _avatarConfig.value.copy(expression = AvatarExpression.NORMAL)
    }

    fun getDeterministicAvatarForPerson(personId: String, name: String): PersonaAvatarConfig {
        val seed = if (personId.isNotBlank()) personId else name
        return PersonaAvatarConfig.fromSeed(seed, style = AvatarStyle.SOFT)
    }

    private fun ownedPhotoFile(path: String?, directory: String): File? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val root = context.filesDir.canonicalFile
            val base = File(root, directory).canonicalFile
            if (base == root || !base.toPath().startsWith(root.toPath())) return@runCatching null
            val source = File(path)
            listOf(source, File(base, source.name)).map { it.canonicalFile }.firstOrNull { canonical ->
                canonical.toPath().startsWith(base.toPath()) && canonical != base && canonical.isFile &&
                    canonical.length() in 1..16L * 1024 * 1024
            }
        }.getOrNull()
    }

    private fun validPersonPhotoId(id: String): Boolean = id.matches(Regex("[A-Za-z0-9_-]{1,160}"))

    private fun localPersonPhotoPath(personId: String, altId: String? = null): String? {
        if (personId.isBlank() && altId.isNullOrBlank()) return null
        val owner = personId.ifBlank { altId.orEmpty() }
        if (prefs.getBoolean(REMOVED_PREFIX + owner, false)) return null
        for (id in listOfNotNull(personId.takeIf { it.isNotBlank() }, altId?.takeIf { it.isNotBlank() })) {
            ownedPhotoFile(prefs.getString("person_photo_$id", null), "contact_photos")?.let { return it.absolutePath }
        }
        return null
    }

    @Synchronized
    fun getPersonPhotoPath(personId: String, altId: String? = null): String? {
        if (personId.isBlank() && altId.isNullOrBlank()) return null
        val owner = personId.ifBlank { altId.orEmpty() }
        if (prefs.getBoolean(REMOVED_PREFIX + owner, false)) return null
        val primary = personId.ifBlank { altId.orEmpty() }
        val current = localPersonPhotoPath(primary, altId)
        if (current != null) {
            if (personId.isNotBlank() && !altId.isNullOrBlank() && personId != altId) {
                prefs.edit().putString("person_photo_$personId", current).putString(ALIAS_PREFIX + altId, personId).apply()
            }
            return current
        }
        val coordinator = getPhotoBackupCoordinator()
        if (coordinator?.isPhotoDeleted("contact_$primary") == true) return null
        // Recover historical files only by their stable person/relationship IDs.
        val directory = File(context.filesDir, "contact_photos")
        val keys = listOfNotNull(personId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,160}")) },
            altId?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,160}")) })
        val legacy = directory.listFiles()?.filter { file ->
            file.isFile && file.length() > 0 && keys.any { id ->
                file.name == "contact_$id.jpg" ||
                    file.name.matches(Regex("contact_${Regex.escape(id)}_(?:[0-9]{10,17}|[0-9a-fA-F-]{36}|restored)\\.jpg"))
            }
        }?.maxByOrNull { it.lastModified() }
        if (legacy != null) {
            if (restorePersonPhotoPath(primary, legacy.absolutePath, legacy.lastModified())) {
                if (!altId.isNullOrBlank() && primary != altId) prefs.edit().putString(ALIAS_PREFIX + altId, primary).apply()
                return legacy.absolutePath
            }
        }
        val key = personId.ifBlank { altId.orEmpty() }
        if (key.isNotBlank()) coordinator?.downloadPhotoIfMissingAsync("contact", "contact_$key", key)
        return null
    }

    @Synchronized
    fun setPersonPhotoPath(personId: String, path: String?, altId: String? = null) {
        val primary = personId.ifBlank { altId.orEmpty() }
        require(validPersonPhotoId(primary)) { "Choose a valid person before saving their photo" }
        val editor = prefs.edit().remove(ALIAS_PREFIX + primary)
        val revision = nextSelectionRevision("contact_$primary")
        editor.putLong(REVISION_PREFIX + "contact_$primary", revision)
        if (path == null) {
            editor.remove("person_photo_$primary").putBoolean(REMOVED_PREFIX + primary, true)
            if (!altId.isNullOrBlank()) editor.remove("person_photo_$altId").putBoolean(REMOVED_PREFIX + altId, true)
        } else {
            val owned = checkNotNull(ownedPhotoFile(path, "contact_photos")) { "The app-owned person photo is unavailable" }
            editor.putString("person_photo_$primary", owned.absolutePath).putBoolean(REMOVED_PREFIX + primary, false)
            if (!altId.isNullOrBlank() && primary != altId) {
                editor.putString("person_photo_$altId", owned.absolutePath).putString(ALIAS_PREFIX + altId, primary)
                    .putBoolean(REMOVED_PREFIX + altId, false)
            }
        }
        check(editor.commit()) { "Person photo preferences could not be saved" }
        _personPhotosVersion.value += 1
        try {
            val coordinator = getPhotoBackupCoordinator()
            if (path == null) coordinator?.deleteContactPhotoAsync(primary, altId)
            else {
                coordinator?.clearPhotoDeleted("contact_$primary")
                if (!altId.isNullOrBlank()) coordinator?.clearPhotoDeleted("contact_$altId")
                coordinator?.backupContactPhotoAsync(primary, localPersonPhotoPath(primary))
            }
        } catch (_: Exception) { }
    }

    fun saveRelationshipPhotoFromBitmap(personId: String, bitmap: Bitmap, altId: String? = null): String? = try {
        val key = personId.ifBlank { altId.orEmpty() }
        require(validPersonPhotoId(key)) { "Invalid person identity" }
        val file = writeOwnedBitmap(bitmap, "contact_photos", "contact_$key")
        setPersonPhotoPath(personId, file.absolutePath, altId)
        file.absolutePath
    } catch (_: Exception) { null }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    companion object {
        private const val REVISION_PREFIX = "photo_selection_revision_"
        private const val REMOVED_PREFIX = "photo_removed_"
        private const val ALIAS_PREFIX = "photo_person_alias_"
        private const val KEY_CONFIG = "user_persona_avatar_config"
        private const val KEY_BEHAVIOR = "user_persona_avatar_behavior"

        @Volatile
        private var instance: PersonaAvatarManager? = null

        fun getInstance(context: Context): PersonaAvatarManager {
            return instance ?: synchronized(this) {
                instance ?: PersonaAvatarManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
