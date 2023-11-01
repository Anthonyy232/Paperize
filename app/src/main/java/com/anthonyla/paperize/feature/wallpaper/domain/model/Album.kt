package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Album(
    @PrimaryKey(autoGenerate = false) val initialAlbumName: String,
    val displayedAlbumName: String,
    val coverUri: String?,
)