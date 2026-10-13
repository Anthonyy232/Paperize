package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.AlbumRepository
import javax.inject.Inject

class DeleteAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(albumId: String) {
        albumRepository.deleteAlbum(albumId)
        settingsRepository.clearAlbumSelectionsIfMatches(albumId)
    }
}
