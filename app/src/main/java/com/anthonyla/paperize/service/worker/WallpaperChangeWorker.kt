package com.anthonyla.paperize.service.worker

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import com.anthonyla.paperize.service.WallpaperChangeLock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.withLock

/**
 * WorkManager worker for changing wallpapers
 *
 * Handles periodic wallpaper changes for home screen, lock screen, or both.
 * Uses CoroutineWorker for suspend function support and Hilt for dependency injection.
 */
@HiltWorker
class WallpaperChangeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val changeWallpaperUseCase: ChangeWallpaperUseCase,
    private val settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperChangeLock: WallpaperChangeLock
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WallpaperChangeWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val screenTypeString = inputData.getString(Constants.EXTRA_SCREEN_TYPE)
            val screenType = screenTypeString?.let { ScreenType.fromString(it) } ?: ScreenType.HOME

            Log.d(TAG, "Starting wallpaper change for $screenType")

            // Use shared lock to prevent concurrent wallpaper changes across service and worker
            wallpaperChangeLock.mutex.withLock {
                changeWallpaper(screenType)
            }

            Log.d(TAG, "Wallpaper change completed successfully for $screenType")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error changing wallpaper", e)
            // Retry on failure (WorkManager will handle backoff)
            if (runAttemptCount < Constants.MAX_WORK_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun changeWallpaper(screenType: ScreenType) {
        val settings = settingsRepository.getScheduleSettings()
        val wallpaperManager = WallpaperManager.getInstance(context)

        when (screenType) {
            ScreenType.LIVE -> {
                // Send broadcast to trigger live wallpaper reload
                val intent = android.content.Intent(Constants.ACTION_RELOAD_WALLPAPER)
                intent.setPackage(context.packageName)
                context.sendBroadcast(intent)
                Log.d(TAG, "Sent reload broadcast to live wallpaper service")
            }
            ScreenType.HOME -> {
                val homeAlbumId = settings.homeAlbumId
                if (homeAlbumId != null) {
                    changeHomeWallpaper(homeAlbumId, settings, wallpaperManager)
                } else {
                    Log.w(TAG, "No home album selected")
                }
            }
            ScreenType.LOCK -> {
                val lockAlbumId = settings.lockAlbumId
                if (lockAlbumId != null) {
                    changeLockWallpaper(lockAlbumId, settings, wallpaperManager)
                } else {
                    Log.w(TAG, "No lock album selected")
                }
            }
            ScreenType.BOTH -> {
                val homeAlbumId = settings.homeAlbumId
                val lockAlbumId = settings.lockAlbumId

                // Only treat as synchronized when both screens point to the same album.
                // If they diverge (transient state during album selection), fall back to
                // independent changes — mirrors the guard in WallpaperChangeService.
                if (homeAlbumId != null && lockAlbumId != null && homeAlbumId == lockAlbumId) {
                    val result = changeWallpaperUseCase(homeAlbumId, ScreenType.HOME)
                    result.onSuccess { bitmap ->
                        try {
                            // Validate bitmap before setting
                            if (bitmap.width <= 0 || bitmap.height <= 0) {
                                throw IllegalStateException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                            }
                            if (bitmap.isRecycled) {
                                throw IllegalStateException("Bitmap has been recycled")
                            }

                            Log.d(TAG, "Setting both screens - size: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")

                            wallpaperManager.setBitmap(
                                bitmap,
                                null,
                                true,
                                WallpaperManager.FLAG_SYSTEM
                            )

                            wallpaperManager.setBitmap(
                                bitmap,
                                null,
                                true,
                                WallpaperManager.FLAG_LOCK
                            )

                            Log.d(TAG, "Both screens wallpaper changed successfully")

                            // Keep LOCK queue in sync with HOME so that if the user later
                            // switches to separate schedules, both screens continue from
                            // the same queue position rather than LOCK restarting at 0.
                            try {
                                val homeCurrentId = wallpaperRepository
                                    .getCurrentWallpaper(homeAlbumId, ScreenType.HOME)?.id
                                if (homeCurrentId != null) {
                                    if (wallpaperRepository.getNextWallpaperInQueue(
                                            homeAlbumId, ScreenType.LOCK) == null) {
                                        wallpaperRepository.buildWallpaperQueue(
                                            homeAlbumId, ScreenType.LOCK, settings.shuffleEnabled)
                                    }
                                    wallpaperRepository.getAndDequeueWallpaper(
                                        homeAlbumId, ScreenType.LOCK)
                                    wallpaperRepository.setCurrentWallpaper(
                                        homeAlbumId, ScreenType.LOCK, homeCurrentId)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to sync LOCK queue in BOTH mode", e)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting wallpaper for both screens", e)
                            throw e
                        } finally {
                            bitmap.recycle()
                        }
                    }.onError { error ->
                        if (error is EmptyAlbumException) {
                            Log.w(TAG, "Album is empty for BOTH screens — disabling changer")
                            val updatedSettings = settings.copy(
                                homeAlbumId = null,
                                lockAlbumId = null,
                                enableChanger = false
                            )
                            settingsRepository.updateScheduleSettings(updatedSettings)
                        } else {
                            Log.e(TAG, "Error getting wallpaper bitmap for both screens", error)
                            throw error
                        }
                    }
                } else {
                    // Albums diverged — change screens independently
                    if (homeAlbumId != null) {
                        changeHomeWallpaper(homeAlbumId, settings, wallpaperManager)
                    } else {
                        Log.w(TAG, "No home album selected for BOTH mode")
                    }
                    if (lockAlbumId != null) {
                        changeLockWallpaper(lockAlbumId, settings, wallpaperManager)
                    } else {
                        Log.w(TAG, "No lock album selected for BOTH mode")
                    }
                }
            }
        }
    }

    private suspend fun changeHomeWallpaper(albumId: String, settings: com.anthonyla.paperize.domain.model.ScheduleSettings, wallpaperManager: WallpaperManager) {
        val result = changeWallpaperUseCase(albumId, ScreenType.HOME)
        result.onSuccess { bitmap ->
            try {
                // Validate bitmap before setting
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    throw IllegalStateException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                }
                if (bitmap.isRecycled) {
                    throw IllegalStateException("Bitmap has been recycled")
                }

                Log.d(TAG, "Setting home wallpaper - size: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")

                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_SYSTEM
                )

                Log.d(TAG, "Home wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting home wallpaper", e)
                throw e
            } finally {
                bitmap.recycle()
            }
        }.onError { error ->
            if (error is EmptyAlbumException) {
                Log.w(TAG, "Home album is empty — disabling home screen")
                val lockStillActive = settings.lockEnabled && settings.lockAlbumId != null
                settingsRepository.updateScheduleSettings(settings.copy(
                    homeAlbumId = null,
                    enableChanger = if (lockStillActive) settings.enableChanger else false
                ))
            } else {
                Log.e(TAG, "Error getting home wallpaper bitmap", error)
                throw error
            }
        }
    }

    private suspend fun changeLockWallpaper(albumId: String, settings: com.anthonyla.paperize.domain.model.ScheduleSettings, wallpaperManager: WallpaperManager) {
        val result = changeWallpaperUseCase(albumId, ScreenType.LOCK)
        result.onSuccess { bitmap ->
            try {
                // Validate bitmap before setting
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    throw IllegalStateException("Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                }
                if (bitmap.isRecycled) {
                    throw IllegalStateException("Bitmap has been recycled")
                }

                Log.d(TAG, "Setting lock wallpaper - size: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")

                wallpaperManager.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )

                Log.d(TAG, "Lock wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting lock wallpaper", e)
                throw e
            } finally {
                bitmap.recycle()
            }
        }.onError { error ->
            if (error is EmptyAlbumException) {
                Log.w(TAG, "Lock album is empty — disabling lock screen")
                val homeStillActive = settings.homeEnabled && settings.homeAlbumId != null
                settingsRepository.updateScheduleSettings(settings.copy(
                    lockAlbumId = null,
                    enableChanger = if (homeStillActive) settings.enableChanger else false
                ))
            } else {
                Log.e(TAG, "Error getting lock wallpaper bitmap", error)
                throw error
            }
        }
    }
}
