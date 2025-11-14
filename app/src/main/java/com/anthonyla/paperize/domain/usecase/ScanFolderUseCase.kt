package com.anthonyla.paperize.domain.usecase

import android.net.Uri
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.util.generateId
import com.anthonyla.paperize.core.util.getFileName
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import javax.inject.Inject

/**
 * Use case to scan a folder and add it to an album
 */
class ScanFolderUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String, folderUri: Uri): Result<Unit> {
        // Scan folder for wallpapers
        val wallpapersResult = wallpaperRepository.scanFolderForWallpapers(folderUri)

        if (wallpapersResult is Result.Error) {
            return Result.Error(wallpapersResult.exception)
        }

        val wallpapers = (wallpapersResult as Result.Success).data
        if (wallpapers.isEmpty()) {
            return Result.Error(Exception("No images found in folder"))
        }

        // Create folder
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            ?: return Result.Error(Exception("Invalid folder URI"))

        val folderId = generateId()
        val folder = Folder(
            id = folderId,
            albumId = albumId,
            name = documentFile.name ?: "Unknown Folder",
            uri = folderUri.toString(),
            coverUri = wallpapers.firstOrNull()?.uri,
            dateModified = documentFile.lastModified(),
            wallpapers = wallpapers.map { it.copy(albumId = albumId, folderId = folderId) }
        )

        return albumRepository.addFolderToAlbum(albumId, folder)
    }
}
