package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.data.database.entities.WallpaperQueueEntity

/**
 * Data Access Object for Wallpaper Queue operations
 *
 * This replaces the manual queue management in the old Album entity
 */
@Dao
interface WallpaperQueueDao {
    /**
     * Get next wallpaper in queue
     */
    @Query("""
        SELECT w.* FROM wallpapers w
        INNER JOIN wallpaper_queue wq ON w.id = wq.wallpaperId
        WHERE wq.albumId = :albumId AND wq.screenType = :screenType
        ORDER BY wq.queuePosition ASC
        LIMIT 1
    """)
    suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): WallpaperEntity?

    /**
     * Atomically get and remove next wallpaper from queue
     * This prevents race conditions when multiple services try to get wallpapers simultaneously
     */
    @Transaction
    suspend fun getAndDequeueWallpaper(albumId: String, screenType: ScreenType): WallpaperEntity? {
        val wallpaper = getNextWallpaperInQueue(albumId, screenType)
        if (wallpaper != null) {
            // Delete specifically the item we just retrieved to ensure atomicity
            deleteQueueItem(albumId, screenType, wallpaper.id)
        }
        return wallpaper
    }

    /**
     * Get queue items for album and screen type
     */
    @Query("SELECT * FROM wallpaper_queue WHERE albumId = :albumId AND screenType = :screenType ORDER BY queuePosition ASC")
    suspend fun getQueueItems(albumId: String, screenType: ScreenType): List<WallpaperQueueEntity>

    /**
     * Insert multiple queue items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<WallpaperQueueEntity>)

    /**
     * Delete a specific queue item by wallpaperId.
     *
     * The MIN() subquery was removed because [getAndDequeueWallpaper] already retrieves
     * the item at the minimum position before calling this, so the subquery is redundant.
     * The [wallpaperId] index makes this O(log n) instead of O(n) with the subquery.
     */
    @Query("""
        DELETE FROM wallpaper_queue
        WHERE albumId = :albumId AND screenType = :screenType
        AND wallpaperId = :wallpaperId
    """)
    suspend fun deleteQueueItem(albumId: String, screenType: ScreenType, wallpaperId: String)

    /**
     * Clear queue for album and screen type
     */
    @Query("DELETE FROM wallpaper_queue WHERE albumId = :albumId AND screenType = :screenType")
    suspend fun clearQueue(albumId: String, screenType: ScreenType)

    /**
     * Clear all queues for an album
     */
    @Query("DELETE FROM wallpaper_queue WHERE albumId = :albumId")
    suspend fun clearAllQueues(albumId: String)

    /**
     * Delete all queue items
     */
    @Query("DELETE FROM wallpaper_queue")
    suspend fun deleteAllQueueItems()

    /**
     * Rebuild queue with wallpaper IDs
     */
    @Transaction
    suspend fun rebuildQueue(
        albumId: String,
        screenType: ScreenType,
        wallpaperIds: List<String>
    ) {
        clearQueue(albumId, screenType)
        val items = wallpaperIds.mapIndexed { index, wallpaperId ->
            WallpaperQueueEntity(
                albumId = albumId,
                wallpaperId = wallpaperId,
                screenType = screenType,
                queuePosition = index
            )
        }
        insertQueueItems(items)
    }
}
