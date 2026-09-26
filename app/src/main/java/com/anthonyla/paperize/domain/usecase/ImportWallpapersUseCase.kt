package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.source.DocumentSource
import com.anthonyla.paperize.domain.source.SourceImage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImportWallpapersUseCase @Inject constructor(
    private val documents: DocumentSource,
    private val albumRepository: AlbumRepository
) {
    suspend fun addImages(albumId: String, uris: List<String>, onSaving: (Int, Int) -> Unit): Boolean {
        val images = uris.distinct().map { uri ->
            documents.retainReadPermission(uri)
            documents.readImage(uri).toWallpaper(albumId)
        }
        return albumRepository.addWallpapersToAlbum(albumId, images, onSaving)
            .getOrThrow() > 0
    }

    suspend fun addFolder(
        albumId: String,
        uri: String,
        onScanning: (Int) -> Unit,
        onSaving: (Int, Int) -> Unit
    ): Boolean {
        documents.retainReadPermission(uri)
        // Avoid rescanning an existing folder; insertion still checks duplicates atomically.
        if (albumRepository.getAlbumById(albumId).first()?.folders.orEmpty().any { it.uri == uri }) return false
        val source = documents.readFolder(uri, onScanning)
        val folderId = generateId()
        val folder = Folder(
            id = folderId, albumId = albumId, uri = uri, name = source.name,
            coverUri = null, dateModified = System.currentTimeMillis(),
            wallpapers = source.images.sortedBy { it.uri }.map { it.toWallpaper(albumId, folderId) }
        )
        return albumRepository.addFolderToAlbum(albumId, folder, onSaving).getOrThrow()
    }
}

internal fun SourceImage.toWallpaper(albumId: String, folderId: String? = null) = Wallpaper(
    id = generateId(), albumId = albumId, folderId = folderId, uri = uri,
    fileName = name, dateModified = modifiedAt,
    sourceType = if (folderId == null) WallpaperSourceType.DIRECT else WallpaperSourceType.FOLDER
)
