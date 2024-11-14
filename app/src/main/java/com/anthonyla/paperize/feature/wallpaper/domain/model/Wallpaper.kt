package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a wallpaper object
 *
 * @param initialAlbumName The name of the album the wallpaper belongs to
 * @param wallpaperUri The URI of the wallpaper
 * @param fileName The name of the file
 * @param dateModified The date the wallpaper was last modified
 */
@Entity
data class Wallpaper(
    val initialAlbumName: String,
    val wallpaperUri: String,
    val fileName: String,
    val dateModified: Long,
    val order: Int,
    @PrimaryKey(autoGenerate = false) val key: Int
)