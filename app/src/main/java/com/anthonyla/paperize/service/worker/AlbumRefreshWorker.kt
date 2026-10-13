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
import kotlinx.coroutines.flow.first

@HiltWorker
class AlbumRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val albumRepository: AlbumRepository,
    private val refreshFolderUseCase: RefreshFolderUseCase
) : CoroutineWorker(appContext, workerParams) {

    /** Prune missing items and pick up new folder files. One failing album or folder does not stop the rest. */
    override suspend fun doWork(): Result {
        albumRepository.getAlbumSummaries().first().forEach { album ->
            runCatchingCancellable { albumRepository.pruneMissingEntries(album.id) }
                .onFailure { Log.e(TAG, "Error validating album '${album.name}'", it) }

            // Reload after validation, which may have removed folders from the snapshot.
            albumRepository.getAlbumById(album.id).first()?.folders.orEmpty().forEach { folder ->
                runCatchingCancellable { refreshFolderUseCase(folder.id) }
                    .onFailure { Log.e(TAG, "Error refreshing folder '${folder.name}'", it) }
            }
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "AlbumRefreshWorker"
    }
}
