package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Complete data class that represents an album with its wallpapers and folders.
 * @param album The album.
 * @param wallpapers The wallpapers.
 * @param folders The folders.
 */
data class AlbumWithWallpaperAndFolder(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
    )
    val wallpapers: List<Wallpaper> = emptyList(),
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
    )
    val folders: List<Folder> = emptyList(),
)