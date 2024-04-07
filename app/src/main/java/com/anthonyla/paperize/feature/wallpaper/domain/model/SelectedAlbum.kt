package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Embedded
import androidx.room.Relation

data class SelectedAlbum(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "initialAlbumName",
        entityColumn = "initialAlbumName",
    )
    val wallpapers: List<Wallpaper> = emptyList(),
)