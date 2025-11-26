package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.core.Result as CoreResult
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.usecase.RefreshAlbumUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker that validates and refreshes all albums daily
 *
 * - Runs once daily (typically at 3 AM)
 * - Validates all wallpaper and folder URIs in all albums
 * - Removes invalid entries (deleted files, permission changes, etc.)
 * - Rescans all folders for new wallpapers and adds them to albums
 * - Only runs when wallpaper changer is enabled
 */
@HiltWorker
class AlbumRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val albumRepository: AlbumRepository,
    private val wallpaperRepository: com.anthonyla.paperize.domain.repository.WallpaperRepository,
    private val refreshAlbumUseCase: RefreshAlbumUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            Log.d(TAG, "Starting daily album refresh")

            // Get all albums from Flow
            val albums = albumRepository.getAllAlbums().first()

            if (albums.isEmpty()) {
                Log.d(TAG, "No albums to refresh")
                return androidx.work.ListenableWorker.Result.success()
            }

            var totalRemoved = 0
            var totalAdded = 0
            var failedCount = 0

            // Refresh each album
            albums.forEach { album ->
                var albumHasNewWallpapers = false

                // Step 1: Validate and remove invalid URIs
                when (val result = refreshAlbumUseCase(album.id)) {
                    is CoreResult.Success -> {
                        val removedCount = result.data
                        totalRemoved += removedCount
                        if (removedCount > 0) {
                            Log.d(TAG, "Album '${album.name}': removed $removedCount invalid items")
                        }
                    }
                    is CoreResult.Error -> {
                        Log.e(TAG, "Error validating album '${album.name}'", result.exception)
                        failedCount++
                    }
                    is CoreResult.Loading -> {
                        /* Loading state not used */
                    }
                }

                // Step 2: Rescan all folders for new wallpapers
                album.folders.forEach { folder ->
                    when (val scanResult = wallpaperRepository.scanFolderForWallpapers(android.net.Uri.parse(folder.uri))) {
                        is CoreResult.Success -> {
                            val scannedWallpapers = scanResult.data

                            // Get ALL existing URIs (direct wallpapers + folder wallpapers)
                            val existingUris = (album.wallpapers.map { it.uri } +
                                               album.folders.flatMap { it.wallpapers.map { w -> w.uri } }).toSet()

                            // Find new wallpapers not already in album (anywhere)
                            val newWallpapers = scannedWallpapers.filter { it.uri !in existingUris }

                            // Add new wallpapers to album
                            newWallpapers.forEach { wallpaper ->
                                // Fix: Create copy with correct albumId and folderId
                                // scanFolderForWallpapers returns wallpapers with empty albumId/null folderId
                                val wallpaperToAdd = wallpaper.copy(
                                    albumId = album.id,
                                    folderId = folder.id
                                )
                                
                                when (wallpaperRepository.addWallpaper(wallpaperToAdd)) {
                                    is CoreResult.Success -> {
                                        totalAdded++
                                        albumHasNewWallpapers = true
                                    }
                                    is CoreResult.Error -> {
                                        Log.e(TAG, "Error adding wallpaper to album '${album.name}'")
                                        failedCount++
                                    }
                                    is CoreResult.Loading -> {
                                        /* Loading state not used */
                                    }
                                }
                            }

                            if (newWallpapers.isNotEmpty()) {
                                Log.d(TAG, "Album '${album.name}': added ${newWallpapers.size} new wallpapers from folder '${folder.name}'")
                            }
                        }
                        is CoreResult.Error -> {
                            Log.e(TAG, "Error scanning folder '${folder.name}' in album '${album.name}'", scanResult.exception)
                        }
                        is CoreResult.Loading -> {
                            /* Loading state not used */
                        }
                    }
                }

                // Step 3: Refresh folder covers if new wallpapers were added
                if (albumHasNewWallpapers) {
                    when (albumRepository.refreshFolderCovers(album.id)) {
                        is CoreResult.Success -> {
                            Log.d(TAG, "Album '${album.name}': folder covers refreshed")
                        }
                        is CoreResult.Error -> {
                            Log.e(TAG, "Error refreshing folder covers for album '${album.name}'")
                        }
                        is CoreResult.Loading -> {
                            /* Loading state not used */
                        }
                    }
                }
            }

            Log.d(TAG, "Daily album refresh completed: removed $totalRemoved items, added $totalAdded new wallpapers across ${albums.size} albums ($failedCount failures)")

            // Return success even if some albums failed (partial success)
            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during album refresh", e)
            androidx.work.ListenableWorker.Result.failure()
        }
    }

    companion object {
        private const val TAG = "AlbumRefreshWorker"
    }
}
