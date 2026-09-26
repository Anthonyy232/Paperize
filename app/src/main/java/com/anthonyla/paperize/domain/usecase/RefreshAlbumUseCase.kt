package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import javax.inject.Inject

/** Removes confirmed missing entries and returns the number of wallpapers and folders removed. */
class RefreshAlbumUseCase @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String): Result<Int> {
        var totalRemoved = 0

        val foldersResult = albumRepository.validateAndRemoveInvalidFolders(albumId)
        if (foldersResult is Result.Success) {
            totalRemoved += foldersResult.data
        }

        val wallpapersResult = wallpaperRepository.validateAndRemoveInvalidWallpapers(albumId)
        if (wallpapersResult is Result.Success) {
            totalRemoved += wallpapersResult.data
        }

        if (totalRemoved > 0) {
            albumRepository.refreshAlbumCover(albumId)
            albumRepository.refreshFolderCovers(albumId)
        }

        return when {
            foldersResult is Result.Error -> foldersResult
            wallpapersResult is Result.Error -> wallpapersResult
            else -> Result.Success(totalRemoved)
        }
    }
}
