package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

sealed class WallpaperEvent {
    data class UpdateSelectedAlbum(val albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder): WallpaperEvent()
    data class StartWallpaperWorker(val time: Long): WallpaperEvent()
    data class ExitRotation(val wallpaper: Wallpaper): WallpaperEvent()
    object Reset : WallpaperEvent()
    object Refresh : WallpaperEvent()
    object StopWallpaperWorker : WallpaperEvent()
}