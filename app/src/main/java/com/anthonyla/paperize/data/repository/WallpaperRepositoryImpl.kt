package com.anthonyla.paperize.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.isValid
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.dao.WallpaperQueueDao
import com.anthonyla.paperize.data.mapper.toDomainModel
import com.anthonyla.paperize.data.mapper.toDomainModels
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of WallpaperRepository
 */
@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperDao: WallpaperDao,
    private val wallpaperQueueDao: WallpaperQueueDao
) : WallpaperRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Mutexes to synchronize queue rebuild operations per (albumId, screenType)
     * Prevents duplicate rebuilds when multiple threads detect empty queue simultaneously
     */
    private val queueRebuildMutexes = ConcurrentHashMap<String, Mutex>()

    override fun getWallpapersByAlbum(albumId: String): Flow<List<Wallpaper>> =
        wallpaperDao.getWallpapersByAlbum(albumId).map { it.toDomainModels() }

    override fun getDirectWallpapersByAlbum(albumId: String): Flow<List<Wallpaper>> =
        wallpaperDao.getDirectWallpapersByAlbum(albumId).map { it.toDomainModels() }

    override fun getWallpapersByFolder(folderId: String): Flow<List<Wallpaper>> =
        wallpaperDao.getWallpapersByFolder(folderId).map { it.toDomainModels() }

    override suspend fun getWallpaperById(wallpaperId: String): Wallpaper? =
        wallpaperDao.getWallpaperById(wallpaperId)?.toDomainModel()

    override suspend fun addWallpaper(wallpaper: Wallpaper): Result<Unit> {
        return try {
            wallpaperDao.insertWallpaper(wallpaper.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateWallpaper(wallpaper: Wallpaper): Result<Unit> {
        return try {
            wallpaperDao.updateWallpaper(wallpaper.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteWallpaper(wallpaperId: String): Result<Unit> {
        return try {
            wallpaperDao.deleteWallpaperById(wallpaperId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteWallpapers(wallpaperIds: List<String>): Result<Unit> {
        return try {
            if (wallpaperIds.isEmpty()) {
                return Result.Success(Unit)
            }
            wallpaperDao.deleteWallpapersByIds(wallpaperIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteWallpapersByFolderId(folderId: String): Result<Unit> {
        return try {
            wallpaperDao.deleteWallpapersByFolder(folderId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateWallpaperOrder(wallpaperId: String, order: Int): Result<Unit> {
        return try {
            wallpaperDao.updateWallpaperOrder(wallpaperId, order)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun validateAndRemoveInvalidWallpapers(albumId: String): Result<Int> {
        return try {
            var totalRemoved = 0
            val batchSize = 100 // Process in smaller batches to avoid memory spikes
            var offset = 0

            // Use while loop to handle deletions correctly
            // When items are deleted, we don't advance offset since indices shift
            while (true) {
                val wallpapers = wallpaperDao.getWallpapersByAlbumPaged(albumId, batchSize, offset)
                if (wallpapers.isEmpty()) break

                val invalidUris = wallpapers
                    .filter { !it.uri.toUri().isValid(contentResolver) }
                    .map { it.uri }

                if (invalidUris.isNotEmpty()) {
                    wallpaperDao.deleteWallpapersByUris(invalidUris)
                    totalRemoved += invalidUris.size
                    // Don't advance offset when deleting - remaining items shift down
                } else {
                    // Only advance offset if no deletions (all items in batch were valid)
                    offset += batchSize
                }
            }

            Result.Success(totalRemoved)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): Wallpaper? =
        wallpaperQueueDao.getNextWallpaperInQueue(albumId, screenType)?.toDomainModel()

    override suspend fun getAndDequeueWallpaper(albumId: String, screenType: ScreenType): Wallpaper? =
        wallpaperQueueDao.getAndDequeueWallpaper(albumId, screenType)?.toDomainModel()

    override suspend fun buildWallpaperQueue(
        albumId: String,
        screenType: ScreenType,
        shuffle: Boolean
    ): Result<Unit> {
        // In shuffle mode, use album-level mutex to ensure synchronized shuffle order
        // In sequential mode, use per-screen mutex for independent rebuilds
        val mutexKey = if (shuffle) albumId else "$albumId:$screenType"
        val mutex = queueRebuildMutexes.getOrPut(mutexKey) { Mutex() }

        return mutex.withLock {
            try {
                val wallpapers = wallpaperDao.getWallpapersByAlbum(albumId).first()

                // If shuffle is enabled, check if there's an existing queue for another screen type
                // with the same album to maintain synchronized order
                val otherScreenType = if (shuffle) {
                    when (screenType) {
                        ScreenType.HOME -> ScreenType.LOCK
                        ScreenType.LOCK -> ScreenType.HOME
                        ScreenType.BOTH -> null
                        ScreenType.LIVE -> null
                    }
                } else null

                val otherScreenQueueIds = otherScreenType?.let {
                    try {
                        wallpaperQueueDao.getQueueItems(albumId, it)
                            .map { queueItem -> queueItem.wallpaperId }
                    } catch (_: Exception) {
                        null
                    }
                }

                val wallpaperIds = com.anthonyla.paperize.core.util.QueueBuilder.buildQueue(
                    wallpapers = wallpapers.toDomainModels(),
                    shuffle = shuffle,
                    otherScreenQueue = otherScreenQueueIds
                )

                wallpaperQueueDao.rebuildQueue(albumId, screenType, wallpaperIds)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun clearAllQueues(): Result<Unit> {
        return try {
            wallpaperQueueDao.deleteAllQueueItems()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun clearQueuesForAlbum(albumId: String): Result<Unit> {
        return try {
            wallpaperQueueDao.clearAllQueues(albumId)
            
            // Prune mutexes for this album to prevent memory leak
            // Keys are either albumId (shuffled) or $albumId:$screenType (sequential)
            // Use exact match or prefix with colon to avoid false positives (e.g., album1 vs album10)
            queueRebuildMutexes.keys.filter { it == albumId || it.startsWith("$albumId:") }.forEach { 
                queueRebuildMutexes.remove(it)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun normalizeAllQueuesForAlbum(albumId: String): Result<Unit> {
        return try {
            // Normalize all queue types for this album
            // This fixes gaps in queue positions caused by CASCADE deletes
            wallpaperQueueDao.normalizeQueuePositions(albumId, ScreenType.HOME)
            wallpaperQueueDao.normalizeQueuePositions(albumId, ScreenType.LOCK)
            wallpaperQueueDao.normalizeQueuePositions(albumId, ScreenType.LIVE)
            wallpaperQueueDao.normalizeQueuePositions(albumId, ScreenType.BOTH)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun scanFolderForWallpapers(folderUri: Uri): Result<List<Wallpaper>> {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
                ?: return Result.Error(Exception("Invalid folder URI"))

            val wallpapers = mutableListOf<Wallpaper>()
            scanDirectoryRecursive(documentFile, wallpapers)

            Result.Success(wallpapers)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun scanDirectoryRecursive(directory: DocumentFile, result: MutableList<Wallpaper>) {
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                scanDirectoryRecursive(file, result)
            } else if (file.isFile && file.name?.let { name -> isImageFile(name) } == true) {
                try {
                    val uri = file.uri.toString()
                    val fileName = file.name ?: return@forEach
                    val dateModified = file.lastModified()

                    result.add(
                        Wallpaper(
                            id = generateId(),
                            albumId = "", 
                            folderId = null,
                            uri = uri,
                            fileName = fileName,
                            dateModified = dateModified,
                            sourceType = WallpaperSourceType.FOLDER
                        )
                    )
                } catch (_: Exception) {
                    // Skip failed items
                }
            }
        }
    }

    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in Constants.SUPPORTED_IMAGE_EXTENSIONS
    }

    override suspend fun isWallpaperInAlbum(albumId: String, uri: String): Boolean =
        wallpaperDao.getWallpaperByUri(uri)?.let { it.albumId == albumId } ?: false
}
