package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import javax.inject.Inject

/**
 * Use case to refresh album wallpapers
 *
 * Validates all wallpaper URIs and removes invalid ones
 */
class RefreshAlbumUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository
) {
    suspend operator fun invoke(albumId: String): Result<Int> {
        return wallpaperRepository.validateAndRemoveInvalidWallpapers(albumId)
    }
}
