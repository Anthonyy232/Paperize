package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a folder directory containing wallpapers
 *
 * @param initialAlbumName The initial album name
 * @param folderName The folder name
 * @param folderUri The folder directory URI
 * @param coverUri The cover URI
 * @param dateModified The date modified of the folder
 * @param wallpaperUris The list of wallpaper URI strings in the folder (stored as JSON)
 * @param order The order of the folder
 */
@Entity(indices = [Index(value = ["initialAlbumName"])])
data class Folder(
    val initialAlbumName: String,
    val folderName: String?,
    val folderUri: String,
    val coverUri: String?,
    val dateModified: Long,
    val wallpaperUris: List<String> = emptyList(),
    val order: Int,
    @PrimaryKey(autoGenerate = false) val key: Int
)