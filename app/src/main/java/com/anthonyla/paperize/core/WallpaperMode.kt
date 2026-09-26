package com.anthonyla.paperize.core

/**
 * Wallpaper mode enum
 *
 * Determines whether the app uses static wallpaper (WallpaperManager.setBitmap)
 * or live wallpaper (WallpaperService) for applying wallpapers.
 *
 * Users choose their preferred mode during onboarding and can switch later
 * with full data reset.
 */
enum class WallpaperMode {
    /**
     * Static wallpaper mode
     * - Uses WallpaperManager.setBitmap() to apply wallpapers
     * - Supports images only
     * - Lower battery usage
     * - No interactive effects
     */
    STATIC,

    /**
     * Live wallpaper mode
     * - Uses WallpaperService for continuous rendering
     * - Supports images only (with effects)
     * - Interactive effects (double-tap, parallax)
     * - Higher battery usage
     */
    LIVE;

    companion object {
        fun fromString(value: String?): WallpaperMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: STATIC
        }
    }
}
