package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.WallpaperChangeLock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.anthonyla.paperize.service.wallpaper.WallpaperController
import kotlinx.coroutines.sync.withLock

@HiltWorker
class WallpaperChangeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wallpaperController: WallpaperController,
    private val settingsRepository: SettingsRepository,
    private val wallpaperChangeLock: WallpaperChangeLock
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val screenType = ScreenType.fromString(inputData.getString(Constants.EXTRA_SCREEN_TYPE))
        return runCatchingCancellable {
            wallpaperChangeLock.mutex.withLock {
                wallpaperController.change(screenType, settingsRepository.getScheduleSettings())
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { e ->
                Log.e(TAG, "Error changing $screenType wallpaper", e)
                if (runAttemptCount < Constants.MAX_WORK_RETRY_ATTEMPTS) Result.retry() else Result.failure()
            }
        )
    }

    private companion object {
        const val TAG = "WallpaperChangeWorker"
    }
}
