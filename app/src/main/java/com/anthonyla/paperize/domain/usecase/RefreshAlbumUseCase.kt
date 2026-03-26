package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import javax.inject.Inject

/**
 * Use case to refresh album wallpapers and folders
 *
 * Validates all wallpaper URIs and folder URIs, removes invalid ones
 * Normalizes wallpaper queues after deletions to maintain queue integrity
 * Returns total number of items removed (wallpapers + folders)
 */
class RefreshAlbumUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String): Result<Int> {
        var totalRemoved = 0

        // Validate and remove invalid folders
        val foldersResult = albumRepository.validateAndRemoveInvalidFolders(albumId)
        if (foldersResult is Result.Success) {
            totalRemoved += foldersResult.data
        }

        // Validate and remove invalid wallpapers
        val wallpapersResult = wallpaperRepository.validateAndRemoveInvalidWallpapers(albumId)
        if (wallpapersResult is Result.Success) {
            totalRemoved += wallpapersResult.data
        }

        if (totalRemoved > 0) {
            albumRepository.refreshAlbumCover(albumId)
        }

        // Return error if either operation failed
        return when {
            foldersResult is Result.Error -> foldersResult
            wallpapersResult is Result.Error -> wallpapersResult
            else -> Result.Success(totalRemoved)
        }
    }
}
