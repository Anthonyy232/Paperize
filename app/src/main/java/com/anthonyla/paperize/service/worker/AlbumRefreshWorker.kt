package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.core.Result as CoreResult
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.usecase.RefreshFolderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class AlbumRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val albumRepository: AlbumRepository,
    private val refreshFolderUseCase: RefreshFolderUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            Log.d(TAG, "Starting daily album refresh")

            val albums = albumRepository.getAlbumSummaries().first()

            if (albums.isEmpty()) {
                Log.d(TAG, "No albums to refresh")
                return androidx.work.ListenableWorker.Result.success()
            }

            var totalRemoved = 0
            var totalAdded = 0
            var failedCount = 0

            albums.forEach { album ->
                when (val result = albumRepository.pruneMissingEntries(album.id)) {
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
                }

                // Reload after validation, which may have removed folders from the snapshot.
                val currentAlbum = albumRepository.getAlbumById(album.id).first()
                currentAlbum?.folders.orEmpty().forEach { folder ->
                    when (val result = refreshFolderUseCase(folder.id)) {
                        is CoreResult.Success -> totalAdded += result.data
                        is CoreResult.Error -> {
                            Log.e(TAG, "Error refreshing folder '${folder.name}'", result.exception)
                            failedCount++
                        }
                    }
                }
            }
            Log.d(TAG, "Daily album refresh completed: removed $totalRemoved items, added $totalAdded new wallpapers across ${albums.size} albums ($failedCount failures)")

            androidx.work.ListenableWorker.Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during album refresh", e)
            androidx.work.ListenableWorker.Result.failure()
        }
    }

    companion object {
        private const val TAG = "AlbumRefreshWorker"
    }
}
