package com.anthonyla.paperize.feature.wallpaper.domain.repository

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface SelectedAlbumRepository {
    /**
     * Get selected album from database
     */
    fun getSelectedAlbum(): Flow<List<SelectedAlbum>>

    /**
     * Get wallpapers in rotation from database
     */
    fun getWallpapersInRotation(): Flow<List<Wallpaper>>

    /**
     * Get random wallpaper in rotation
     */
    fun getRandomWallpaperInRotation(): Wallpaper?

    /**
     * Get count of wallpapers in rotation
     */
    fun countWallpapersInRotation(): Int

    /**
     * Add all wallpapers back into rotation
     */
    suspend fun setAllWallpapersInRotation()

    /**
     * Upsert or insert selectedAlbum to database with its wallpapers
     */
    suspend fun upsertSelectedAlbum(selectedAlbum: SelectedAlbum)

    /**
     * Upsert or insert album to database
     */
    suspend fun upsertAlbum(album: Album)

    /**
     * Upsert or insert wallpaper to database
     */
    suspend fun updateAlbum(album: Album)

    /**
     *  Delete album from database
     */
    suspend fun deleteAlbum(album: Album)

    /**
     * Upsert or insert wallpaper to database
     */
    suspend fun upsertWallpaper(wallpaper: Wallpaper)

    /**
     * Upsert or insert list of wallpapers to database

     */
    suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>)

    /**
     * Delete wallpaper from database
     */
    suspend fun deleteWallpaper(wallpaper: Wallpaper)

    /**
     * Delete all wallpapers and folders from database
     */
    suspend fun cascadeDeleteWallpaper(initialAlbumName: String)

    /**
     * Delete all data from database
     */
    suspend fun deleteAll()
}