package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun getAlbumsWithWallpapers(): Flow<List<AlbumWithWallpaper>>
    suspend fun upsertAlbum(album: Album)
    suspend fun deleteAlbum(album: Album)
    suspend fun upsertWallpaper(wallpaper: Wallpaper)
    suspend fun deleteWallpaper(wallpaper: Wallpaper)
    suspend fun upsertFolder(folder: Folder)
    suspend fun deleteFolder(folder: Folder)
}