package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Transaction
    @Query("SELECT * FROM album")
    fun getAlbumsWithWallpapers(): Flow<List<AlbumWithWallpaper>>

    @Upsert
    suspend fun upsertAlbum(album: Album)

    @Upsert
    suspend fun upsertWallpaper(wallpaper: Wallpaper)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Delete
    suspend fun deleteWallpaper(wallpaper: Wallpaper)
}