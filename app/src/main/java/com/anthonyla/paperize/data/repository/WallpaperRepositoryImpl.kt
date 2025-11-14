package com.anthonyla.paperize.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
import javax.inject.Inject
import javax.inject.Singleton

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

    override fun getWallpapersByAlbum(albumId: String): Flow<List<Wallpaper>> =
        wallpaperDao.getWallpapersByAlbum(albumId).map { it.toDomainModels() }

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
            val wallpapers = wallpaperDao.getWallpapersByAlbum(albumId).first()
            val invalidUris = wallpapers
                .filter { !Uri.parse(it.uri).isValid(contentResolver) }
                .map { it.uri }

            if (invalidUris.isNotEmpty()) {
                wallpaperDao.deleteWallpapersByUris(invalidUris)
            }

            Result.Success(invalidUris.size)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getNextWallpaperInQueue(albumId: String, screenType: ScreenType): Wallpaper? =
        wallpaperQueueDao.getNextWallpaperInQueue(albumId, screenType)?.toDomainModel()

    override suspend fun buildWallpaperQueue(
        albumId: String,
        screenType: ScreenType,
        shuffle: Boolean
    ): Result<Unit> {
        return try {
            val wallpapers = wallpaperDao.getWallpapersByAlbum(albumId).first()
            val wallpaperIds = if (shuffle) {
                wallpapers.map { it.id }.shuffled()
            } else {
                wallpapers.sortedBy { it.displayOrder }.map { it.id }
            }

            wallpaperQueueDao.rebuildQueue(albumId, screenType, wallpaperIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun dequeueWallpaper(albumId: String, screenType: ScreenType): Result<Unit> {
        return try {
            wallpaperQueueDao.dequeueWallpaper(albumId, screenType)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun scanFolderForWallpapers(folderUri: Uri): Result<List<Wallpaper>> {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
                ?: return Result.Error(Exception("Invalid folder URI"))

            val wallpapers = documentFile.listFiles()
                .filter { it.isFile && it.name?.let { name -> isImageFile(name) } == true }
                .mapNotNull { file ->
                    try {
                        val uri = file.uri.toString()
                        val fileName = file.name ?: return@mapNotNull null
                        val dateModified = file.lastModified()

                        Wallpaper(
                            id = generateId(),
                            albumId = "", // Will be set when adding to album
                            folderId = null, // Will be set when adding to folder
                            uri = uri,
                            fileName = fileName,
                            dateModified = dateModified,
                            sourceType = WallpaperSourceType.FOLDER
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

            Result.Success(wallpapers)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in Constants.SUPPORTED_IMAGE_EXTENSIONS
    }
}
