package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anthonyla.paperize.data.database.entities.WallpaperEntity

@Dao
interface WallpaperDao {
    @Query("DELETE FROM wallpapers WHERE albumId = :albumId AND id IN (:ids)")
    suspend fun deleteAlbumWallpapers(albumId: String, ids: List<String>): Int

    @Query("SELECT uri FROM wallpapers WHERE albumId = :albumId AND folderId IS :folderId")
    suspend fun getUrisInCollection(albumId: String, folderId: String?): List<String>

    @Query("SELECT COALESCE(MAX(displayOrder), -1) FROM wallpapers WHERE albumId = :albumId")
    suspend fun getMaxOrder(albumId: String): Int

    @Query("SELECT * FROM wallpapers WHERE albumId = :albumId AND (:afterId IS NULL OR id > :afterId) ORDER BY id LIMIT :limit")
    suspend fun getWallpapersByAlbumPage(albumId: String, limit: Int, afterId: String?): List<WallpaperEntity>

    @Query("SELECT uri FROM wallpapers WHERE albumId = :albumId ORDER BY folderId IS NOT NULL, displayOrder, id LIMIT 1")
    suspend fun getAlbumCoverUri(albumId: String): String?

    @Query("SELECT uri FROM wallpapers WHERE folderId = :folderId ORDER BY displayOrder, id LIMIT 1")
    suspend fun getFolderCoverUri(folderId: String): String?

    @Query("SELECT * FROM wallpapers WHERE id = :wallpaperId")
    suspend fun getWallpaperById(wallpaperId: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Update
    suspend fun updateWallpaper(wallpaper: WallpaperEntity)

    @Query("UPDATE wallpapers SET displayOrder = :order WHERE id = :wallpaperId AND albumId = :albumId")
    suspend fun updateAlbumWallpaperOrder(albumId: String, wallpaperId: String, order: Int)

    @Query("DELETE FROM wallpapers WHERE folderId = :folderId")
    suspend fun deleteWallpapersByFolder(folderId: String)

    @Query("SELECT COUNT(*) FROM wallpapers WHERE albumId = :albumId")
    suspend fun getWallpaperCountByAlbum(albumId: String): Int

    @Query("SELECT id FROM wallpapers WHERE albumId = :albumId")
    suspend fun getWallpaperIdsByAlbum(albumId: String): List<String>

    @Query("SELECT id FROM wallpapers WHERE albumId = :albumId ORDER BY displayOrder, id")
    suspend fun getOrderedWallpaperIdsByAlbum(albumId: String): List<String>
}
