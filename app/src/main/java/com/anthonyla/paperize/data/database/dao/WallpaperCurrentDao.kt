package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.data.database.entities.WallpaperCurrentEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperCurrentDao {

    /** Emits null until a wallpaper is applied, and again if that wallpaper or album is deleted. */
    @Query("""
        SELECT w.* FROM wallpapers w
        INNER JOIN wallpaper_current wc ON w.id = wc.wallpaperId
        WHERE wc.albumId = :albumId AND wc.screenType = :screenType
        LIMIT 1
    """)
    fun getCurrentWallpaperFlow(albumId: String, screenType: ScreenType): Flow<WallpaperEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentWallpaper(entity: WallpaperCurrentEntity)
}
