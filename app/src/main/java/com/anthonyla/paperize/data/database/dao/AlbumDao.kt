package com.anthonyla.paperize.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.AlbumSummaryEntity
import com.anthonyla.paperize.data.database.relations.AlbumWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("""
        SELECT 
            a.id, 
            a.name, 
            a.coverUri, 
            a.createdAt, 
            a.modifiedAt,
            (SELECT COUNT(*) FROM wallpapers w WHERE w.albumId = a.id) as wallpaperCount,
            (SELECT COUNT(*) FROM folders f WHERE f.albumId = a.id) as folderCount
        FROM albums a
        ORDER BY a.modifiedAt DESC
    """)
    fun getAlbumSummaries(): Flow<List<AlbumSummaryEntity>>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getAlbumWithDetails(albumId: String): Flow<AlbumWithDetails?>

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE name = :name LIMIT 1")
    suspend fun getAlbumByName(name: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbumById(albumId: String)

    @Query("UPDATE albums SET coverUri = :coverUri, modifiedAt = :modifiedAt WHERE id = :albumId")
    suspend fun updateAlbumCover(albumId: String, coverUri: String?, modifiedAt: Long)

    @Query("UPDATE albums SET modifiedAt = :modifiedAt WHERE id = :albumId")
    suspend fun updateAlbumModifiedTime(albumId: String, modifiedAt: Long)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()
}
