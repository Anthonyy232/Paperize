package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Wallpaper(
    val initialAlbumName: String,
    val wallpaperUri: String,
    @PrimaryKey(autoGenerate = false) val key: Int
)