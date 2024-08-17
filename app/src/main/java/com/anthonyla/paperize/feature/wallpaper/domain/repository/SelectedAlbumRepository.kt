package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface SelectedAlbumRepository {
    /**
     * Get selected album from database
     */
    fun getSelectedAlbum(): Flow<List<SelectedAlbum>>

    /**
     * Upsert or insert selectedAlbum to database with its wallpapers
     */
    suspend fun upsertSelectedAlbum(selectedAlbum: SelectedAlbum)

    /**
     *  Delete album from database
     */
    suspend fun deleteAlbum(initialAlbumName: String)

    /**
     * Delete wallpaper from database
     */
    suspend fun deleteWallpaper(wallpaper: Wallpaper)

    /**
     * Cascade delete album and its wallpaper and folder. Proper way to delete an AlbumWithWallpaperAndFolder
     */
    suspend fun cascadeDeleteAlbum(initialAlbumName: String)

    /**
     * Delete all data from database
     */
    suspend fun deleteAll()
}