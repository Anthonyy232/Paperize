package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a wallpaper object.
 *
 * @param initialAlbumName The name of the album the wallpaper belongs to.
 * @param wallpaperUri The URI of the wallpaper.
 * @param isInRotation Whether the wallpaper is in rotation.
 */
@Entity
data class Wallpaper(
    val initialAlbumName: String,
    val wallpaperUri: String,
    val isInRotation: Boolean,
    @PrimaryKey(autoGenerate = false) val key: Int
)