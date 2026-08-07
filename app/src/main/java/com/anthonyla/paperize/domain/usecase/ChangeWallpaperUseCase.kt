package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.NoValidWallpaperException
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.adaptiveBrightnessAdjustment
import com.anthonyla.paperize.core.util.getWallpaperRenderSize
import com.anthonyla.paperize.core.util.processBitmap
import com.anthonyla.paperize.core.util.retrieveBitmap
import com.anthonyla.paperize.core.util.usesLauncherManagedScrolling
import com.anthonyla.paperize.domain.model.PreparedWallpaper
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Prepares the next wallpaper without marking it as current.
 *
 * The queue item is dequeued while it is decoded, but callers must invoke [complete] only after
 * WallpaperManager confirms the bitmap was applied. If the platform rejects it, [restore] puts
 * the item back at the front of the queue.
 */
class ChangeWallpaperUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        albumId: String,
        screenType: ScreenType
    ): Result<PreparedWallpaper> {
        return try {
            val settings = settingsRepository.getScheduleSettings()

            if (wallpaperRepository.getNextWallpaperInQueue(albumId, screenType) == null) {
                val buildResult = wallpaperRepository.buildWallpaperQueue(
                    albumId,
                    screenType,
                    settings.shuffleEnabled
                )
                if (buildResult is Result.Error) {
                    return Result.Error(
                        Exception(context.getString(R.string.error_failed_to_build_queue))
                    )
                }
            }

            val effects = when (screenType) {
                ScreenType.LIVE -> settings.liveEffects
                ScreenType.HOME, ScreenType.BOTH -> settings.homeEffects
                ScreenType.LOCK -> settings.lockEffects
            }
            val scaling = when (screenType) {
                ScreenType.LIVE -> settings.liveScalingType
                ScreenType.HOME, ScreenType.BOTH -> settings.homeScalingType
                ScreenType.LOCK -> settings.lockScalingType
            }
            val preserveSourceOverflow = usesLauncherManagedScrolling(
                screenType,
                scaling,
                settings.homeScrollingEnabled
            )
            val screenSize = getWallpaperRenderSize(context, screenType, scaling)

            var finalBitmap: Bitmap? = null
            var preparedWallpaperId: String? = null
            var remainingRetries = Constants.MAX_WALLPAPER_LOAD_RETRIES
            var queueRebuildAttempts = 0

            while (finalBitmap == null && remainingRetries > 0) {
                val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, screenType)

                if (candidate == null) {
                    queueRebuildAttempts++
                    if (queueRebuildAttempts > Constants.MAX_QUEUE_REBUILD_ATTEMPTS) {
                        return Result.Error(
                            EmptyAlbumException(context.getString(R.string.no_wallpapers_in_album))
                        )
                    }
                    val rebuildResult = wallpaperRepository.buildWallpaperQueue(
                        albumId,
                        screenType,
                        settings.shuffleEnabled
                    )
                    if (rebuildResult is Result.Error) {
                        return Result.Error(
                            Exception(context.getString(R.string.error_failed_to_build_queue))
                        )
                    }
                    continue
                }

                try {
                    val bitmap = retrieveBitmap(
                        context = context,
                        wallpaperUri = candidate.uri.toUri(),
                        width = screenSize.width,
                        height = screenSize.height,
                        scaling = scaling,
                        preserveSourceOverflow = preserveSourceOverflow
                    )
                    if (bitmap == null) {
                        // A temporarily inaccessible/corrupt item is skipped for this cycle.
                        // AlbumRefreshWorker owns permanent pruning.
                        remainingRetries--
                        continue
                    }

                    var processedBitmap: Bitmap? = null
                    try {
                        processedBitmap = processBitmap(
                            source = bitmap,
                            enableDarken = effects.enableDarken,
                            darkenPercent = effects.darkenPercentage,
                            enableBlur = effects.enableBlur,
                            blurPercent = effects.blurPercentage,
                            enableVignette = effects.enableVignette,
                            vignettePercent = effects.vignettePercentage,
                            enableGrayscale = effects.enableGrayscale,
                            grayscalePercent = effects.grayscalePercentage
                        )

                        if (processedBitmap !== bitmap) bitmap.recycle()

                        if (settings.adaptiveBrightness) {
                            val previousBitmap = processedBitmap
                            processedBitmap = adaptiveBrightnessAdjustment(context, processedBitmap)
                            if (processedBitmap !== previousBitmap) previousBitmap.recycle()
                        }

                        finalBitmap = processedBitmap
                        preparedWallpaperId = candidate.id
                    } catch (e: Exception) {
                        if (processedBitmap != null && !processedBitmap.isRecycled) {
                            processedBitmap.recycle()
                        } else if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                        throw e
                    }
                } catch (_: Exception) {
                    remainingRetries--
                }
            }

            val preparedBitmap = finalBitmap
                ?: return Result.Error(
                    NoValidWallpaperException(
                        context.getString(R.string.error_no_valid_wallpaper_after_retries)
                    )
                )

            Result.Success(
                PreparedWallpaper(
                    bitmap = preparedBitmap,
                    albumId = albumId,
                    screenType = screenType,
                    wallpaperId = checkNotNull(preparedWallpaperId),
                    shuffle = settings.shuffleEnabled
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Record a successfully applied wallpaper and keep the target screen queue in sync.
     *
     * [screenType] can differ from the prepared queue when one HOME item was atomically applied to
     * both screens. The exact item is removed from LOCK rather than blindly dequeuing its head.
     */
    suspend fun complete(
        prepared: PreparedWallpaper,
        screenType: ScreenType = prepared.screenType
    ) {
        try {
            wallpaperRepository.setCurrentWallpaper(
                prepared.albumId,
                screenType,
                prepared.wallpaperId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Applied wallpaper could not be recorded as current", e)
            return
        }

        try {
            if (wallpaperRepository.getNextWallpaperInQueue(prepared.albumId, screenType) == null) {
                wallpaperRepository.buildWallpaperQueue(
                    prepared.albumId,
                    screenType,
                    prepared.shuffle
                )
            }
            // Build first when this is the first synchronized use of a screen queue, then remove
            // the exact applied item. This prevents the just-applied wallpaper from being
            // reintroduced at the head of a freshly built queue.
            wallpaperRepository.removeWallpaperFromQueue(
                prepared.albumId,
                screenType,
                prepared.wallpaperId
            )
        } catch (e: Exception) {
            Log.w(TAG, "Queue sync failed; it will rebuild on the next change", e)
        }
    }

    /** Restore a prepared item after WallpaperManager rejected it. */
    suspend fun restore(prepared: PreparedWallpaper) {
        try {
            wallpaperRepository.restoreWallpaperToQueueFront(
                prepared.albumId,
                prepared.screenType,
                prepared.wallpaperId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore rejected wallpaper to its queue", e)
        }
    }

    private companion object {
        const val TAG = "ChangeWallpaperUseCase"
    }
}
