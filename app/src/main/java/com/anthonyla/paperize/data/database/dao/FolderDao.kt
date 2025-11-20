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
     * Insert folder
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    /**
     * Update folder
     */
    @Update
    suspend fun updateFolder(folder: FolderEntity)

    /**
     * Update folder cover
     */
    @Query("UPDATE folders SET coverUri = :coverUri WHERE id = :folderId")
    suspend fun updateFolderCover(folderId: String, coverUri: String?)

    /**
     * Delete folder by ID
     */
    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)
}
