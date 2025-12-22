package com.anthonyla.paperize.core.util

import android.graphics.Bitmap
import androidx.core.graphics.get
import com.anthonyla.paperize.core.constants.Constants

/**
 * Utility class for brightness calculations and adaptive adjustments.
 * Extracted from WallpaperUtil for better testability.
 */
object BrightnessCalculator {

    /**
     * Calculate relative luminance of a single pixel using ITU-R BT.709 standard.
     * Use bitwise operations to avoid dependency on android.graphics.Color in core logic tests.
     */
    fun calculateLuminance(pixel: Int): Double {
        val r = ((pixel shr 16) and 0xFF) / 255.0
        val g = ((pixel shr 8) and 0xFF) / 255.0
        val b = (pixel and 0xFF) / 255.0

        return Constants.LUMINANCE_RED * r + Constants.LUMINANCE_GREEN * g + Constants.LUMINANCE_BLUE * b
    }

    /**
     * Calculate brightness estimate of bitmap (0.0 - 1.0)
     * Uses luminance calculation based on RGB values.
     */
    fun calculateBitmapBrightness(bitmap: Bitmap): Float {
        val sampleSize = Constants.BRIGHTNESS_SAMPLE_SIZE
        var totalLuminance = 0.0
        var pixelCount = 0

        for (x in 0 until bitmap.width step sampleSize) {
            for (y in 0 until bitmap.height step sampleSize) {
                val pixel = bitmap[x, y]
                totalLuminance += calculateLuminance(pixel)
                pixelCount++
            }
        }

        return if (pixelCount > 0) {
            (totalLuminance / pixelCount).toFloat()
        } else {
            Constants.DEFAULT_BRIGHTNESS
        }
    }

    /**
     * Get adaptive brightness multiplier factor based on system dark/light mode.
     *
     * @param isDarkMode Whether the system is in dark mode
     * @param brightness Current image brightness (0.0 to 1.0)
     * @return Multiplier factor to apply to colors
     */
    fun getAdaptiveMultiplier(isDarkMode: Boolean, brightness: Float): Float {
        val lightBrightnessMin = Constants.LIGHT_BRIGHTNESS_MIN
        val darkBrightnessMax = Constants.DARK_BRIGHTNESS_MAX
        val targetBrightnessDark = Constants.TARGET_BRIGHTNESS_DARK
        val targetBrightnessLight = Constants.TARGET_BRIGHTNESS_LIGHT

        // Avoid issues with very dark items or division by zero
        if (brightness < 0.01f) return 1.0f

        return when {
            // In dark mode with very bright image: darken it
            isDarkMode && brightness > lightBrightnessMin -> targetBrightnessDark / brightness
            // In light mode with very dark image: brighten it
            !isDarkMode && brightness < darkBrightnessMax -> targetBrightnessLight / brightness
            // No adjustment needed
            else -> 1.0f
        }
    }
}
