package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.relations.AlbumWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Album operations
 */
@Dao
interface AlbumDao {
    /**
     * Get all albums with their wallpapers and folders
     */
    @Transaction
    @Query("SELECT * FROM albums ORDER BY modifiedAt DESC")
    fun getAllAlbumsWithDetails(): Flow<List<AlbumWithDetails>>

    /**
     * Get a specific album by ID with details
     */
    @Transaction
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getAlbumWithDetails(albumId: String): Flow<AlbumWithDetails?>

    /**
     * Get all albums (without relations)
     */
    @Query("SELECT * FROM albums ORDER BY modifiedAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    /**
     * Get a specific album by ID
     */
    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: String): AlbumEntity?

    /**
     * Get album count
     */
    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int

    /**
     * Insert album
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    /**
     * Update album
     */
    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    /**
     * Delete album
     */
    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    /**
     * Delete album by ID
     */
    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbumById(albumId: String)

    /**
     * Update album name
     */
    @Query("UPDATE albums SET name = :name, modifiedAt = :modifiedAt WHERE id = :albumId")
    suspend fun updateAlbumName(albumId: String, name: String, modifiedAt: Long)

    /**
     * Update album cover
     */
    @Query("UPDATE albums SET coverUri = :coverUri, modifiedAt = :modifiedAt WHERE id = :albumId")
    suspend fun updateAlbumCover(albumId: String, coverUri: String?, modifiedAt: Long)

    /**
     * Update album modified time
     */
    @Query("UPDATE albums SET modifiedAt = :modifiedAt WHERE id = :albumId")
    suspend fun updateAlbumModifiedTime(albumId: String, modifiedAt: Long)

    /**
     * Delete all albums
     */
    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()
}
