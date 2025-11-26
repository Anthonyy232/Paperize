package com.anthonyla.paperize.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.dao.AlbumDao
import com.anthonyla.paperize.data.database.dao.FolderDao
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.mapper.toDomainModel
import com.anthonyla.paperize.data.mapper.toDomainModelsFromRelations
import com.anthonyla.paperize.data.mapper.toEntities
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    @param:ApplicationContext private val context: Context,
    private val database: PaperizeDatabase,
    private val albumDao: AlbumDao,
    private val wallpaperDao: WallpaperDao,
    private val folderDao: FolderDao
) : AlbumRepository {

    override fun getAllAlbums(): Flow<List<Album>> =
        albumDao.getAllAlbumsWithDetails().map { it.toDomainModelsFromRelations() }

    override fun getAlbumById(albumId: String): Flow<Album?> =
        albumDao.getAlbumWithDetails(albumId).map { it?.toDomainModel() }

    override suspend fun getAlbumByName(name: String): Album? =
        albumDao.getAlbumByName(name)?.toDomainModel()

    override fun getFolderById(folderId: String): Flow<Folder?> =
        folderDao.getFolderWithWallpapers(folderId).map { it?.toDomainModel() }

    override suspend fun createAlbum(name: String, coverUri: String?): Result<Album> {
        return try {
            val album = Album(
                id = generateId(),
                name = name,
                coverUri = coverUri,
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
            // Atomic transaction - insert wallpapers, update timestamp, and update cover
            database.withTransaction {
                wallpaperDao.insertWallpapers(wallpapers.toEntities())
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                // Update album cover if it doesn't have one
                updateAlbumCoverIfNeeded(albumId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addFolderToAlbum(albumId: String, folder: Folder): Result<Unit> {
        return try {
            // Atomic transaction - folder, wallpapers, timestamp, and cover all succeed or all fail
            database.withTransaction {
                folderDao.insertFolder(folder.toEntity())
                // Insert folder wallpapers
                wallpaperDao.insertWallpapers(folder.wallpapers.toEntities())
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                // Update album cover if it doesn't have one
                updateAlbumCoverIfNeeded(albumId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        return try {
            folderDao.updateFolder(folder.toEntity())
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
            // Atomic transaction - delete wallpaper, update timestamp, and refresh cover
            database.withTransaction {
                val wallpaper = wallpaperDao.getWallpaperById(wallpaperId)
                wallpaperDao.deleteWallpaperById(wallpaperId)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                // Refresh cover if deleted wallpaper was the cover
                val album = albumDao.getAlbumById(albumId)
                if (album?.coverUri == wallpaper?.uri) {
                    updateAlbumCoverIfNeeded(albumId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeWallpapersFromAlbum(
        albumId: String,
        wallpaperIds: List<String>
    ): Result<Unit> {
        return try {
            if (wallpaperIds.isEmpty()) {
                return Result.Success(Unit)
            }

            // Atomic transaction - delete wallpapers, update timestamp, and refresh cover
            database.withTransaction {
                val album = albumDao.getAlbumById(albumId)
                val currentCoverUri = album?.coverUri

                // Batch delete wallpapers
                wallpaperDao.deleteWallpapersByIds(wallpaperIds)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                // Refresh cover if any deleted wallpaper was the cover
                // (we check by querying the album cover to see if it still exists)
                if (currentCoverUri != null) {
                    val coverStillExists = wallpaperDao.getWallpaperByUri(currentCoverUri) != null
                    if (!coverStillExists) {
                        updateAlbumCoverIfNeeded(albumId)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeFolderFromAlbum(albumId: String, folderId: String): Result<Unit> {
        return try {
            // Atomic transaction - delete folder, update timestamp, and refresh cover
            database.withTransaction {
                folderDao.deleteFolderById(folderId)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                // Always refresh cover after folder deletion (folder wallpapers might be cover)
                updateAlbumCoverIfNeeded(albumId)
            }
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

    override suspend fun validateAndRemoveInvalidFolders(albumId: String): Result<Int> {
        return try {
            val album = albumDao.getAlbumWithDetails(albumId).first() ?: return Result.Success(0)
            val folders = album.folders.map { it.toDomainModel() }

            val invalidFolderIds = folders.filter { folder ->
                // Check if folder URI is still valid
                val uri = Uri.parse(folder.uri)
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                documentFile == null || !documentFile.exists() || !documentFile.canRead()
            }.map { it.id }

            if (invalidFolderIds.isNotEmpty()) {
                // Atomic transaction - delete all invalid folders or none
                database.withTransaction {
                    invalidFolderIds.forEach { folderId ->
                        folderDao.deleteFolderById(folderId)
                    }
                }
            }

            Result.Success(invalidFolderIds.size)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshAlbumCover(albumId: String): Result<Unit> {
        return try {
            database.withTransaction {
                updateAlbumCoverIfNeeded(albumId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshFolderCovers(albumId: String): Result<Unit> {
        return try {
            // Get all folders in the album with their wallpapers
            val folders = folderDao.getFoldersWithWallpapersByAlbum(albumId).first()

            // Update each folder's cover to its first wallpaper
            folders.forEach { folderWithWallpapers ->
                val folder = folderWithWallpapers.folder
                val wallpapers = folderWithWallpapers.wallpapers
                val newCoverUri = wallpapers.firstOrNull()?.uri

                // Only update if cover changed
                if (folder.coverUri != newCoverUri) {
                    folderDao.updateFolderCover(folder.id, newCoverUri)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Update album cover art based on available wallpapers
     * Priority: 1) First direct wallpaper, 2) First wallpaper from first folder
     * NOTE: This must be called within a transaction
     */
    private suspend fun updateAlbumCoverIfNeeded(albumId: String) {
        val album = albumDao.getAlbumById(albumId) ?: return

        // Try to get first direct wallpaper (uploaded directly, not from folder)
        val directWallpapers = wallpaperDao.getDirectWallpapersByAlbumSync(albumId)
        val newCoverUri = when {
            // Priority 1: First direct wallpaper
            directWallpapers.isNotEmpty() -> directWallpapers.first().uri

            // Priority 2: First wallpaper from any folder
            else -> {
                val allWallpapers = wallpaperDao.getWallpapersByAlbumSync(albumId)
                allWallpapers.firstOrNull()?.uri
            }
        }

        // Only update if cover changed
        if (album.coverUri != newCoverUri) {
            albumDao.updateAlbumCover(albumId, newCoverUri, System.currentTimeMillis())
        }
    }
}
