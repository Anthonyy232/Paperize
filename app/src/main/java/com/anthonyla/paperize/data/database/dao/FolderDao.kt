package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.relations.FolderWithWallpapers
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE albumId = :albumId ORDER BY displayOrder, id")
    suspend fun getFoldersByAlbum(albumId: String): List<FolderEntity>

    @Query("DELETE FROM folders WHERE id = :folderId AND albumId = :albumId")
    suspend fun deleteAlbumFolder(albumId: String, folderId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE albumId = :albumId AND uri = :uri)")
    suspend fun containsUri(albumId: String, uri: String): Boolean

    @Query("SELECT COALESCE(MAX(displayOrder), -1) FROM folders WHERE albumId = :albumId")
    suspend fun getMaxOrder(albumId: String): Int

    @Transaction
    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolderWithWallpapers(folderId: String): Flow<FolderWithWallpapers?>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET displayOrder = :order WHERE id = :folderId AND albumId = :albumId")
    suspend fun updateFolderOrder(albumId: String, folderId: String, order: Int)

    @Query("UPDATE folders SET coverUri = :coverUri WHERE id = :folderId")
    suspend fun updateFolderCover(folderId: String, coverUri: String?)

    @Query("""
        UPDATE folders SET coverUri = (
            SELECT uri FROM wallpapers WHERE folderId = folders.id ORDER BY displayOrder, id LIMIT 1
        ) WHERE albumId = :albumId
    """)
    suspend fun refreshFolderCovers(albumId: String)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)
}
