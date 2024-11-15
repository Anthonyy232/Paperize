package com.anthonyla.paperize.feature.wallpaper.presentation.album

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder

sealed class AlbumsEvent {
    data object DeselectSelected: AlbumsEvent()
    data object Reset: AlbumsEvent()
    data object Refresh: AlbumsEvent()

    data class AddSelectedAlbum(val album: AlbumWithWallpaperAndFolder, val deselectAlbumName: String? = null): AlbumsEvent()
    data class RemoveSelectedAlbum(val deselectAlbumName: String): AlbumsEvent()
}