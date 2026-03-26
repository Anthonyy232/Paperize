package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.entities.WallpaperCurrentEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity

@Dao
interface WallpaperCurrentDao {

    /**
     * Return the wallpaper entity that was last applied for this album/screen,
     * or null if none has been recorded yet (or if it was deleted via CASCADE).
     */
    @Query("""
        SELECT w.* FROM wallpapers w
        INNER JOIN wallpaper_current wc ON w.id = wc.wallpaperId
        WHERE wc.albumId = :albumId AND wc.screenType = :screenType
        LIMIT 1
    """)
    suspend fun getCurrentWallpaper(albumId: String, screenType: ScreenType): WallpaperEntity?

    /** Record (or overwrite) the current wallpaper for an album/screen pair. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentWallpaper(entity: WallpaperCurrentEntity)
}
