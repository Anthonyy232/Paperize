package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.source.DocumentSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RefreshFolderUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val documents: DocumentSource
) {
    suspend operator fun invoke(folderId: String): Result<Int> = Result.runCatching {
        val folder = albumRepository.getFolderById(folderId).first() ?: return@runCatching 0
        val source = documents.readFolder(folder.uri)
        albumRepository.addWallpapersToAlbum(
            folder.albumId, source.images.map { it.toWallpaper(folder.albumId, folderId) }
        ).getOrThrow()
    }
}
