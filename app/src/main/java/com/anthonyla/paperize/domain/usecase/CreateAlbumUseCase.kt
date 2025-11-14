package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Use case to create a new album
 */
class CreateAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(name: String, coverUri: String? = null): Result<Album> {
        if (name.isBlank()) {
            return Result.Error(IllegalArgumentException("Album name cannot be empty"))
        }
        return albumRepository.createAlbum(name, coverUri)
    }
}
