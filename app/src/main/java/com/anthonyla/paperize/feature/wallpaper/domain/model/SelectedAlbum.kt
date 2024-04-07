package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class that represents a selected album with its wallpapers.
 * @param album the selected album.
 * @param wallpapers the wallpapers of the selected album. Includes the wallpapers from each folders in album.
 */
data class SelectedAlbum(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
    )
    val wallpapers: List<Wallpaper> = emptyList(),
)