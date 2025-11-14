package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
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

/**
 * Use case to change wallpaper
 *
 * Handles the complete wallpaper changing flow:
 * 1. Get next wallpaper from queue
 * 2. Validate URI
 * 3. Load and process bitmap
 * 4. Apply effects
 * 5. Dequeue wallpaper
 * 6. Refill queue if needed
 */
class ChangeWallpaperUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(albumId: String, screenType: ScreenType): Result<Bitmap> {
        return try {
            // Get next wallpaper from queue
            val wallpaper = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
                ?: return Result.Error(Exception("No wallpapers in queue"))

            // Validate URI
            val uri = wallpaper.uri.toUri()
            if (!uri.isValid(context.contentResolver)) {
                // Remove invalid wallpaper and try again
                wallpaperRepository.deleteWallpaper(wallpaper.id)
                return invoke(albumId, screenType)
            }

            // Get settings
            val settings = settingsRepository.getScheduleSettings()
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
                ?: return Result.Error(Exception("Failed to load bitmap"))

            // Apply scaling
            val scaledBitmap = scaleBitmap(bitmap, screenSize.width, screenSize.height, scaling)

            // Apply effects
            val processedBitmap = processBitmap(
                source = scaledBitmap,
                darkenPercent = effects.darkenPercentage,
                blurPercent = effects.blurPercentage,
                vignettePercent = effects.vignettePercentage,
                grayscalePercent = if (effects.enableGrayscale) 100 else 0
            )

            // Dequeue wallpaper
            wallpaperRepository.dequeueWallpaper(albumId, screenType)

            // Check if queue needs refilling
            val queueSize = wallpaperRepository.getNextWallpaperInQueue(albumId, screenType)
            if (queueSize == null) {
                // Rebuild queue
                wallpaperRepository.buildWallpaperQueue(albumId, screenType, settings.shuffleEnabled)
            }

            Result.Success(processedBitmap)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
