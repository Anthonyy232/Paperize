package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.util.adaptiveBrightnessAdjustment
import com.anthonyla.paperize.core.util.getDeviceScreenSize
import com.anthonyla.paperize.core.util.isValid
import com.anthonyla.paperize.core.util.processBitmap
import com.anthonyla.paperize.core.util.retrieveBitmap
import com.anthonyla.paperize.core.util.scaleBitmap
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Constants
private const val MAX_VALIDATION_RETRIES = 10

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

            // Atomically get and dequeue wallpaper, skipping invalid ones
            // This prevents race conditions when multiple wallpaper changes happen simultaneously
            var wallpaper: Wallpaper? = null
            var maxRetries = MAX_VALIDATION_RETRIES // Prevent infinite loop

            while (wallpaper == null && maxRetries > 0) {
                // Atomically get and remove from queue in a single transaction
                val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, screenType)

                // If still null after building queue, no wallpapers exist in album
                if (candidate == null) {
                    return Result.Error(Exception(context.getString(R.string.no_wallpapers_in_album)))
                }

                // Validate URI
                val uri = candidate.uri.toUri()
                if (uri.isValid(context.contentResolver)) {
                    wallpaper = candidate
                } else {
                    // Remove invalid wallpaper entity (already dequeued)
                    wallpaperRepository.deleteWallpaper(candidate.id)
                    maxRetries--
                }
            }

            if (wallpaper == null) {
                return Result.Error(Exception(context.getString(R.string.error_no_valid_wallpaper_after_retries)))
            }

            val uri = wallpaper.uri.toUri()

            // Get effects and scaling from settings
            val effects = if (screenType == ScreenType.HOME || screenType == ScreenType.BOTH) {
                settings.homeEffects
            } else {
                settings.lockEffects
            }
            val scaling = if (screenType == ScreenType.HOME || screenType == ScreenType.BOTH) {
                settings.homeScalingType
            } else {
                settings.lockScalingType
            }

            // Get screen dimensions
            val screenSize = getDeviceScreenSize(context)

            // Load bitmap
            val bitmap = retrieveBitmap(context, uri, screenSize.width, screenSize.height, scaling)
                ?: return Result.Error(Exception(context.getString(R.string.error_failed_to_load_bitmap)))

            // Apply scaling
            val scaledBitmap = scaleBitmap(bitmap, screenSize.width, screenSize.height, scaling)
            // Recycle original bitmap if scaling created a new one
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
            }

            // Apply effects with proper enable flags
            var processedBitmap = processBitmap(
                context = context,
                source = scaledBitmap,
                enableDarken = effects.enableDarken,
                darkenPercent = effects.darkenPercentage,
                enableBlur = effects.enableBlur,
                blurPercent = effects.blurPercentage,
                enableVignette = effects.enableVignette,
                vignettePercent = effects.vignettePercentage,
                enableGrayscale = effects.enableGrayscale,
                grayscalePercent = if (effects.enableGrayscale) 100 else 0
            )

            // Recycle scaled bitmap if effects created a new one
            if (processedBitmap !== scaledBitmap) {
                scaledBitmap.recycle()
            }

            // Apply adaptive brightness if enabled
            if (settings.adaptiveBrightness) {
                val previousBitmap = processedBitmap
                processedBitmap = adaptiveBrightnessAdjustment(context, processedBitmap)
                // Recycle previous if adaptive brightness created a new bitmap
                if (processedBitmap !== previousBitmap) {
                    previousBitmap.recycle()
                }
            }

            // Check if queue needs refilling (wallpaper was already dequeued atomically)
            val queueSize = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
            if (queueSize == null) {
                // Queue is now empty, rebuild it
                wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
            }

            Result.Success(processedBitmap)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
