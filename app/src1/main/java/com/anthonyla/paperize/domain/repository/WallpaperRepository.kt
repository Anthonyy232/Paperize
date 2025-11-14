package com.anthonyla.paperize.domain.repository

import android.net.Uri
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Wallpaper operations
 */
interface WallpaperRepository {
    /**
     * Get wallpapers for an album
     */
    fun getWallpapersByAlbum(albumId: String): Flow<List<Wallpaper>>

    /**
     * Get wallpapers for a folder
     */
    fun getWallpapersByFolder(folderId: String): Flow<List<Wallpaper>>

    /**
     * Get wallpaper by ID
     */
    suspend fun getWallpaperById(wallpaperId: String): Wallpaper?

    /**
     * Add wallpaper
     */
    suspend fun addWallpaper(wallpaper: Wallpaper): Result<Unit>

    /**
     * Update wallpaper
     */
    suspend fun updateWallpaper(wallpaper: Wallpaper): Result<Unit>

    /**
     * Delete wallpaper
     */
    suspend fun deleteWallpaper(wallpaperId: String): Result<Unit>

    /**
     * Update wallpaper order
     */
    suspend fun updateWallpaperOrder(wallpaperId: String, order: Int): Result<Unit>

    /**
     * Validate wallpaper URIs and remove invalid ones
     */
    suspend fun validateAndRemoveInvalidWallpapers(albumId: String): Result<Int>

    /**
     * Get next wallpaper in queue
     */
    suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): Wallpaper?

    /**
     * Build wallpaper queue for album
     */
    suspend fun buildWallpaperQueue(
        albumId: String,
        screenType: ScreenType,
        shuffle: Boolean = false
    ): Result<Unit>

    /**
     * Dequeue wallpaper (remove first from queue)
     */
    suspend fun dequeueWallpaper(albumId: String, screenType: ScreenType): Result<Unit>

    /**
     * Scan folder for wallpapers
     */
    suspend fun scanFolderForWallpapers(folderUri: Uri): Result<List<Wallpaper>>
}
