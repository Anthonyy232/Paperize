package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for AlbumWithWallpaperAndFolder which is used for application's list of user albums
 */
@Dao
interface AlbumDao {
    @Transaction
    @Query("SELECT * FROM album ORDER BY initialAlbumName LIMIT 50")
    fun getAlbumsWithWallpaperAndFolder(): Flow<List<AlbumWithWallpaperAndFolder>>

    @Transaction
    @Query("SELECT * FROM album WHERE selected = 1 ORDER BY initialAlbumName LIMIT 20")
    fun getSelectedAlbums(): Flow<List<AlbumWithWallpaperAndFolder>>

    @Query("SELECT * FROM album ORDER BY initialAlbumName")
    fun getAlbums(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE selected = 1 ORDER BY initialAlbumName")
    fun getSelectedAlbumsBasic(): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM album WHERE initialAlbumName = :albumName")
    fun getAlbumWithWallpaperAndFolder(albumName: String): Flow<AlbumWithWallpaperAndFolder?>

    @Query("UPDATE album SET selected = :selected WHERE initialAlbumName = :albumName")
    suspend fun updateAlbumSelection(albumName: String, selected: Boolean)

    @Query("UPDATE album SET selected = 0")
    suspend fun deselectAllAlbums()

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

    @Query("DELETE FROM album")
    suspend fun deleteAllAlbums()

    @Query("DELETE FROM wallpaper")
    suspend fun deleteAllWallpapers()

    @Query("DELETE FROM folder")
    suspend fun deleteAllFolders()

    @Transaction
    suspend fun deleteAllData() {
        deleteAllWallpapers()
        deleteAllFolders()
        deleteAllAlbums()
    }
}