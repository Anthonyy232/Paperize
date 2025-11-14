package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.relations.FolderWithWallpapers
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Folder operations
 */
@Dao
interface FolderDao {
    /**
     * Get all folders for an album with their wallpapers
     */
    @Transaction
    @Query("SELECT * FROM folders WHERE albumId = :albumId ORDER BY displayOrder ASC")
    fun getFoldersWithWallpapersByAlbum(albumId: String): Flow<List<FolderWithWallpapers>>

    /**
     * Get all folders for an album (without wallpapers)
     */
    @Query("SELECT * FROM folders WHERE albumId = :albumId ORDER BY displayOrder ASC")
    fun getFoldersByAlbum(albumId: String): Flow<List<FolderEntity>>

    /**
     * Get a specific folder by ID with wallpapers
     */
    @Transaction
    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolderWithWallpapers(folderId: String): Flow<FolderWithWallpapers?>

    /**
     * Get a specific folder by ID
     */
    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: String): FolderEntity?

    /**
     * Get folder by URI
     */
    @Query("SELECT * FROM folders WHERE uri = :uri LIMIT 1")
    suspend fun getFolderByUri(uri: String): FolderEntity?

    /**
     * Get folder count for album
     */
    @Query("SELECT COUNT(*) FROM folders WHERE albumId = :albumId")
    suspend fun getFolderCountByAlbum(albumId: String): Int

    /**
     * Insert folder
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    /**
     * Insert multiple folders
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    /**
     * Update folder
     */
    @Update
    suspend fun updateFolder(folder: FolderEntity)

    /**
     * Update folder display order
     */
    @Query("UPDATE folders SET displayOrder = :order WHERE id = :folderId")
    suspend fun updateFolderOrder(folderId: String, order: Int)

    /**
     * Update folder modified time
     */
    @Query("UPDATE folders SET dateModified = :dateModified WHERE id = :folderId")
    suspend fun updateFolderModifiedTime(folderId: String, dateModified: Long)

    /**
     * Update folder cover
     */
    @Query("UPDATE folders SET coverUri = :coverUri WHERE id = :folderId")
    suspend fun updateFolderCover(folderId: String, coverUri: String?)

    /**
     * Delete folder
     */
    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    /**
     * Delete folder by ID
     */
    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    /**
     * Delete all folders for an album
     */
    @Query("DELETE FROM folders WHERE albumId = :albumId")
    suspend fun deleteFoldersByAlbum(albumId: String)

    /**
     * Delete all folders
     */
    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    /**
     * Check if folder exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE id = :folderId)")
    suspend fun folderExists(folderId: String): Boolean
}
