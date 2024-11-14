package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a folder directory containing wallpapers
 *
 * @param initialAlbumName The initial album name
 * @param folderName The folder name
 * @param folderUri The folder directory URI
 * @param coverUri The cover URI
 * @param dateModified The date modified of the folder
 * @param wallpapers The list of wallpaper URI strings in the folder
 * @param order The order of the folder
 */
@Entity
data class Folder(
    val initialAlbumName: String,
    val folderName: String?,
    val folderUri: String,
    val coverUri: String?,
    val dateModified: Long,
    val wallpapers: List<Wallpaper> = emptyList(),
    val order: Int,
    @PrimaryKey(autoGenerate = false) val key: Int
)