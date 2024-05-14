package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum

sealed class WallpaperEvent {
    data class UpdateSelectedAlbum(val selectedAlbum: SelectedAlbum?, val album: AlbumWithWallpaperAndFolder?): WallpaperEvent()
    object Reset : WallpaperEvent()
    object Refresh : WallpaperEvent()
}