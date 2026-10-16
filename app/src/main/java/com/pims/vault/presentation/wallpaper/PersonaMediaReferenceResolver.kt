package com.pims.vault.presentation.wallpaper

import android.content.Context
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.media.MediaReference
import com.pims.vault.presentation.media.MediaReferenceResolver
import com.pims.vault.presentation.media.MediaReferenceType
import java.io.File

/**
 * Concrete MediaReferenceResolver for Persona Vault.
 * Inspects all local app subsystems to detect if a file is shared before deletion.
 */
class PersonaMediaReferenceResolver(
    private val context: Context,
    private val wallpaperManagerProvider: () -> LocalWallpaperManager
) : MediaReferenceResolver {

    override fun findReferences(filePath: String, excludingWallpaperId: String?): List<MediaReference> {
        if (filePath.isBlank()) return emptyList()
        val targetCanonical = try { File(filePath).canonicalPath } catch (_: Exception) { filePath }
        val references = mutableListOf<MediaReference>()

        try {
            val wallpaperManager = wallpaperManagerProvider()
            val allWallpapers = wallpaperManager.wallpapers.value
            val activeIndex = wallpaperManager.activeWallpaperIndex.value
            val activeItem = allWallpapers.getOrNull(activeIndex)

            // 1. Check if active Persona wallpaper
            if (activeItem != null && activeItem.id == excludingWallpaperId) {
                // If it's the item being deleted, it's currently the active wallpaper!
                references.add(
                    MediaReference(
                        type = MediaReferenceType.ACTIVE_PERSONA_WALLPAPER,
                        description = "Currently active as your Persona wallpaper",
                        referenceKey = activeItem.id
                    )
                )
            }

            // 2. Check if referenced by other wallpaper entries
            allWallpapers.forEach { other ->
                if (other.id != excludingWallpaperId && !other.localFilePath.isNullOrBlank()) {
                    val otherCanonical = try { File(other.localFilePath).canonicalPath } catch (_: Exception) { other.localFilePath }
                    if (otherCanonical == targetCanonical) {
                        references.add(
                            MediaReference(
                                type = MediaReferenceType.ANOTHER_WALLPAPER_ENTRY,
                                description = "Used by wallpaper entry \"${other.title.ifBlank { "Photo" }}\"",
                                referenceKey = other.id
                            )
                        )
                    }
                }
            }

            // 3. Check if currently assigned as Widget background
            val widgetPrefs = context.getSharedPreferences("persona_widget_prefs", Context.MODE_PRIVATE)
            val widgetPath = widgetPrefs.getString("widget_bg_file_path", null)
            if (!widgetPath.isNullOrBlank()) {
                val widgetCanonical = try { File(widgetPath).canonicalPath } catch (_: Exception) { widgetPath }
                if (widgetCanonical == targetCanonical) {
                    references.add(
                        MediaReference(
                            type = MediaReferenceType.WIDGET_BACKGROUND,
                            description = "Assigned as home-screen widget background",
                            referenceKey = "widget_bg"
                        )
                    )
                }
            }

            // 4. Check if referenced by User Avatar
            val avatarManager = PersonaAvatarManager.getInstance(context)
            val avatarPath = avatarManager.customAvatarPath.value
            if (!avatarPath.isNullOrBlank()) {
                val avatarCanonical = try { File(avatarPath).canonicalPath } catch (_: Exception) { avatarPath }
                if (avatarCanonical == targetCanonical) {
                    references.add(
                        MediaReference(
                            type = MediaReferenceType.USER_AVATAR,
                            description = "Used as your Persona identity profile avatar",
                            referenceKey = "user_avatar"
                        )
                    )
                }
            }

            // 5. Check if referenced by any Contact photo
            val contactPhotosDir = File(context.filesDir, "contact_photos")
            if (contactPhotosDir.exists()) {
                val contactDirCanonical = try { contactPhotosDir.canonicalPath } catch (_: Exception) { contactPhotosDir.absolutePath }
                if (targetCanonical.startsWith(contactDirCanonical)) {
                    references.add(
                        MediaReference(
                            type = MediaReferenceType.CONTACT_PHOTO,
                            description = "Used by a contact in your kinship directory",
                            referenceKey = "contact_photos"
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Failsafe: return whatever references were found so far
        }

        return references
    }
}
