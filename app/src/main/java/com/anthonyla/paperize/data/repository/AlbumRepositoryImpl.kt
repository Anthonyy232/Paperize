package com.anthonyla.paperize.data.repository

import androidx.room.withTransaction
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.mapper.toDomainModel
import com.anthonyla.paperize.data.mapper.toEntity
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.source.DocumentSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val documents: DocumentSource,
    private val database: PaperizeDatabase
) : AlbumRepository {
    private val albumDao = database.albumDao()
    private val wallpaperDao = database.wallpaperDao()
    private val folderDao = database.folderDao()

    companion object {
        private const val WALLPAPER_BATCH_SIZE = 500
    }

    override fun getAlbumSummaries(): Flow<List<AlbumSummary>> =
        albumDao.getAlbumSummaries().map { it.map { summary -> summary.toDomainModel() } }

    override fun getAlbumById(albumId: String): Flow<Album?> =
        albumDao.getAlbumWithDetails(albumId).map { it?.toDomainModel() }

    override suspend fun getAlbumByName(name: String): Album? =
        albumDao.getAlbumByName(name)?.toDomainModel()

    override fun getFolderById(folderId: String): Flow<Folder?> =
        folderDao.getFolderWithWallpapers(folderId).map { it?.toDomainModel() }

    override suspend fun createAlbum(name: String, coverUri: String?): Result<Album> = Result.runCatching {
        val now = System.currentTimeMillis()
        val album = Album(id = generateId(), name = name, coverUri = coverUri, createdAt = now, modifiedAt = now)
        albumDao.insertAlbum(album.toEntity())
        album
    }

    override suspend fun deleteAlbum(albumId: String): Result<Unit> = Result.runCatching {
        albumDao.deleteAlbumById(albumId)
    }

    override suspend fun addWallpapersToAlbum(
        albumId: String,
        wallpapers: List<Wallpaper>,
        onProgress: (saved: Int, total: Int) -> Unit
    ): Result<Int> = Result.runCatching {
        database.withTransaction {
            checkNotNull(albumDao.getAlbumById(albumId))
            insertNewWallpapers(albumId, wallpapers, onProgress)
        }
    }

    override suspend fun addFolderToAlbum(
        albumId: String,
        folder: Folder,
        onProgress: (saved: Int, total: Int) -> Unit
    ): Result<Boolean> = Result.runCatching {
        database.withTransaction {
            checkNotNull(albumDao.getAlbumById(albumId))
            if (folderDao.containsUri(albumId, folder.uri)) return@withTransaction false
            folderDao.insertFolder(folder.toEntity().copy(
                albumId = albumId, displayOrder = folderDao.getMaxOrder(albumId) + 1, coverUri = null
            ))
            insertNewWallpapers(albumId, folder.wallpapers.map { it.copy(folderId = folder.id) }, onProgress)
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            true
        }
    }

    private suspend fun insertNewWallpapers(
        albumId: String,
        wallpapers: List<Wallpaper>,
        onProgress: (Int, Int) -> Unit
    ): Int {
        var nextOrder = wallpaperDao.getMaxOrder(albumId) + 1
        val additions = wallpapers.groupBy { it.folderId }.flatMap { (folderId, images) ->
            // A folder can be removed while its provider scan is in flight.
            if (folderId != null && folderDao.getFolderById(folderId)?.albumId != albumId) {
                emptyList()
            } else {
                val existing = wallpaperDao.getUrisInCollection(albumId, folderId).toHashSet()
                images.filter { existing.add(it.uri) }.map {
                    it.copy(albumId = albumId, displayOrder = nextOrder++)
                }
            }
        }
        if (additions.isEmpty()) return 0
        insertWallpapersChunked(additions, onProgress)
        additions.mapNotNull { it.folderId }.toSet().forEach { folderId ->
            folderDao.updateFolderCover(folderId, wallpaperDao.getFolderCoverUri(folderId))
        }
        database.wallpaperQueueDao().clearAllQueues(albumId)
        albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
        updateAlbumCoverIfNeeded(albumId)
        return additions.size
    }

    /** Report chunk progress within the caller's transaction; cancellation rolls back every chunk. */
    private suspend fun insertWallpapersChunked(
        wallpapers: List<Wallpaper>,
        onProgress: (saved: Int, total: Int) -> Unit
    ) {
        val total = wallpapers.size
        var saved = 0
        wallpapers.chunked(WALLPAPER_BATCH_SIZE).forEach { chunk ->
            wallpaperDao.insertWallpapers(chunk.map { it.toEntity() })
            saved += chunk.size
            onProgress(saved, total)
        }
    }

    override suspend fun reorderAlbum(
        albumId: String,
        folders: List<Folder>,
        wallpapers: List<Wallpaper>
    ): Result<Unit> = Result.runCatching {
        database.withTransaction {
            folders.forEachIndexed { index, folder ->
                folderDao.updateFolderOrder(albumId, folder.id, index)
            }
            // Rotation follows direct images, then each folder in the saved order.
            (wallpapers.asSequence() + folders.asSequence().flatMap { it.wallpapers })
                .forEachIndexed { index, wallpaper ->
                    wallpaperDao.updateAlbumWallpaperOrder(albumId, wallpaper.id, index)
                }
            folderDao.refreshFolderCovers(albumId)
            updateAlbumCoverIfNeeded(albumId)
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            database.wallpaperQueueDao().clearAllQueues(albumId)
        }
    }

    override suspend fun removeWallpapersFromAlbum(
        albumId: String,
        wallpaperIds: List<String>
    ): Result<Unit> = Result.runCatching {
        if (wallpaperIds.isEmpty()) return@runCatching
        database.withTransaction {
            wallpaperIds.chunked(WALLPAPER_BATCH_SIZE).forEach {
                wallpaperDao.deleteAlbumWallpapers(albumId, it)
            }
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            folderDao.refreshFolderCovers(albumId)
            updateAlbumCoverIfNeeded(albumId)
        }
    }

    override suspend fun removeFolderFromAlbum(albumId: String, folderId: String): Result<Unit> = Result.runCatching {
        database.withTransaction {
            folderDao.deleteAlbumFolder(albumId, folderId)
            albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            updateAlbumCoverIfNeeded(albumId)
        }
    }

    override suspend fun deleteAllAlbums(): Result<Unit> = Result.runCatching {
        albumDao.deleteAllAlbums()
    }

    override suspend fun pruneMissingEntries(albumId: String): Result<Int> = Result.runCatching {
        val missingFolders = folderDao.getFoldersByAlbum(albumId)
            .filter { documents.isMissing(it.uri, isTree = true) }.map { it.id }.toSet()
        val missingImages = mutableListOf<String>()
        var afterId: String? = null
        while (true) {
            val batch = wallpaperDao.getWallpapersByAlbumPage(albumId, WALLPAPER_BATCH_SIZE, afterId)
            if (batch.isEmpty()) break
            batch.filter { it.folderId !in missingFolders && documents.isMissing(it.uri) }
                .mapTo(missingImages) { it.id }
            afterId = batch.last().id
        }
        database.withTransaction {
            var removed = missingFolders.sumOf { folderDao.deleteAlbumFolder(albumId, it) }
            missingImages.chunked(WALLPAPER_BATCH_SIZE).forEach {
                removed += wallpaperDao.deleteAlbumWallpapers(albumId, it)
            }
            if (removed > 0) {
                folderDao.refreshFolderCovers(albumId)
                updateAlbumCoverIfNeeded(albumId)
                albumDao.updateAlbumModifiedTime(albumId, System.currentTimeMillis())
            }
            removed
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
