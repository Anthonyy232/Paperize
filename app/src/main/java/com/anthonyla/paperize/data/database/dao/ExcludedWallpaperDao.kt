package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anthonyla.paperize.data.database.entities.ExcludedWallpaperEntity

@Dao
interface ExcludedWallpaperDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExcludedWallpaperEntity)

    @Query("SELECT uri FROM excluded_wallpapers WHERE folderId = :folderId")
    suspend fun getUris(folderId: String): List<String>

    @Query("DELETE FROM excluded_wallpapers WHERE folderId = :folderId AND uri IN (:uris)")
    suspend fun deleteUris(folderId: String, uris: List<String>)
}
