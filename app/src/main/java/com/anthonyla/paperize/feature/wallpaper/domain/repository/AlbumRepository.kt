package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithImages
import com.anthonyla.paperize.feature.wallpaper.domain.model.Image
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun getAlbums(): Flow<List<AlbumWithImages>>

    suspend fun insertAlbum(album: Album) : Long

    suspend fun insertImage(image: Image)

    suspend fun deleteAlbum(album: Album)

    suspend fun deleteImage(image: Image)
}