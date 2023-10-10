package com.anthonyla.paperize.feature.wallpaper.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Transaction
    @Query("SELECT * FROM wallpaper")
    fun getImages(): Flow<List<Wallpaper>>

    @Upsert
    suspend fun upsertImage(wallpaper: Wallpaper)

    @Delete
    suspend fun deleteImage(wallpaper: Wallpaper)
}