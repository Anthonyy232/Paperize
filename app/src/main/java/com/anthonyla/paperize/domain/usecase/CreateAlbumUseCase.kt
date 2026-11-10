package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.domain.repository.AlbumRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CreateAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    /** Returns false, creating nothing, when an album already has this name ignoring case. */
    suspend operator fun invoke(name: String): Boolean {
        if (albumRepository.getAlbumSummaries().first().any { it.name.equals(name, ignoreCase = true) }) return false
        albumRepository.createAlbum(name)
        return true
    }
}
