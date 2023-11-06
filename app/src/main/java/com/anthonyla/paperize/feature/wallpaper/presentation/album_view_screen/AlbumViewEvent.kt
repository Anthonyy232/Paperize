package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper

sealed class AlbumViewEvent {
    data object ClearState: AlbumViewEvent()
    data class SelectAll(
        val albumsWithWallpaper: AlbumWithWallpaper
    ): AlbumViewEvent()
    data object DeselectAll: AlbumViewEvent()
    data class DeleteSelected(
        val albumsWithWallpaper: AlbumWithWallpaper
    ): AlbumViewEvent()
    data class SelectWallpaper(
        val wallpaperUri: String
    ): AlbumViewEvent()
    data class SelectFolder(
        val directoryUri: String
    ): AlbumViewEvent()

    data class RemoveFolderFromSelection(
        val directoryUri: String
    ): AlbumViewEvent()
    data class RemoveWallpaperFromSelection(
        val wallpaperUri: String
    ): AlbumViewEvent()
    data class SetSize(
        val size: Int
    ): AlbumViewEvent()
}