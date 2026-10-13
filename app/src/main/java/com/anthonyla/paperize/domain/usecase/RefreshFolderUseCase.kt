package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.source.DocumentSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RefreshFolderUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val documents: DocumentSource
) {
    /** Returns the number of new images. */
    suspend operator fun invoke(folderId: String): Int {
        val folder = albumRepository.getFolderById(folderId).first() ?: return 0
        val source = documents.readFolder(folder.uri)
        return albumRepository.addWallpapersToAlbum(
            folder.albumId, source.images.map { it.toWallpaper(folder.albumId, folderId) }
        )
    }
}
