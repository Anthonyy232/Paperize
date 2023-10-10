package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper

sealed class WallpaperEvent {
    data class DeleteWallpaper(val uriString: String): WallpaperEvent()
    data class AddWallpaper(val uriString: String): WallpaperEvent()
}