package com.anthonyla.paperize.core.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class WallpaperRenderer @Inject constructor(@param:ApplicationContext private val context: Context) {
    suspend fun render(wallpaper: Wallpaper, screen: ScreenType, settings: ScheduleSettings): Bitmap? {
        currentCoroutineContext().ensureActive()
        val effects = when (screen) {
            ScreenType.HOME, ScreenType.BOTH -> settings.homeEffects
            ScreenType.LOCK -> settings.lockEffects
            ScreenType.LIVE -> settings.liveEffects
        }
        val scaling = when (screen) {
            ScreenType.HOME, ScreenType.BOTH -> settings.homeScalingType
            ScreenType.LOCK -> settings.lockScalingType
            ScreenType.LIVE -> settings.liveScalingType
        }
        val size = getDeviceScreenSize(context)
        var bitmap = retrieveBitmap(context, wallpaper.uri.toUri(), size.width, size.height, scaling,
            usesLauncherManagedScrolling(screen, scaling, settings.homeScrollingEnabled)) ?: return null
        try {
            val processed = processBitmap(
                source = bitmap,
                enableDarken = effects.enableDarken, darkenPercent = effects.darkenPercentage,
                enableBlur = effects.enableBlur, blurPercent = effects.blurPercentage,
                enableVignette = effects.enableVignette, vignettePercent = effects.vignettePercentage,
                enableGrayscale = effects.enableGrayscale, grayscalePercent = effects.grayscalePercentage
            )
            if (processed !== bitmap) bitmap.recycle()
            bitmap = processed
            if (settings.adaptiveBrightness) {
                val adjusted = adaptiveBrightnessAdjustment(context, bitmap)
                if (adjusted !== bitmap) bitmap.recycle()
                bitmap = adjusted
            }
            currentCoroutineContext().ensureActive()
            return bitmap
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }
    }
}
