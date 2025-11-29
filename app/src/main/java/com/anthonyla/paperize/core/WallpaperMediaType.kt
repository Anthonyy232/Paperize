package com.anthonyla.paperize.core

/**
 * Media type for wallpapers
 *
 * Determines what type of media the wallpaper file is, which affects
 * how it's rendered in live wallpaper mode.
 *
 * Note: Currently only IMAGE type is supported.
 */
enum class WallpaperMediaType {
    /**
     * Image files: JPEG, PNG, WEBP, AVIF
     * Supported in both static and live wallpaper modes
     */
    IMAGE;

    /**
     * Supported file extensions for this media type
     */
    val supportedExtensions: Set<String>
        get() = when (this) {
            IMAGE -> setOf("jpg", "jpeg", "png", "webp", "avif")
        }

    /**
     * Whether this media type is supported in static wallpaper mode
     */
    val supportedInStaticMode: Boolean
        get() = this == IMAGE

    /**
     * Whether this media type is supported in live wallpaper mode
     */
    val supportedInLiveMode: Boolean
        get() = true  // All types supported in live mode

    companion object {
        /**
         * Detect media type from file extension
         */
        fun fromExtension(extension: String): WallpaperMediaType? {
            val ext = extension.lowercase()
            return entries.find { ext in it.supportedExtensions }
        }

        /**
         * Convert string to WallpaperMediaType
         */
        fun fromString(value: String?): WallpaperMediaType? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
