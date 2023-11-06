package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Transaction
    @Query("SELECT * FROM album")
    fun getAlbumsWithWallpapers(): Flow<List<AlbumWithWallpaper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallpaper(wallpaper: Wallpaper)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolderList(folders: List<Folder>)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Delete
    suspend fun deleteWallpaper(wallpaper: Wallpaper)

    @Delete
    suspend fun deleteFolder(folder: Folder)
    @Delete
    suspend fun deleteWallpaperList(wallpapers: List<Wallpaper>)

    @Delete
    suspend fun deleteFolderList(folders: List<Folder>)

    @Query("DELETE FROM wallpaper WHERE initialAlbumName=:initialAlbumName")
    suspend fun cascadeDeleteWallpaper(initialAlbumName: String)

    @Query("DELETE FROM folder WHERE initialAlbumName=:initialAlbumName")
    suspend fun cascadeDeleteFolder(initialAlbumName: String)

    @Update
    suspend fun updateAlbum(album: Album)

    @Update
    suspend fun updateWallpaper(wallpaper: Wallpaper)

    @Update
    suspend fun updateFolder(folder: Folder)
}