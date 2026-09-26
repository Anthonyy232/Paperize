package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers WHERE albumId = :albumId ORDER BY displayOrder ASC")
    fun getWallpapersByAlbum(albumId: String): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE albumId = :albumId AND folderId IS NULL ORDER BY displayOrder ASC")
    fun getDirectWallpapersByAlbum(albumId: String): Flow<List<WallpaperEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wallpapers WHERE albumId = :albumId AND uri = :uri)")
    suspend fun containsUri(albumId: String, uri: String): Boolean

    @Query("SELECT * FROM wallpapers WHERE albumId = :albumId ORDER BY displayOrder ASC, id ASC LIMIT :limit OFFSET :offset")
    suspend fun getWallpapersByAlbumPaged(albumId: String, limit: Int, offset: Int): List<WallpaperEntity>

    @Query("SELECT uri FROM wallpapers WHERE albumId = :albumId ORDER BY folderId IS NOT NULL, displayOrder, id LIMIT 1")
    suspend fun getAlbumCoverUri(albumId: String): String?

    @Query("SELECT uri FROM wallpapers WHERE folderId = :folderId ORDER BY displayOrder, id LIMIT 1")
    suspend fun getFolderCoverUri(folderId: String): String?

    @Query("SELECT * FROM wallpapers WHERE folderId = :folderId ORDER BY displayOrder ASC")
    fun getWallpapersByFolder(folderId: String): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :wallpaperId")
    suspend fun getWallpaperById(wallpaperId: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Update
    suspend fun updateWallpaper(wallpaper: WallpaperEntity)

    @Query("UPDATE wallpapers SET displayOrder = :order WHERE id = :wallpaperId")
    suspend fun updateWallpaperOrder(wallpaperId: String, order: Int)

    @Delete
    suspend fun deleteWallpaper(wallpaper: WallpaperEntity)

    @Query("DELETE FROM wallpapers WHERE id = :wallpaperId")
    suspend fun deleteWallpaperById(wallpaperId: String)

    @Query("DELETE FROM wallpapers WHERE id IN (:wallpaperIds)")
    suspend fun deleteWallpapersByIds(wallpaperIds: List<String>)

    @Query("DELETE FROM wallpapers WHERE folderId = :folderId")
    suspend fun deleteWallpapersByFolder(folderId: String)

    @Query("SELECT COUNT(*) FROM wallpapers WHERE albumId = :albumId")
    suspend fun getWallpaperCountByAlbum(albumId: String): Int

    /**
     * Get wallpaper IDs only for queue building (shuffle mode).
     * Avoids loading full entities when only IDs are needed.
     */
    @Query("SELECT id FROM wallpapers WHERE albumId = :albumId")
    suspend fun getWallpaperIdsByAlbum(albumId: String): List<String>

    /**
     * Get wallpaper IDs with display order for queue building (sequential mode).
     * Avoids loading full entities when only ID + order are needed.
     */
    @Query("SELECT id, displayOrder FROM wallpapers WHERE albumId = :albumId ORDER BY displayOrder ASC")
    suspend fun getWallpaperIdsAndOrderByAlbum(albumId: String): List<WallpaperIdAndOrder>
}

/** Lightweight projection used for queue building â€” avoids loading full WallpaperEntity. */
data class WallpaperIdAndOrder(
    val id: String,
    val displayOrder: Int
)
