package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder

sealed class WallpaperEvent {
    data class UpdateSelectedAlbum(val albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder): WallpaperEvent()
    object Reset : WallpaperEvent()
    object Refresh : WallpaperEvent()
}