package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent

sealed class AlbumViewEvent {
    data object Reset: AlbumViewEvent()
    data object SelectAll: AlbumViewEvent()
    data object DeselectAll: AlbumViewEvent()
    data object DeleteSelected: AlbumViewEvent()
    data object DeleteAlbum: AlbumViewEvent()

    data class LoadAlbum(
        val initialAlbumName: String
    ): AlbumViewEvent()
    data class AddWallpapers(
        val wallpaperUris: List<String>
    ): AlbumViewEvent()
    data class AddFolder(
        val directoryUri: String
    ): AlbumViewEvent()
    data class SelectWallpaper(
        val wallpaperUri: String
    ): AlbumViewEvent()
    data class SelectFolder(
        val directoryUri: String
    ): AlbumViewEvent()
    data class DeselectFolder(
        val directoryUri: String
    ): AlbumViewEvent()
    data class DeselectWallpaper(
        val wallpaperUri: String
    ): AlbumViewEvent()
    data class ChangeAlbumName(
        val displayName: String
    ): AlbumViewEvent()
    data class SetLoading(
        val isLoading: Boolean
    ): AlbumViewEvent()
    data class LoadFoldersAndWallpapers(
        val folders: List<Folder>,
        val wallpapers: List<Wallpaper>
    ): AlbumViewEvent()
}