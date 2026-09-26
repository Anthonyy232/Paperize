package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.data.database.entities.WallpaperQueueEntity

@Dao
interface WallpaperQueueDao {
    @Query("""
        SELECT w.* FROM wallpapers w
        INNER JOIN wallpaper_queue wq ON w.id = wq.wallpaperId
        WHERE wq.albumId = :albumId AND wq.screenType = :screenType
        ORDER BY wq.queuePosition ASC
        LIMIT 1
    """)
    suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): WallpaperEntity?

    @Transaction
    suspend fun getAndDequeueWallpaper(albumId: String, screenType: ScreenType): WallpaperEntity? {
        val wallpaper = getNextWallpaperInQueue(albumId, screenType)
        if (wallpaper != null) {
            deleteQueueItem(albumId, screenType, wallpaper.id)
        }
        return wallpaper
    }

    @Query("SELECT * FROM wallpaper_queue WHERE albumId = :albumId AND screenType = :screenType ORDER BY queuePosition ASC")
    suspend fun getQueueItems(albumId: String, screenType: ScreenType): List<WallpaperQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<WallpaperQueueEntity>)

    @Query("""
        DELETE FROM wallpaper_queue
        WHERE albumId = :albumId AND screenType = :screenType
        AND wallpaperId = :wallpaperId
    """)
    suspend fun deleteQueueItem(albumId: String, screenType: ScreenType, wallpaperId: String)

    @Query("""
        SELECT MIN(queuePosition) FROM wallpaper_queue
        WHERE albumId = :albumId AND screenType = :screenType
    """)
    suspend fun getFirstQueuePosition(albumId: String, screenType: ScreenType): Int?

    @Transaction
    suspend fun restoreQueueItem(
        albumId: String,
        screenType: ScreenType,
        wallpaperId: String
    ) {
        deleteQueueItem(albumId, screenType, wallpaperId)
        val firstPosition = getFirstQueuePosition(albumId, screenType) ?: 0
        insertQueueItems(
            listOf(
                WallpaperQueueEntity(
                    albumId = albumId,
                    wallpaperId = wallpaperId,
                    screenType = screenType,
                    queuePosition = firstPosition - 1
                )
            )
        )
    }

    @Query("DELETE FROM wallpaper_queue WHERE albumId = :albumId AND screenType = :screenType")
    suspend fun clearQueue(albumId: String, screenType: ScreenType)

    @Query("DELETE FROM wallpaper_queue WHERE albumId = :albumId")
    suspend fun clearAllQueues(albumId: String)

    @Query("DELETE FROM wallpaper_queue")
    suspend fun deleteAllQueueItems()

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
