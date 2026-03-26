package com.anthonyla.paperize.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.util.adaptiveBrightnessAdjustment
import com.anthonyla.paperize.core.util.getDeviceScreenSize
import com.anthonyla.paperize.core.util.isValid
import com.anthonyla.paperize.core.util.processBitmap
import com.anthonyla.paperize.core.util.retrieveBitmap
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reapply current visual effects to the last-applied wallpaper without advancing the queue.
 *
 * Used when display settings (blur, darken, scaling, etc.) change and the user wants
 * to see the effect on the current wallpaper immediately rather than jumping to the next one.
 *
 * Returns Result.Error if no current wallpaper has been recorded yet — callers should
 * fall back to [ChangeWallpaperUseCase] in that case.
 */
class ReapplyEffectsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(albumId: String, screenType: ScreenType): Result<Bitmap> {
        return try {
            val current = wallpaperRepository.getCurrentWallpaper(albumId, screenType)
                ?: return Result.Error(Exception(context.getString(R.string.error_no_valid_wallpaper_after_retries)))

            val settings = settingsRepository.getScheduleSettings()
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

            val uri = current.uri.toUri()
            if (!uri.isValid(context.contentResolver)) {
                return Result.Error(Exception(context.getString(R.string.error_no_valid_wallpaper_after_retries)))
            }

            val bitmap = retrieveBitmap(context, uri, screenSize.width, screenSize.height, scaling)
                ?: return Result.Error(Exception(context.getString(R.string.error_no_valid_wallpaper_after_retries)))

            var processed = processBitmap(
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

            if (processed !== bitmap) bitmap.recycle()

            if (settings.adaptiveBrightness) {
                val prev = processed
                processed = adaptiveBrightnessAdjustment(context, processed)
                if (processed !== prev) prev.recycle()
            }

            Result.Success(processed)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
