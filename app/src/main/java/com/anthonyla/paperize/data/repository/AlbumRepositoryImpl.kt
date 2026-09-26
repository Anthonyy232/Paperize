package com.anthonyla.paperize.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.isDocumentMissing
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.dao.AlbumDao
import com.anthonyla.paperize.data.database.dao.FolderDao
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.mapper.toDomainModel
import com.anthonyla.paperize.data.mapper.toDomainModelsFromRelations
import com.anthonyla.paperize.data.mapper.toDomainModelsFromSummaries
import com.anthonyla.paperize.data.mapper.toEntities
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: PaperizeDatabase,
    private val albumDao: AlbumDao,
    private val wallpaperDao: WallpaperDao,
    private val folderDao: FolderDao,
    private val wallpaperRepository: dagger.Lazy<com.anthonyla.paperize.domain.repository.WallpaperRepository>
) : AlbumRepository {

    companion object {
        private const val WALLPAPER_BATCH_SIZE = 500
    }

    override fun getAlbumSummaries(): Flow<List<AlbumSummary>> =
        albumDao.getAlbumSummaries().map { it.toDomainModelsFromSummaries() }

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAlbum(album: Album): Result<Unit> {
        return try {
            albumDao.updateAlbum(album.toEntity())
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAlbum(albumId: String): Result<Unit> {
        return try {
            database.withTransaction {
                albumDao.deleteAlbumById(albumId)
                wallpaperRepository.get().clearQueuesForAlbum(albumId)
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAlbumName(albumId: String, name: String): Result<Unit> {
        return try {
            albumDao.updateAlbumName(albumId, name, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAlbumCover(albumId: String, coverUri: String?): Result<Unit> {
        return try {
            albumDao.updateAlbumCover(albumId, coverUri, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addWallpapersToAlbum(
        albumId: String,
        wallpapers: List<Wallpaper>,
        onProgress: (saved: Int, total: Int) -> Unit
    ): Result<Unit> {
        return try {
            database.withTransaction {
                insertWallpapersChunked(wallpapers, onProgress)
                wallpapers.mapNotNull { it.folderId }.toSet().forEach { folderId ->
                    folderDao.updateFolderCover(folderId, wallpaperDao.getFolderCoverUri(folderId))
                }
                database.wallpaperQueueDao().clearAllQueues(albumId)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                updateAlbumCoverIfNeeded(albumId)
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addFolderToAlbum(
        albumId: String,
        folder: Folder,
        onProgress: (saved: Int, total: Int) -> Unit
    ): Result<Unit> {
        return try {
            database.withTransaction {
                folderDao.insertFolder(folder.toEntity())
                insertWallpapersChunked(folder.wallpapers, onProgress)
                database.wallpaperQueueDao().clearAllQueues(albumId)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                updateAlbumCoverIfNeeded(albumId)
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Insert [wallpapers] in chunks, mapping each chunk to entities only as it is written so the
     * full entity list is never materialized at once, and reporting progress after each chunk.
     * Must be called inside a transaction so the overall insert stays atomic.
     */
    private suspend fun insertWallpapersChunked(
        wallpapers: List<Wallpaper>,
        onProgress: (saved: Int, total: Int) -> Unit
    ) {
        val total = wallpapers.size
        var saved = 0
        wallpapers.chunked(WALLPAPER_BATCH_SIZE).forEach { chunk ->
            wallpaperDao.insertWallpapers(chunk.toEntities())
            saved += chunk.size
            onProgress(saved, total)
        }
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        return try {
            folderDao.updateFolder(folder.toEntity())
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
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

            database.withTransaction {
                val album = albumDao.getAlbumById(albumId)
                val currentCoverUri = album?.coverUri

                wallpaperIds.chunked(WALLPAPER_BATCH_SIZE).forEach {
                    wallpaperDao.deleteWallpapersByIds(it)
                }
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                if (currentCoverUri != null && !wallpaperDao.containsUri(albumId, currentCoverUri)) {
                    updateAlbumCoverIfNeeded(albumId)
                }
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeFolderFromAlbum(albumId: String, folderId: String): Result<Unit> {
        return try {
            database.withTransaction {
                folderDao.deleteFolderById(folderId)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())

                // Always refresh cover after folder deletion (folder wallpapers might be cover)
                updateAlbumCoverIfNeeded(albumId)
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAlbumCount(): Int = albumDao.getAlbumCount()

    override suspend fun deleteAllAlbums(): Result<Unit> {
        return try {
            database.withTransaction {
                albumDao.deleteAllAlbums()
                wallpaperRepository.get().clearAllQueues()
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun validateAndRemoveInvalidFolders(albumId: String): Result<Int> {
        return try {
            val album = albumDao.getAlbumWithDetails(albumId).first() ?: return Result.Success(0)
            val folders = album.folders.map { it.toDomainModel() }

            val invalidFolderIds = withContext(Dispatchers.IO) {
                folders.filter { folder ->
                    val treeUri = Uri.parse(folder.uri)
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri, DocumentsContract.getTreeDocumentId(treeUri)
                    )
                    documentUri.isDocumentMissing(context.contentResolver)
                }.map { it.id }
            }

            if (invalidFolderIds.isNotEmpty()) {
                database.withTransaction {
                    invalidFolderIds.forEach { folderId ->
                        folderDao.deleteFolderById(folderId)
                    }
                    updateAlbumCoverIfNeeded(albumId)
                }
            }

            Result.Success(invalidFolderIds.size)
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshFolderCovers(albumId: String): Result<Unit> {
        return try {
            folderDao.refreshFolderCovers(albumId)
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Call within a transaction; direct wallpapers take priority over folder images. */
    private suspend fun updateAlbumCoverIfNeeded(albumId: String) {
        val album = albumDao.getAlbumById(albumId) ?: return
        val newCoverUri = wallpaperDao.getAlbumCoverUri(albumId)

        if (album.coverUri != newCoverUri) {
            albumDao.updateAlbumCover(albumId, newCoverUri, System.currentTimeMillis())
        }
    }
}
