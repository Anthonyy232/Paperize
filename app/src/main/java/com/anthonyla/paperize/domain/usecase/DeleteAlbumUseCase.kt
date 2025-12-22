package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.data.datastore.PreferencesManager
import com.anthonyla.paperize.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Use case to delete an album
 */
class DeleteAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(albumId: String): Result<Unit> {
        val result = albumRepository.deleteAlbum(albumId)
        if (result is Result.Success) {
            preferencesManager.clearAlbumSelectionsIfMatches(albumId)
        }
        return result
    }
}
