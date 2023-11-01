package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Wallpaper(
    val initialAlbumName: String,
    @PrimaryKey(autoGenerate = false) val wallpaperUri: String
)