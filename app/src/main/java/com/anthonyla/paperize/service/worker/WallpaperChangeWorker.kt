package com.anthonyla.paperize.service.worker

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.usecase.ChangeWallpaperUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex
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
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WallpaperChangeWorker"

        /**
         * Mutex to prevent concurrent wallpaper changes
         * Ensures only one wallpaper change operation runs at a time
         */
        private val wallpaperChangeMutex = Mutex()
    }

    override suspend fun doWork(): Result {
        return try {
            val screenTypeString = inputData.getString(Constants.EXTRA_SCREEN_TYPE)
            val screenType = screenTypeString?.let { ScreenType.fromString(it) } ?: ScreenType.HOME

            Log.d(TAG, "Starting wallpaper change for $screenType")

            // Use mutex to prevent concurrent wallpaper changes
            wallpaperChangeMutex.withLock {
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
                // Live wallpaper changes are handled by the live wallpaper service itself
                // WorkManager is not used for live wallpaper scheduling
                Log.d(TAG, "Live wallpaper change - no action needed in worker")
            }
            ScreenType.HOME -> {
                val homeAlbumId = settings.homeAlbumId
                if (homeAlbumId != null) {
                    changeHomeWallpaper(homeAlbumId, wallpaperManager)
                } else {
                    Log.w(TAG, "No home album selected")
                }
            }
            ScreenType.LOCK -> {
                val lockAlbumId = settings.lockAlbumId
                if (lockAlbumId != null) {
                    changeLockWallpaper(lockAlbumId, wallpaperManager)
                } else {
                    Log.w(TAG, "No lock album selected")
                }
            }
            ScreenType.BOTH -> {
                // Synchronized mode: set both screens to the same wallpaper
                // This ensures both HOME and LOCK show identical wallpapers
                val albumId = settings.homeAlbumId  // Use home album (should be same as lock when synced)

                if (albumId != null) {
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

                            bitmap.recycle()

                            Log.d(TAG, "Both screens wallpaper changed successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting wallpaper for both screens", e)
                            throw e
                        }
                    }.onError { error ->
                        Log.e(TAG, "Error getting wallpaper bitmap for both screens", error)
                        throw error
                    }
                } else {
                    Log.w(TAG, "No album selected for synchronized mode")
                }
            }
        }
    }

    private suspend fun changeHomeWallpaper(albumId: String, wallpaperManager: WallpaperManager) {
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

                // Recycle bitmap after setting to free memory
                bitmap.recycle()

                Log.d(TAG, "Home wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting home wallpaper", e)
                throw e
            }
        }.onError { error ->
            Log.e(TAG, "Error getting home wallpaper bitmap", error)
            throw error
        }
    }

    private suspend fun changeLockWallpaper(albumId: String, wallpaperManager: WallpaperManager) {
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

                // Recycle bitmap after setting to free memory
                bitmap.recycle()

                Log.d(TAG, "Lock wallpaper changed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting lock wallpaper", e)
                throw e
            }
        }.onError { error ->
            Log.e(TAG, "Error getting lock wallpaper bitmap", error)
            throw error
        }
    }
}
