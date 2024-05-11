package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum

data class WallpaperState (
    val isDataLoaded: Boolean = false,
    val selectedAlbum: SelectedAlbum? = null
)