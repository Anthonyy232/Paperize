package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.constants.Constants

/**
 * Domain model for Wallpaper Effects
 *
 * Represents the visual effects applied to wallpapers
 */
data class WallpaperEffects(
    val enableBlur: Boolean = false,
    val blurPercentage: Int = Constants.DEFAULT_BLUR_PERCENTAGE,
    val enableDarken: Boolean = false,
    val darkenPercentage: Int = Constants.DEFAULT_DARKEN_PERCENTAGE,
    val enableVignette: Boolean = false,
    val vignettePercentage: Int = Constants.DEFAULT_VIGNETTE_PERCENTAGE,
    val enableGrayscale: Boolean = false
) {
    /**
     * Check if any effects are applied
     */
    val hasEffects: Boolean
        get() = (enableBlur && blurPercentage > 0) ||
                (enableDarken && darkenPercentage != 0) ||
                (enableVignette && vignettePercentage > 0) ||
                enableGrayscale

    /**
     * Validate effect percentages
     */
    fun validate(): WallpaperEffects = copy(
        darkenPercentage = darkenPercentage.coerceIn(-100, 100),
        blurPercentage = blurPercentage.coerceIn(0, 100),
        vignettePercentage = vignettePercentage.coerceIn(0, 100)
    )

    companion object {
        fun default() = WallpaperEffects()
        fun none() = WallpaperEffects()
    }
}
