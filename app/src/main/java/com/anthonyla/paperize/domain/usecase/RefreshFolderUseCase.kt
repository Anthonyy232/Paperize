package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.map
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshFolderUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val wallpaperRepository: WallpaperRepository
) {
    private val refreshMutex = Mutex()

    suspend operator fun invoke(folderId: String): Result<Int> {
        return try {
            val folder = albumRepository.getFolderById(folderId).first()
                ?: return Result.Success(0)
            val scan = wallpaperRepository.scanFolderForWallpapers(folder.uri.toUri())
            if (scan !is Result.Success) return scan.map { 0 }

            // Serialize read-and-save, but let unrelated provider scans run independently.
            refreshMutex.withLock {
                val current = albumRepository.getFolderById(folderId).first()
                    ?: return@withLock Result.Success(0)
                val existingUris = current.wallpapers.map { it.uri }.toHashSet()
                val nextOrder = (current.wallpapers.maxOfOrNull { it.displayOrder } ?: -1) + 1
                val additions = scan.data.filterNot { it.uri in existingUris }
                    .mapIndexed { index, wallpaper ->
                        wallpaper.copy(albumId = current.albumId, folderId = folderId, displayOrder = nextOrder + index)
                    }
                if (additions.isEmpty()) return@withLock Result.Success(0)
                albumRepository.addWallpapersToAlbum(current.albumId, additions).map { additions.size }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
