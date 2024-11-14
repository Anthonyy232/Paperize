package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper

sealed class AddAlbumEvent {
    data object Reset: AddAlbumEvent()
    data object DeleteSelected: AddAlbumEvent()
    data object SelectAll: AddAlbumEvent()
    data object DeselectAll: AddAlbumEvent()
    data class SaveAlbum(
        val initialAlbumName: String
    ): AddAlbumEvent()
    data class AddWallpapers(
        val wallpaperUris: List<String>
    ): AddAlbumEvent()
    data class AddFolder(
        val directoryUri: String
    ): AddAlbumEvent()
    data class SelectWallpaper(
        val wallpaperUri: String
    ): AddAlbumEvent()
    data class SelectFolder(
        val directoryUri: String
    ): AddAlbumEvent()
    data class DeselectFolder(
        val directoryUri: String
    ): AddAlbumEvent()
    data class DeselectWallpaper(
        val wallpaperUri: String
    ): AddAlbumEvent()
    data class SetLoading(
        val isLoading: Boolean
    ): AddAlbumEvent()
    data class LoadFoldersAndWallpapers(
        val folders: List<Folder>,
        val wallpapers: List<Wallpaper>
    ): AddAlbumEvent()
}