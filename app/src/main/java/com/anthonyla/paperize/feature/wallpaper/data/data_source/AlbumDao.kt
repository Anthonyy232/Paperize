package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithImages
import com.anthonyla.paperize.feature.wallpaper.domain.model.Image
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Transaction
    @Query("SELECT * FROM album")
    fun getAlbumsWithImages(): Flow<List<AlbumWithImages>>

    @Upsert
    suspend fun upsertAlbum(album: Album) : Long

    @Upsert
    suspend fun upsertImage(image: Image)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Delete
    suspend fun deleteImage(image: Image)
}