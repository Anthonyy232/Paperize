package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.data.database.relations.FolderWithWallpapers
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Transaction
    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolderWithWallpapers(folderId: String): Flow<FolderWithWallpapers?>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

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
