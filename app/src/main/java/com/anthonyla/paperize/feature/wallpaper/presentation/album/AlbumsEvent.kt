package com.anthonyla.paperize.feature.wallpaper.presentation.album
sealed class AlbumsEvent {
    data object LoadAlbums: AlbumsEvent()
}