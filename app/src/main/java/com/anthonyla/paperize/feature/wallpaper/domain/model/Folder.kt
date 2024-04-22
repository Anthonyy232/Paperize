package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a folder directory containing wallpapers.
 *
 * @param initialAlbumName The initial album name.
 * @param folderName The folder name.
 * @param coverUri The cover URI.
 * @param wallpapers The list of wallpaper URI strings paired with inRotation boolean as a <Pair>
 * @param folderUri The folder directory URI.
 */
@Entity
data class Folder(
    val initialAlbumName: String,
    val folderName: String?,
    val coverUri: String?,
    val wallpapers: List<Pair<String, Boolean>> = emptyList(),
    val folderUri: String,
    @PrimaryKey(autoGenerate = false) val key: Int
)