package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Use case to delete an album
 */
class DeleteAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String): Result<Unit> {
        return albumRepository.deleteAlbum(albumId)
    }
}
