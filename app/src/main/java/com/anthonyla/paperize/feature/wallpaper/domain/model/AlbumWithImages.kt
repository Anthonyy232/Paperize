package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Embedded
import androidx.room.Relation

data class AlbumWithImages(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "albumId",
        entityColumn = "albumId"
    )
    val images: List<Image>
)