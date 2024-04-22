package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for selected album for application to keep track of which album is currently selected for changing wallpapers from
 */
@Dao
interface SelectedAlbumDao {

    @Transaction
    @Query("SELECT * FROM album")
    fun getSelectedAlbum(): Flow<List<SelectedAlbum>>

    @Query("SELECT * FROM wallpaper WHERE isInRotation = 1")
    fun getWallpapersInRotation(): Flow<List<Wallpaper>>

    @Query("SELECT * FROM wallpaper ORDER BY RANDOM() LIMIT 1")
    fun getRandomWallpaperInRotation(): Wallpaper?

    @Query("UPDATE wallpaper SET isInRotation = 1")
    suspend fun setAllWallpapersInRotation()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallpaper(wallpaper: Wallpaper)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Delete
    suspend fun deleteWallpaper(wallpaper: Wallpaper)

    @Query("DELETE FROM wallpaper WHERE initialAlbumName=:initialAlbumName")
    suspend fun cascadeDeleteWallpaper(initialAlbumName: String)

    @Query("DELETE FROM album")
    suspend fun deleteAllAlbums()

    @Query("DELETE FROM wallpaper")
    suspend fun deleteAllWallpapers()

    @Update
    suspend fun updateAlbum(album: Album)

    @Transaction
    suspend fun deleteAll() {
        deleteAllAlbums()
        deleteAllWallpapers()
    }
}