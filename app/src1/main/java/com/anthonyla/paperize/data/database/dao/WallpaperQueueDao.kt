package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.data.database.entities.WallpaperQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Wallpaper Queue operations
 *
 * This replaces the manual queue management in the old Album entity
 */
@Dao
interface WallpaperQueueDao {
    /**
     * Get queue for a specific album and screen type
     */
    @Query("""
        SELECT w.* FROM wallpapers w
        INNER JOIN wallpaper_queue wq ON w.id = wq.wallpaperId
        WHERE wq.albumId = :albumId AND wq.screenType = :screenType
        ORDER BY wq.queuePosition ASC
    """)
    fun getQueueWallpapers(albumId: String, screenType: ScreenType): Flow<List<WallpaperEntity>>

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
     * Get queue items for album and screen type
     */
    @Query("SELECT * FROM wallpaper_queue WHERE albumId = :albumId AND screenType = :screenType ORDER BY queuePosition ASC")
    suspend fun getQueueItems(albumId: String, screenType: ScreenType): List<WallpaperQueueEntity>

    /**
     * Get queue size
     */
    @Query("SELECT COUNT(*) FROM wallpaper_queue WHERE albumId = :albumId AND screenType = :screenType")
    suspend fun getQueueSize(albumId: String, screenType: ScreenType): Int

    /**
     * Insert queue item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: WallpaperQueueEntity)

    /**
     * Insert multiple queue items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<WallpaperQueueEntity>)

    /**
     * Remove first item from queue and shift positions
     */
    @Transaction
    suspend fun dequeueWallpaper(albumId: String, screenType: ScreenType) {
        // Delete the first item
        deleteFirstQueueItem(albumId, screenType)
        // Shift all positions down by 1
        decrementQueuePositions(albumId, screenType)
    }

    @Query("""
        DELETE FROM wallpaper_queue
        WHERE albumId = :albumId AND screenType = :screenType
        AND queuePosition = (
            SELECT MIN(queuePosition)
            FROM wallpaper_queue
            WHERE albumId = :albumId AND screenType = :screenType
        )
    """)
    suspend fun deleteFirstQueueItem(albumId: String, screenType: ScreenType)

    @Query("UPDATE wallpaper_queue SET queuePosition = queuePosition - 1 WHERE albumId = :albumId AND screenType = :screenType")
    suspend fun decrementQueuePositions(albumId: String, screenType: ScreenType)

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
