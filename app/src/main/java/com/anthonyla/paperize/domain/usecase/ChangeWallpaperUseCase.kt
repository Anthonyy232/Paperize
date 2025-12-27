package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.EmptyAlbumException
import com.anthonyla.paperize.core.NoValidWallpaperException
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.util.adaptiveBrightnessAdjustment
import com.anthonyla.paperize.core.util.getDeviceScreenSize
import com.anthonyla.paperize.core.util.isValid
import com.anthonyla.paperize.core.util.processBitmap
import com.anthonyla.paperize.core.util.retrieveBitmap
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject



/**
 * Use case to change wallpaper
 *
 * Handles the complete wallpaper changing flow:
 * 1. Atomically get and dequeue next wallpaper from queue
 * 2. Validate URI
 * 3. Load and process bitmap
 * 4. Apply effects
 * 5. Refill queue if needed
 */
class ChangeWallpaperUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(albumId: String, screenType: ScreenType): Result<Bitmap> {
        return try {
            // Get settings first - needed for queue building
            val settings = settingsRepository.getScheduleSettings()

            // Check if queue exists, build it if empty
            val queueCheck = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
            if (queueCheck == null) {
                // Queue is empty, build it first
                val buildResult = wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
                if (buildResult is Result.Error) {
                    return Result.Error(Exception(context.getString(R.string.error_failed_to_build_queue)))
                }
            }

            // Atomically get and dequeue wallpaper, skipping invalid or corrupted ones
            var finalBitmap: Bitmap? = null
            var maxRetries = Constants.MAX_WALLPAPER_LOAD_RETRIES
            var queueRebuildAttempts = 0

            // Get screen dimensions once for the retry loop
            val screenSize = getDeviceScreenSize(context)
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

            while (finalBitmap == null && maxRetries > 0) {
                val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, screenType)

                if (candidate == null) {
                    queueRebuildAttempts++
                    if (queueRebuildAttempts > 2) {
                        return Result.Error(EmptyAlbumException(context.getString(R.string.no_wallpapers_in_album)))
                    }
                    val rebuildResult = wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
                    if (rebuildResult is Result.Error) {
                        return Result.Error(Exception(context.getString(R.string.error_failed_to_build_queue)))
                    }
                    continue
                }

                val uri = candidate.uri.toUri()
                if (uri.isValid(context.contentResolver)) {
                    // Try to load and process the bitmap
                    try {
                        val bitmap = retrieveBitmap(context, uri, screenSize.width, screenSize.height, scaling)
                        if (bitmap != null) {
                            // retrieveBitmap already applies scaling via ImageDecoder.setTargetSize
                            // Apply effects directly to the retrieved bitmap

                            var processedBitmap = processBitmap(
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

                            if (processedBitmap !== bitmap) {
                                bitmap.recycle()
                            }

                            // Apply adaptive brightness
                            if (settings.adaptiveBrightness) {
                                val previousBitmap = processedBitmap
                                processedBitmap = adaptiveBrightnessAdjustment(context, processedBitmap)
                                if (processedBitmap !== previousBitmap) {
                                    previousBitmap.recycle()
                                }
                            }

                            finalBitmap = processedBitmap
                        } else {
                            // File exists but failed to decode (corrupted)
                            wallpaperRepository.deleteWallpaper(candidate.id)
                            maxRetries--
                        }
                    } catch (_: Exception) {
                        // Error during processing, skip this wallpaper
                        wallpaperRepository.deleteWallpaper(candidate.id)
                        maxRetries--
                    }
                } else {
                    // URI invalid
                    wallpaperRepository.deleteWallpaper(candidate.id)
                    maxRetries--
                }
            }

            if (finalBitmap == null) {
                return Result.Error(NoValidWallpaperException(context.getString(R.string.error_no_valid_wallpaper_after_retries)))
            }

            // Check if queue needs refilling
            val nextItem = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
            if (nextItem == null) {
                wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
            }

            Result.Success(finalBitmap)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
