package com.anthonyla.paperize.data.repository

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.data.database.dao.AlbumDao
import com.anthonyla.paperize.data.database.dao.FolderDao
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.mapper.toDomainModelsFromRelations
import com.anthonyla.paperize.data.mapper.toEntities
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AlbumRepository
 *
 * Handles data operations for albums using Room database
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val wallpaperDao: WallpaperDao,
    private val folderDao: FolderDao
) : AlbumRepository {

    override fun getAllAlbums(): Flow<List<Album>> =
        albumDao.getAllAlbumsWithDetails().map { it.toDomainModelsFromRelations() }

    override fun getAlbumById(albumId: String): Flow<Album?> =
        albumDao.getAlbumWithDetails(albumId).map { it?.toDomainModel() }

    override fun getSelectedAlbums(): Flow<List<Album>> =
        albumDao.getSelectedAlbumsWithDetails().map { it.toDomainModelsFromRelations() }

    override suspend fun createAlbum(name: String, coverUri: String?): Result<Album> {
        return try {
            val album = Album(
                id = generateId(),
                name = name,
                coverUri = coverUri,
                isSelected = false,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            albumDao.insertAlbum(album.toEntity())
            Result.Success(album)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAlbum(album: Album): Result<Unit> {
        return try {
            albumDao.updateAlbum(album.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAlbum(albumId: String): Result<Unit> {
        return try {
            albumDao.deleteAlbumById(albumId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setAlbumSelected(albumId: String, isSelected: Boolean): Result<Unit> {
        return try {
            albumDao.updateAlbumSelection(albumId, isSelected)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deselectAllAlbums(): Result<Unit> {
        return try {
            albumDao.deselectAllAlbums()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAlbumName(albumId: String, name: String): Result<Unit> {
        return try {
            albumDao.updateAlbumName(albumId, name, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAlbumCover(albumId: String, coverUri: String?): Result<Unit> {
        return try {
            albumDao.updateAlbumCover(albumId, coverUri, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addWallpapersToAlbum(
        albumId: String,
        wallpapers: List<Wallpaper>
    ): Result<Unit> {
        return try {
            wallpaperDao.insertWallpapers(wallpapers.toEntities())
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addFolderToAlbum(albumId: String, folder: Folder): Result<Unit> {
        return try {
            folderDao.insertFolder(folder.toEntity())
            // Insert folder wallpapers
            wallpaperDao.insertWallpapers(folder.wallpapers.toEntities())
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeWallpaperFromAlbum(
        albumId: String,
        wallpaperId: String
    ): Result<Unit> {
        return try {
            wallpaperDao.deleteWallpaperById(wallpaperId)
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeFolderFromAlbum(albumId: String, folderId: String): Result<Unit> {
        return try {
            folderDao.deleteFolderById(folderId)
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAlbumCount(): Int = albumDao.getAlbumCount()

    override suspend fun deleteAllAlbums(): Result<Unit> {
        return try {
            albumDao.deleteAllAlbums()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
