package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.getFileName
import com.anthonyla.paperize.core.util.scanFolderImages
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Prepares imports off the UI thread; the repository writes each import atomically. */
class ImportWallpapersUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val albumRepository: AlbumRepository
) {
    suspend fun addImages(
        albumId: String,
        uris: List<String>,
        onSaving: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val album = checkNotNull(albumRepository.getAlbumById(albumId).first())
        val selectedUris = uris.distinct()
        selectedUris.forEach { value ->
            currentCoroutineContext().ensureActive()
            context.contentResolver.takePersistableUriPermission(value.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val existingUris = album.wallpapers.map { it.uri }.toHashSet()
        val newUris = selectedUris.filterNot { it in existingUris }
        if (newUris.isEmpty()) return@withContext false

        onSaving(0, newUris.size)
        val nextOrder = (album.wallpapers.maxOfOrNull { it.displayOrder } ?: -1) + 1
        val wallpapers = newUris.mapIndexed { index, value ->
            currentCoroutineContext().ensureActive()
            val uri = value.toUri()
            Wallpaper(
                id = generateId(), albumId = albumId, uri = value,
                fileName = uri.getFileName(context) ?: value.substringAfterLast('/'),
                dateModified = System.currentTimeMillis(), displayOrder = nextOrder + index
            )
        }
        albumRepository.addWallpapersToAlbum(albumId, wallpapers, onSaving).onError { throw it }
        true
    }

    suspend fun addFolder(
        albumId: String,
        value: String,
        onScanning: (Int) -> Unit,
        onSaving: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val album = checkNotNull(albumRepository.getAlbumById(albumId).first())
        val uri = value.toUri()
        // Re-selecting a folder also restores its read permission.
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (album.folders.any { it.uri == value }) return@withContext false

        val images = uri.scanFolderImages(context, onScanning).sortedBy { it.uri.toString() }
        val folderId = generateId()
        val wallpapers = images.mapIndexed { index, image ->
            currentCoroutineContext().ensureActive()
            Wallpaper(
                id = generateId(), albumId = albumId, folderId = folderId,
                uri = image.uri.toString(), fileName = image.name,
                dateModified = image.lastModified, displayOrder = index,
                sourceType = WallpaperSourceType.FOLDER
            )
        }
        onSaving(0, wallpapers.size)
        val folder = Folder(
            id = folderId, albumId = albumId, uri = value,
            name = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)?.name
                ?: value.substringAfterLast('/'),
            coverUri = wallpapers.firstOrNull()?.uri,
            dateModified = System.currentTimeMillis(),
            displayOrder = (album.folders.maxOfOrNull { it.displayOrder } ?: -1) + 1,
            wallpapers = wallpapers
        )
        albumRepository.addFolderToAlbum(albumId, folder, onSaving).onError { throw it }
        true
    }

}
