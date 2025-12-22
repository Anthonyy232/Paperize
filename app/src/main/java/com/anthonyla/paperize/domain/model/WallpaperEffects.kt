package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.constants.Constants

/**
 * Domain model for Wallpaper Effects
 *
 * Represents the visual effects applied to wallpapers
 * Includes both visual effects (blur, darken, etc.) and interactive effects (double-tap, parallax)
 */
data class WallpaperEffects(
    // Visual effects (available in both static and live modes)
    val enableBlur: Boolean = false,
    val blurPercentage: Int = Constants.DEFAULT_BLUR_PERCENTAGE,
    val enableDarken: Boolean = false,
    val darkenPercentage: Int = Constants.DEFAULT_DARKEN_PERCENTAGE,
    val enableVignette: Boolean = false,
    val vignettePercentage: Int = Constants.DEFAULT_VIGNETTE_PERCENTAGE,
    val enableGrayscale: Boolean = false,
    val grayscalePercentage: Int = Constants.DEFAULT_GRAYSCALE_PERCENTAGE,

    // Interactive effects (live wallpaper mode only)
    val enableDoubleTap: Boolean = false,
    val enableChangeOnScreenOn: Boolean = false,
    val enableParallax: Boolean = false,
    val parallaxIntensity: Int = Constants.DEFAULT_PARALLAX_INTENSITY
) {
    /**
     * Check if any effects are applied
     */
    val hasEffects: Boolean
        get() = (enableBlur && blurPercentage > 0) ||
                (enableDarken && darkenPercentage > 0) ||
                (enableVignette && vignettePercentage > 0) ||
                (enableGrayscale && grayscalePercentage > 0)

    /**
     * Validate effect percentages
     */
    fun validate(): WallpaperEffects = copy(
        darkenPercentage = darkenPercentage.coerceIn(0, 100),
        blurPercentage = blurPercentage.coerceIn(0, 100),
        vignettePercentage = vignettePercentage.coerceIn(0, 100),
        grayscalePercentage = grayscalePercentage.coerceIn(0, 100),
        parallaxIntensity = parallaxIntensity.coerceIn(0, 100)
    )

    companion object {
        fun default() = WallpaperEffects()
        fun none() = WallpaperEffects()
    }
}
