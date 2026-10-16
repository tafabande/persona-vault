package com.pims.vault.presentation.wallpaper

/**
 * Distinguishes the distinct surfaces to which wallpapers and media can be applied.
 * Changes apply strictly to the user-selected surface without bleeding over.
 */
enum class WallpaperSurface(
    val title: String,
    val subtitle: String,
    val iconDescription: String
) {
    PERSONA(
        title = "Persona App",
        subtitle = "In-app vault carousel and dashboard",
        iconDescription = "Persona App Background"
    ),
    WIDGET(
        title = "Home Widget",
        subtitle = "Home-screen Quick Actions widget",
        iconDescription = "Home Widget Background"
    ),
    SYSTEM(
        title = "Android System",
        subtitle = "Device home and lock screen",
        iconDescription = "Android System Wallpaper"
    )
}
