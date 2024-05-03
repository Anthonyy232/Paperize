package com.anthonyla.paperize.feature.wallpaper.presentation.album

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
sealed class AlbumsEvent {
    data object RefreshAlbums: AlbumsEvent()
    data object Reset: AlbumsEvent()

    data class DeleteAlbumWithWallpapers(
        val albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder
    ): AlbumsEvent()

    data class ChangeAlbumName(
        val title: String,
        val albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder
    ): AlbumsEvent()

    data class InitializeAlbum(
        val albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder
    ): AlbumsEvent()
}