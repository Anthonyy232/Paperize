package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder

sealed class WallpaperEvent {
    data class AddSelectedAlbum(val album: AlbumWithWallpaperAndFolder, val deselectAlbumName: String? = null): WallpaperEvent()
    data class RemoveSelectedAlbum(val deselectAlbumName: String): WallpaperEvent()
    data object Reset : WallpaperEvent()
}