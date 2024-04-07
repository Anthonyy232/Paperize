package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface SelectedAlbumRepository {
    fun getSelectedAlbum(): Flow<List<SelectedAlbum>>
    suspend fun upsertSelectedAlbum(selectedAlbum: SelectedAlbum)
    suspend fun upsertAlbum(album: Album)
    suspend fun updateAlbum(album: Album)
    suspend fun deleteAlbum(album: Album)
    suspend fun upsertWallpaper(wallpaper: Wallpaper)
    suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>)
    suspend fun deleteWallpaper(wallpaper: Wallpaper)
    suspend fun cascadeDeleteWallpaper(initialAlbumName: String)
    suspend fun deleteAll()
}