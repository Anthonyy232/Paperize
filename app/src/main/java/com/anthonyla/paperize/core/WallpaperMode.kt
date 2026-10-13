package com.anthonyla.paperize.core

/**
 * How wallpapers are applied: [STATIC] via WallpaperManager.setBitmap, or [LIVE] via the
 * Paperize WallpaperService (continuous rendering, double-tap and parallax, higher battery use).
 * Users pick a mode during onboarding; switching later resets all data.
 */
enum class WallpaperMode {
    STATIC,
    LIVE;

    companion object {
        fun fromString(value: String?): WallpaperMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: STATIC
        }
    }
}
