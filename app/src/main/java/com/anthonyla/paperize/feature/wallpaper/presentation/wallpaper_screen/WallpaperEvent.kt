package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum

sealed class WallpaperEvent {
    data class UpdateSelectedAlbum(val album: AlbumWithWallpaperAndFolder, val deleteAlbumName: String? = null): WallpaperEvent()
    data class Reset(val album: SelectedAlbum? = null) : WallpaperEvent()
    data object Refresh : WallpaperEvent()
}