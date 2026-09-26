package com.anthonyla.paperize.data.repository

import androidx.room.withTransaction
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.util.QueueBuilder
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.entities.WallpaperCurrentEntity
import com.anthonyla.paperize.data.mapper.toDomainModel
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    private val database: PaperizeDatabase
) : WallpaperRepository {

    private val wallpaperDao = database.wallpaperDao()
    private val wallpaperQueueDao = database.wallpaperQueueDao()
    private val wallpaperCurrentDao = database.wallpaperCurrentDao()

    override suspend fun getWallpaperById(wallpaperId: String): Wallpaper? =
        wallpaperDao.getWallpaperById(wallpaperId)?.toDomainModel()

    override suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): Wallpaper? =
        wallpaperQueueDao.getNextWallpaperInQueue(albumId, screenType)?.toDomainModel()

    override suspend fun getAndDequeueWallpaper(albumId: String, screenType: ScreenType): Wallpaper? =
        wallpaperQueueDao.getAndDequeueWallpaper(albumId, screenType)?.toDomainModel()

    override suspend fun removeWallpaperFromQueue(
        albumId: String,
        screenType: ScreenType,
        wallpaperId: String
    ) {
        wallpaperQueueDao.deleteQueueItem(albumId, screenType, wallpaperId)
    }

    override suspend fun restoreWallpaperToQueueFront(
        albumId: String,
        screenType: ScreenType,
        wallpaperId: String
    ) {
        wallpaperQueueDao.restoreQueueItem(albumId, screenType, wallpaperId)
    }

    override suspend fun ensureWallpaperQueue(
        albumId: String,
        screenType: ScreenType,
        shuffle: Boolean
    ): Result<Unit> = Result.runCatching {
        database.withTransaction {
            // Recheck inside the transaction: another caller may already have filled it.
            if (wallpaperQueueDao.getNextWallpaperInQueue(albumId, screenType) != null) return@withTransaction
            val otherScreen = if (shuffle) when (screenType) {
                ScreenType.HOME -> ScreenType.LOCK
                ScreenType.LOCK -> ScreenType.HOME
                else -> null
            } else null
            val otherQueue = otherScreen?.let { wallpaperQueueDao.getQueueItems(albumId, it) }.orEmpty()
            val ids = if (shuffle) {
                val available = wallpaperDao.getWallpaperIdsByAlbum(albumId)
                if (otherQueue.isEmpty()) available.shuffled()
                else QueueBuilder.mergeWithExistingQueue(otherQueue.map { it.wallpaperId }, available)
            } else {
                wallpaperDao.getOrderedWallpaperIdsByAlbum(albumId)
            }
            wallpaperQueueDao.rebuildQueue(albumId, screenType, ids)
            if (otherScreen != null && otherQueue.isEmpty()) {
                wallpaperQueueDao.rebuildQueue(albumId, otherScreen, ids)
            }
        }
    }

    override suspend fun clearAllQueues(): Result<Unit> = Result.runCatching {
        wallpaperQueueDao.deleteAllQueueItems()
    }

    override suspend fun getCurrentWallpaper(albumId: String, screenType: ScreenType): Wallpaper? =
        wallpaperCurrentDao.getCurrentWallpaper(albumId, screenType)?.toDomainModel()

    override fun getCurrentWallpaperFlow(albumId: String, screenType: ScreenType): Flow<Wallpaper?> =
        wallpaperCurrentDao.getCurrentWallpaperFlow(albumId, screenType).map { it?.toDomainModel() }

    override suspend fun setCurrentWallpaper(albumId: String, screenType: ScreenType, wallpaperId: String) {
        wallpaperCurrentDao.upsertCurrentWallpaper(
            WallpaperCurrentEntity(albumId = albumId, screenType = screenType, wallpaperId = wallpaperId)
        )
    }

}
