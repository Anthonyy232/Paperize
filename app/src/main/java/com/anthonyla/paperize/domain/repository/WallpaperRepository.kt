package com.anthonyla.paperize.domain.repository

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    suspend fun getWallpaperById(wallpaperId: String): Wallpaper?

    suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): Wallpaper?

    /**
     * Atomically get and remove next wallpaper from queue
     * Prevents race conditions when multiple wallpaper changes happen simultaneously
     */
    suspend fun getAndDequeueWallpaper(albumId: String, screenType: ScreenType): Wallpaper?

    suspend fun removeWallpaperFromQueue(
        albumId: String,
        screenType: ScreenType,
        wallpaperId: String
    )

    /**
     * Restore a prepared wallpaper to the front when applying it did not succeed.
     */
    suspend fun restoreWallpaperToQueueFront(
        albumId: String,
        screenType: ScreenType,
        wallpaperId: String
    )

    /**
     * Fill an empty wallpaper queue; preserve a queue already filled by another caller
     */
    suspend fun ensureWallpaperQueue(
        albumId: String,
        screenType: ScreenType,
        shuffle: Boolean = false
    ): Result<Unit>

    /**
     * Clear all queues for all albums
     * Used when shuffle setting changes to force rebuild with new mode
     */
    suspend fun clearAllQueues(): Result<Unit>

    suspend fun getCurrentWallpaper(albumId: String, screenType: ScreenType): Wallpaper?

    fun getCurrentWallpaperFlow(albumId: String, screenType: ScreenType): Flow<Wallpaper?>

    suspend fun setCurrentWallpaper(albumId: String, screenType: ScreenType, wallpaperId: String)

}
