package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Complete data class that represents an album with its wallpapers and folders.
 * @param album The album.
 * @param wallpapers The wallpapers.
 * @param folders The folders.
 * @param totalWallpapers The total wallpapers from wallpapers and folders
 */
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
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
        entity = Wallpaper::class
    )
    val totalWallpapers: List<Wallpaper> = emptyList()
) {
    val sortedFolders: List<Folder>
        get() = folders.map { folder ->
            folder.copy(wallpapers = folder.wallpapers.sortedBy { it.order })
        }.sortedBy { it.order }
    val sortedWallpapers: List<Wallpaper>
        get() = wallpapers.sortedBy { it.order }
    val sortedTotalWallpapers: List<Wallpaper>
        get() = totalWallpapers.sortedBy { it.order }
}