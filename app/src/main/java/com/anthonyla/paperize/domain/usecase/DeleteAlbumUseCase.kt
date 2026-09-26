package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.AlbumRepository
import javax.inject.Inject

class DeleteAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(albumId: String): Result<Unit> {
        val result = albumRepository.deleteAlbum(albumId)
        if (result is Result.Success) {
            settingsRepository.clearAlbumSelectionsIfMatches(albumId)
        }
        return result
    }
}
