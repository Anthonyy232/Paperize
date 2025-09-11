package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Complete data class that represents an album with its wallpapers and folders.
 * @param album The album.
 * @param wallpapers The wallpapers.
 * @param folders The folders.
 * */
data class AlbumWithWallpaperAndFolder(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
        entity = Wallpaper::class,
    )
    val wallpapers: List<Wallpaper> = emptyList(),
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
        entity = Folder::class
    )
    val folders: List<Folder> = emptyList(),
) {
    val totalWallpaperUris: List<String>
        get() = folders.flatMap { it.wallpaperUris } + wallpapers.map { it.wallpaperUri }
    val sortedFolders: List<Folder>
        get() = folders.sortedBy { it.order }
    val totalWallpapers: List<Wallpaper>
        get() = wallpapers + folders.flatMap { folder ->
            folder.wallpaperUris.mapNotNull { uri ->
                wallpapers.find { it.wallpaperUri == uri }
            }
        }
    val sortedWallpapers: List<Wallpaper>
        get() = wallpapers.sortedBy { it.order }
    val sortedTotalWallpapers: List<Wallpaper>
        get() = totalWallpapers.sortedBy { it.order }
}