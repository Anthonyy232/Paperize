package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.core.runCatchingCancellable
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

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily album refresh")

            val albums = albumRepository.getAlbumSummaries().first()

            if (albums.isEmpty()) {
                Log.d(TAG, "No albums to refresh")
                return Result.success()
            }

            var totalRemoved = 0
            var totalAdded = 0
            var failedCount = 0

            albums.forEach { album ->
                runCatchingCancellable { albumRepository.pruneMissingEntries(album.id) }
                    .onSuccess { removedCount ->
                        totalRemoved += removedCount
                        if (removedCount > 0) {
                            Log.d(TAG, "Album '${album.name}': removed $removedCount invalid items")
                        }
                    }
                    .onFailure {
                        Log.e(TAG, "Error validating album '${album.name}'", it)
                        failedCount++
                    }

                // Reload after validation, which may have removed folders from the snapshot.
                val currentAlbum = albumRepository.getAlbumById(album.id).first()
                currentAlbum?.folders.orEmpty().forEach { folder ->
                    runCatchingCancellable { refreshFolderUseCase(folder.id) }
                        .onSuccess { totalAdded += it }
                        .onFailure {
                            Log.e(TAG, "Error refreshing folder '${folder.name}'", it)
                            failedCount++
                        }
                }
            }
            Log.d(TAG, "Daily album refresh completed: removed $totalRemoved items, added $totalAdded new wallpapers across ${albums.size} albums ($failedCount failures)")

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during album refresh", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "AlbumRefreshWorker"
    }
}
