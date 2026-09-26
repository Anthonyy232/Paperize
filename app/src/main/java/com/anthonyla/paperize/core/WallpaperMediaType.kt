package com.anthonyla.paperize.core

/** Stored in Room so existing library rows retain their media type. */
enum class WallpaperMediaType {
    IMAGE;

    companion object {
        fun fromString(value: String?): WallpaperMediaType? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
