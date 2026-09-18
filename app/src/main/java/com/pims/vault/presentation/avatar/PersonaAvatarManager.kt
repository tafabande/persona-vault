package com.pims.vault.presentation.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("persona_avatar_prefs", Context.MODE_PRIVATE)

    private val _avatarConfig = MutableStateFlow(loadConfig())
    val avatarConfig: StateFlow<PersonaAvatarConfig> = _avatarConfig.asStateFlow()

    private val _avatarBehavior = MutableStateFlow(loadBehaviorMode())
    val avatarBehavior: StateFlow<AvatarBehaviorMode> = _avatarBehavior.asStateFlow()

    private val _avatarSource = MutableStateFlow(_avatarConfig.value.avatarSource)
    val avatarSource: StateFlow<AvatarSource> = _avatarSource.asStateFlow()

    private val _customAvatarPath = MutableStateFlow(_avatarConfig.value.customAvatarPath)
    val customAvatarPath: StateFlow<String?> = _customAvatarPath.asStateFlow()

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
            style = AvatarStyle.valueOf(json.optString("style", AvatarStyle.SOFT.name)),
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
            eyeType = EyeType.valueOf(json.optString("eyeType", EyeType.GENTLE_DOT.name)),
            eyebrowType = eyebrowType,
            mouthType = MouthType.valueOf(json.optString("mouthType", MouthType.WARM_SMILE.name)),
            facialFeature = FacialFeature.valueOf(json.optString("facialFeature", FacialFeature.CUTE_BLUSH.name)),
            accessory = Accessory.valueOf(json.optString("accessory", Accessory.NONE.name)),
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
            backgroundShape = BackgroundShape.valueOf(json.optString("backgroundShape", BackgroundShape.ORGANIC_BLOB.name)),
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

    private fun loadConfig(): PersonaAvatarConfig {
        val jsonStr = prefs.getString(KEY_CONFIG, null) ?: return PersonaAvatarConfig.default()
        return try {
            parseConfigFromJson(JSONObject(jsonStr))
        } catch (_: Exception) {
            PersonaAvatarConfig.default()
        }
    }

    fun saveConfig(config: PersonaAvatarConfig) {
        val json = configToJson(config)
        prefs.edit().putString(KEY_CONFIG, json.toString()).apply()
        _avatarConfig.value = config.copy(expression = AvatarExpression.NORMAL)
        _avatarSource.value = config.avatarSource
        _customAvatarPath.value = config.customAvatarPath
    }

    fun getAvatarConfigForRelationship(personId: String, name: String, role: String): PersonaAvatarConfig {
        val customKey = "rel_avatar_$personId"
        val jsonStr = prefs.getString(customKey, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                return parseConfigFromJson(JSONObject(jsonStr))
            } catch (_: Exception) {}
        }
        return PersonaAvatarConfig.fromRelationship(name, role)
    }

    fun saveAvatarConfigForRelationship(personId: String, config: PersonaAvatarConfig) {
        val customKey = "rel_avatar_$personId"
        val json = configToJson(config)
        prefs.edit().putString(customKey, json.toString()).apply()
    }

    /**
     * Saves a local custom photo to internal storage.
     * Works 100% offline, local-first. Never auto-uploads to cloud.
     */
    fun saveCustomPhoto(bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val photoFile = File(dir, "avatar_custom_${System.currentTimeMillis()}.jpg")
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val newPath = photoFile.absolutePath

            // Delete previous custom photo if different
            _customAvatarPath.value?.let { oldPath ->
                try {
                    val oldFile = File(oldPath)
                    if (oldFile.exists() && oldFile.absolutePath != newPath) {
                        oldFile.delete()
                    }
                } catch (_: Exception) {}
            }

            val updated = _avatarConfig.value.copy(
                customAvatarPath = newPath,
                avatarSource = AvatarSource.CUSTOM_IMAGE
            )
            saveConfig(updated)
            newPath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Removes the custom photo and falls back seamlessly to the preserved generated avatar.
     */
    fun removeCustomPhoto() {
        _customAvatarPath.value?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        val updated = _avatarConfig.value.copy(
            customAvatarPath = null,
            avatarSource = AvatarSource.GENERATED
        )
        saveConfig(updated)
    }

    fun updateCustomPhotoAlignment(alignX: Float, alignY: Float) {
        val updated = _avatarConfig.value.copy(
            customAvatarAlignmentX = alignX.coerceIn(-1f, 1f),
            customAvatarAlignmentY = alignY.coerceIn(-1f, 1f)
        )
        saveConfig(updated)
    }

    fun loadCustomPhotoBitmap(): Bitmap? {
        val path = _customAvatarPath.value ?: return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }

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

    fun getPersonPhotoPath(personId: String): String? {
        val path = prefs.getString("person_photo_$personId", null) ?: return null
        return if (File(path).exists()) path else null
    }

    fun setPersonPhotoPath(personId: String, path: String?) {
        if (path == null) {
            prefs.edit().remove("person_photo_$personId").apply()
        } else {
            prefs.edit().putString("person_photo_$personId", path).apply()
        }
    }

    fun saveRelationshipPhotoFromBitmap(personId: String, bitmap: Bitmap): String? {
        return try {
            val avatarsDir = File(context.filesDir, "contact_photos").apply { mkdirs() }
            val destFile = File(avatarsDir, "contact_${personId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            setPersonPhotoPath(personId, destFile.absolutePath)
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_CONFIG = "user_persona_avatar_config"
        private const val KEY_BEHAVIOR = "user_persona_avatar_behavior"
    }
}
