package com.anthonyla.paperize.feature.wallpaper.presentation.library

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.Image

sealed class AlbumsEvent {
    //data class Order(val albumsOrder: AlbumsOrder): AlbumsEvent()
    data class DeleteAlbum(val album: Album): AlbumsEvent()
    data class DeleteImage(val image: Image): AlbumsEvent()
    data class AddAlbum(val album: Album): AlbumsEvent()
    data class AddImage(val image: Image): AlbumsEvent()
}