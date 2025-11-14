package com.anthonyla.paperize.domain.usecase

import android.net.Uri
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.getFileName
import com.anthonyla.paperize.core.util.getLastModified
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject

/**
 * Use case to add wallpapers to an album
 */
class AddWallpapersToAlbumUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String, uris: List<Uri>): Result<Unit> {
        if (uris.isEmpty()) {
            return Result.Error(IllegalArgumentException("No wallpapers provided"))
        }

        val wallpapers = uris.mapIndexed { index, uri ->
            Wallpaper(
                id = generateId(),
                albumId = albumId,
                folderId = null,
                uri = uri.toString(),
                fileName = uri.getFileName(context) ?: "Unknown",
                dateModified = uri.getLastModified(context),
                displayOrder = index,
                sourceType = WallpaperSourceType.DIRECT
            )
        }

        return albumRepository.addWallpapersToAlbum(albumId, wallpapers)
    }
}
