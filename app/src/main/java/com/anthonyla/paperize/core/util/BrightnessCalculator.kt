package com.anthonyla.paperize.core.util

import android.graphics.Bitmap
import com.anthonyla.paperize.core.constants.Constants

object BrightnessCalculator {

    /** Estimates luminance with BT.709 coefficients; channels remain in their encoded color space. */
    fun calculateLuminance(pixel: Int): Double {
        val r = ((pixel shr 16) and 0xFF) / 255.0
        val g = ((pixel shr 8) and 0xFF) / 255.0
        val b = (pixel and 0xFF) / 255.0

        return Constants.LUMINANCE_RED * r + Constants.LUMINANCE_GREEN * g + Constants.LUMINANCE_BLUE * b
    }

    /** Samples rows in bulk to avoid a JNI call for every pixel. Returns brightness in 0..1. */
    fun calculateBitmapBrightness(bitmap: Bitmap): Float {
        val sampleSize = Constants.BRIGHTNESS_SAMPLE_SIZE
        val w = bitmap.width
        val h = bitmap.height

        val rowBuffer = IntArray(w)
        var totalLuminance = 0.0
        var pixelCount = 0

        for (y in 0 until h step sampleSize) {
            bitmap.getPixels(rowBuffer, 0, w, 0, y, w, 1)
            for (x in 0 until w step sampleSize) {
                totalLuminance += calculateLuminance(rowBuffer[x])
                pixelCount++
            }
        }

        return (totalLuminance / pixelCount).toFloat()
    }

    fun getAdaptiveMultiplier(isDarkMode: Boolean, brightness: Float): Float {
        val lightBrightnessMin = Constants.LIGHT_BRIGHTNESS_MIN
        val darkBrightnessMax = Constants.DARK_BRIGHTNESS_MAX
        val targetBrightnessDark = Constants.TARGET_BRIGHTNESS_DARK
        val targetBrightnessLight = Constants.TARGET_BRIGHTNESS_LIGHT

        // Avoid issues with very dark items or division by zero
        if (brightness < 0.01f) return 1.0f

        return when {
            isDarkMode && brightness > lightBrightnessMin -> targetBrightnessDark / brightness
            !isDarkMode && brightness < darkBrightnessMax -> targetBrightnessLight / brightness
            else -> 1.0f
        }
    }
}
