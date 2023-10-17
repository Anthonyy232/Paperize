package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen

sealed class AddAlbumEvent {
    object SaveAlbum: AddAlbumEvent()
    object ClearState: AddAlbumEvent()

    object SelectAll: AddAlbumEvent()
    object DeselectAll: AddAlbumEvent()
    data class SetAlbumName(
        val initialAlbumName: String
    ): AddAlbumEvent()
    data class ReflectAlbumName(
        val newAlbumName: String
    ): AddAlbumEvent()
    data class AddWallpaper(
        val wallpaperUri: String
    ): AddAlbumEvent()
    data class AddFolder(
        val directoryUri: String
    ): AddAlbumEvent()

    data class DeleteFolder(
        val directoryUri: String
    ): AddAlbumEvent()
    data class DeleteWallpaper(
        val wallpaperUri: String
    ): AddAlbumEvent()

    data class SelectWallpaper(
        val wallpaperUri: String
    ): AddAlbumEvent()
    data class SelectFolder(
        val directoryUri: String
    ): AddAlbumEvent()

    data class RemoveFolderFromSelection(
        val directoryUri: String
    ): AddAlbumEvent()
    data class RemoveWallpaperFromSelection(
        val wallpaperUri: String
    ): AddAlbumEvent()
}