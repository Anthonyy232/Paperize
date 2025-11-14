package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Wallpaper operations
 */
@Dao
interface WallpaperDao {
    /**
     * Get all wallpapers for an album
     */
    @Query("SELECT * FROM wallpapers WHERE albumId = :albumId ORDER BY displayOrder ASC")
    fun getWallpapersByAlbum(albumId: String): Flow<List<WallpaperEntity>>

    /**
     * Get direct wallpapers for an album (not from folders)
     */
    @Query("SELECT * FROM wallpapers WHERE albumId = :albumId AND folderId IS NULL ORDER BY displayOrder ASC")
    fun getDirectWallpapersByAlbum(albumId: String): Flow<List<WallpaperEntity>>

    /**
     * Get wallpapers for a folder
     */
    @Query("SELECT * FROM wallpapers WHERE folderId = :folderId ORDER BY displayOrder ASC")
    fun getWallpapersByFolder(folderId: String): Flow<List<WallpaperEntity>>

    /**
     * Get a specific wallpaper by ID
     */
    @Query("SELECT * FROM wallpapers WHERE id = :wallpaperId")
    suspend fun getWallpaperById(wallpaperId: String): WallpaperEntity?

    /**
     * Get wallpaper by URI
     */
    @Query("SELECT * FROM wallpapers WHERE uri = :uri LIMIT 1")
    suspend fun getWallpaperByUri(uri: String): WallpaperEntity?

    /**
     * Get wallpaper count for album
     */
    @Query("SELECT COUNT(*) FROM wallpapers WHERE albumId = :albumId")
    suspend fun getWallpaperCountByAlbum(albumId: String): Int

    /**
     * Get wallpaper count for folder
     */
    @Query("SELECT COUNT(*) FROM wallpapers WHERE folderId = :folderId")
    suspend fun getWallpaperCountByFolder(folderId: String): Int

    /**
     * Insert wallpaper
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperEntity)

    /**
     * Insert multiple wallpapers
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    /**
     * Update wallpaper
     */
    @Update
    suspend fun updateWallpaper(wallpaper: WallpaperEntity)

    /**
     * Update wallpaper display order
     */
    @Query("UPDATE wallpapers SET displayOrder = :order WHERE id = :wallpaperId")
    suspend fun updateWallpaperOrder(wallpaperId: String, order: Int)

    /**
     * Delete wallpaper
     */
    @Delete
    suspend fun deleteWallpaper(wallpaper: WallpaperEntity)

    /**
     * Delete wallpaper by ID
     */
    @Query("DELETE FROM wallpapers WHERE id = :wallpaperId")
    suspend fun deleteWallpaperById(wallpaperId: String)

    /**
     * Delete all wallpapers for an album
     */
    @Query("DELETE FROM wallpapers WHERE albumId = :albumId")
    suspend fun deleteWallpapersByAlbum(albumId: String)

    /**
     * Delete all wallpapers for a folder
     */
    @Query("DELETE FROM wallpapers WHERE folderId = :folderId")
    suspend fun deleteWallpapersByFolder(folderId: String)

    /**
     * Delete wallpapers by URIs
     */
    @Query("DELETE FROM wallpapers WHERE uri IN (:uris)")
    suspend fun deleteWallpapersByUris(uris: List<String>)

    /**
     * Delete all wallpapers
     */
    @Query("DELETE FROM wallpapers")
    suspend fun deleteAllWallpapers()

    /**
     * Check if wallpaper exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM wallpapers WHERE id = :wallpaperId)")
    suspend fun wallpaperExists(wallpaperId: String): Boolean
}
